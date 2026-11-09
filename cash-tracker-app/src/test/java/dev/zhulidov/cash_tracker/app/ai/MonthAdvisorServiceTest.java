package dev.zhulidov.cash_tracker.app.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zhulidov.cash_tracker.app.dto.AiAdviceResponse;
import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetStatusDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategorySpendingDto;
import dev.zhulidov.cash_tracker.transactions.dto.MonthSummaryDto;
import dev.zhulidov.cash_tracker.transactions.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonthAdvisorServiceTest {

    private static final long USER = 424242L;
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    @Mock
    private AnalyticsService analytics;
    @Mock
    private AiUsageStore store;
    @Mock
    private GigaChatClient client;

    private final AdvisorPrompt prompt = new AdvisorPrompt();
    private final AiTestData.MutableClock clock = new AiTestData.MutableClock(AiTestData.NOW);

    private MonthAdvisorService service;

    @BeforeEach
    void setUp() {
        service = serviceWith(AiTestData.props().build());

        CategoryDto food = new CategoryDto(1L, "Еда", USER);
        when(analytics.monthSummary(USER, OCTOBER)).thenReturn(new MonthSummaryDto(OCTOBER, new BigDecimal("8160"),
                new BudgetStatusDto(5L, OCTOBER.atDay(1), null, new BigDecimal("55000"), new BigDecimal("8160"), new BigDecimal("46840")),
                List.of(new BudgetStatusDto(6L, OCTOBER.atDay(1), food, new BigDecimal("15000"), new BigDecimal("8160"), new BigDecimal("6840")))));
        when(analytics.spendingByCategory(USER, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(new CategorySpendingDto(1L, "Еда", new BigDecimal("8160"))));
        when(analytics.spendingByCategory(USER, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(new CategorySpendingDto(1L, "Еда", new BigDecimal("6000"))));

        when(store.find(anyString())).thenReturn(Optional.empty());
        when(client.chat(anyString(), anyString())).thenReturn(new GigaChatClient.ChatAnswer("Совет", false, 400));
    }

    private MonthAdvisorService serviceWith(AiProperties properties) {
        return new MonthAdvisorService(properties, analytics, new AdvicePayloadBuilder(), store, client, prompt,
                new ObjectMapper(), clock);
    }

    // ---------- когда советник недоступен или не нужен ----------

    @Test
    void whenSwitchedOffItAnswers503WithoutTouchingAnything() {
        MonthAdvisorService off = serviceWith(AiTestData.props().enabled(false).authKey("").build());

        AiException error = assertThrows(AiException.class, () -> off.advise(USER, OCTOBER));

        assertEquals(AiException.DISABLED, error.getErrorCode());
        assertFalse(off.isEnabled());
        verifyNoInteractions(analytics, store, client);
    }

    @Test
    void aFutureMonthIsRejectedBeforeAnyPaidCall() {
        assertThrows(InvalidRequestException.class, () -> service.advise(USER, YearMonth.of(2026, 12)));

        verifyNoInteractions(client, store);
    }

    @Test
    void aMonthThatAlreadyStartedSomewhereEastIsAllowedButNotBefore() {
        YearMonth november = YearMonth.of(2026, 11);
        when(analytics.monthSummary(USER, november)).thenReturn(new MonthSummaryDto(november, BigDecimal.ZERO, null, List.of()));
        clock.advance(Duration.ofHours(-1)); // 09:00 UTC: даже на UTC+14 ещё 31 октября

        assertThrows(InvalidRequestException.class, () -> service.advise(USER, november));

        clock.advance(Duration.ofHours(1)); // 10:00 UTC: на UTC+14 уже 1 ноября
        assertDoesNotThrow(() -> service.advise(USER, november));
        verifyNoInteractions(client);
    }

    @Test
    void anAbsurdlyOldMonthIsRejected() {
        assertThrows(InvalidRequestException.class, () -> service.advise(USER, YearMonth.of(1999, 12)));
    }

    @Test
    void theCurrentMonthIsAllowed() {
        assertDoesNotThrow(() -> service.advise(USER, OCTOBER));
    }

    @Test
    void aMonthWithoutDataIsAnsweredLocallyForFree() {
        YearMonth empty = YearMonth.of(2026, 8);
        when(analytics.monthSummary(USER, empty)).thenReturn(new MonthSummaryDto(empty, BigDecimal.ZERO, null, List.of()));
        when(analytics.spendingByCategory(eq(USER), any(), any())).thenReturn(List.of());

        AiAdviceResponse response = service.advise(USER, empty);

        assertEquals(MonthAdvisorService.NO_DATA_TEXT, response.text());
        assertFalse(response.cached());
        verifyNoInteractions(client);
        verify(store, never()).consume(anyLong());
    }

    // ---------- обычный путь ----------

    @Test
    void asksTheModelAndRemembersTheAnswer() {
        AiAdviceResponse response = service.advise(USER, OCTOBER);

        assertEquals("Совет", response.text());
        assertFalse(response.cached());
        assertEquals(AiTestData.NOW, response.generatedAt());
        verify(store).consume(USER);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AiUsageStore.CachedAdvice> saved = ArgumentCaptor.forClass(AiUsageStore.CachedAdvice.class);
        verify(store).save(key.capture(), saved.capture());
        assertTrue(key.getValue().startsWith("ai-test:advice:" + USER + ":2026-10:"), key.getValue());
        assertEquals("Совет", saved.getValue().text());
    }

    @Test
    void sendsTheInstructionsAsTheSystemMessageAndOnlyDataAsTheUserMessage() {
        service.advise(USER, OCTOBER);

        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(client).chat(system.capture(), user.capture());
        assertEquals(prompt.text(), system.getValue());
        assertTrue(system.getValue().contains("Cash Tracker"));
        assertTrue(user.getValue().startsWith("Данные за месяц (JSON):"));
        assertTrue(user.getValue().contains("\"totalSpent\":8160.00"), user.getValue());
        assertTrue(user.getValue().contains("\"name\":\"Еда\""));
        assertTrue(user.getValue().contains("\"previousMonthTotalSpent\":6000.00"));
        assertTrue(user.getValue().contains("\"monthInProgress\":true"));
        assertTrue(user.getValue().contains("\"daysLeftInMonth\":1"), "31 октября — последний день");
        assertFalse(user.getValue().contains(String.valueOf(USER)), "id пользователя не должен уходить в модель");
    }

    @Test
    void theAnswerForUnchangedDataComesFromTheCacheWithoutAPaidCall() {
        AiUsageStore.CachedAdvice cached = new AiUsageStore.CachedAdvice("Из кэша", AiTestData.NOW.minusSeconds(3600));
        when(store.find(anyString())).thenReturn(Optional.of(cached));

        AiAdviceResponse response = service.advise(USER, OCTOBER);

        assertEquals("Из кэша", response.text());
        assertTrue(response.cached());
        assertEquals(AiTestData.NOW.minusSeconds(3600), response.generatedAt());
        verifyNoInteractions(client);
        verify(store, never()).consume(anyLong());
    }

    @Test
    void theCacheKeyChangesWhenTheDataChanges() {
        service.advise(USER, OCTOBER);
        when(analytics.spendingByCategory(USER, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(new CategorySpendingDto(1L, "Еда", new BigDecimal("9000"))));
        when(analytics.monthSummary(USER, OCTOBER)).thenReturn(new MonthSummaryDto(OCTOBER, new BigDecimal("9000"), null, List.of()));

        service.advise(USER, OCTOBER);

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(store, times(2)).save(keys.capture(), any());
        assertNotEquals(keys.getAllValues().get(0), keys.getAllValues().get(1));
    }

    @Test
    void aCutAnswerIsReturnedButNotRemembered() {
        when(client.chat(anyString(), anyString())).thenReturn(new GigaChatClient.ChatAnswer("Обрыв", true, 1500));

        AiAdviceResponse response = service.advise(USER, OCTOBER);

        assertEquals("Обрыв", response.text());
        verify(store, never()).save(anyString(), any());
    }

    // ---------- лимит и сбои ----------

    @Test
    void whenTheUserLimitIsSpentTheModelIsNotCalled() {
        doThrow(AiException.rateLimited(900)).when(store).consume(USER);

        AiException error = assertThrows(AiException.class, () -> service.advise(USER, OCTOBER));

        assertEquals(AiException.RATE_LIMIT_EXCEEDED, error.getErrorCode());
        assertEquals(900L, error.getRetryAfterSeconds());
        verifyNoInteractions(client);
        verify(store, never()).refund(anyLong());
    }

    @Test
    void aFailureOnTheModelSideGivesTheRequestBack() {
        when(client.chat(anyString(), anyString())).thenThrow(AiException.unavailable());

        AiException error = assertThrows(AiException.class, () -> service.advise(USER, OCTOBER));

        assertEquals(AiException.UNAVAILABLE, error.getErrorCode());
        verify(store).consume(USER);
        verify(store).refund(USER);
        verify(store, never()).save(anyString(), any());
    }

    // ---------- один поток ----------

    @Test
    void aSecondRequestWaitsInLineAndThenReusesTheFirstAnswer() throws Exception {
        CountDownLatch modelStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(client.chat(anyString(), anyString())).thenAnswer(invocation -> {
            modelStarted.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return new GigaChatClient.ChatAnswer("Первый ответ", false, 100);
        });
        // пока первый запрос в работе, кэш пуст; после него — ответ появляется
        when(store.find(anyString())).thenReturn(Optional.empty());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<AiAdviceResponse> first = pool.submit(() -> service.advise(USER, OCTOBER));
            assertTrue(modelStarted.await(5, TimeUnit.SECONDS));

            AiUsageStore.CachedAdvice result = new AiUsageStore.CachedAdvice("Первый ответ", AiTestData.NOW);
            when(store.find(anyString())).thenReturn(Optional.empty(), Optional.of(result));
            Future<AiAdviceResponse> second = pool.submit(() -> service.advise(USER, OCTOBER));
            Thread.sleep(200); // второй уже стоит в очереди
            release.countDown();

            assertEquals("Первый ответ", first.get(5, TimeUnit.SECONDS).text());
            AiAdviceResponse reused = second.get(5, TimeUnit.SECONDS);
            assertEquals("Первый ответ", reused.text());
            assertTrue(reused.cached());
            verify(client, times(1)).chat(anyString(), anyString());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void whenTheLineIsTooLongTheRequestGetsBusy() throws Exception {
        MonthAdvisorService impatient = serviceWith(AiTestData.props().queueTimeout(Duration.ofMillis(150)).build());
        CountDownLatch modelStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(client.chat(anyString(), anyString())).thenAnswer(invocation -> {
            modelStarted.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return new GigaChatClient.ChatAnswer("Ответ", false, 100);
        });

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<AiAdviceResponse> running = pool.submit(() -> impatient.advise(USER, OCTOBER));
            assertTrue(modelStarted.await(5, TimeUnit.SECONDS));

            AiException busy = assertThrows(AiException.class, () -> impatient.advise(USER, OCTOBER));

            assertEquals(AiException.BUSY, busy.getErrorCode());
            assertEquals(429, busy.getStatus().value());
            assertNotNull(busy.getRetryAfterSeconds());
            release.countDown();
            assertEquals("Ответ", running.get(5, TimeUnit.SECONDS).text());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void theLineIsFreedEvenWhenTheModelFails() {
        when(client.chat(anyString(), anyString())).thenThrow(AiException.unavailable())
                .thenReturn(new GigaChatClient.ChatAnswer("Теперь получилось", false, 10));

        assertThrows(AiException.class, () -> service.advise(USER, OCTOBER));
        AiAdviceResponse second = service.advise(USER, OCTOBER);

        assertEquals("Теперь получилось", second.text());
    }
}
