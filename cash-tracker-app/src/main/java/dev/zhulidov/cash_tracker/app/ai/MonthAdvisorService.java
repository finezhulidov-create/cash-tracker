package dev.zhulidov.cash_tracker.app.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zhulidov.cash_tracker.app.dto.AiAdviceResponse;
import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.transactions.dto.CategorySpendingDto;
import dev.zhulidov.cash_tracker.transactions.dto.MonthSummaryDto;
import dev.zhulidov.cash_tracker.transactions.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Совет по тратам месяца от языковой модели. Порядок работы:
 * <ol>
 *   <li>собрать данные месяца (суммы, бюджеты, прошлый месяц) без внутренних идентификаторов;</li>
 *   <li>если для тех же данных уже есть готовый ответ — отдать его бесплатно;</li>
 *   <li>встать в очередь: у физлица в GigaChat один поток, остальные запросы получают 429;</li>
 *   <li>проверить лимит пользователя, спросить модель, сохранить ответ.</li>
 * </ol>
 */
@Slf4j
@Service
public class MonthAdvisorService {

    static final YearMonth EARLIEST_MONTH = YearMonth.of(2000, 1);
    /** Самый восточный часовой пояс: там новый месяц наступает раньше, чем по UTC. */
    private static final ZoneOffset EASTMOST_ZONE = ZoneOffset.ofHours(14);
    static final String NO_DATA_TEXT = "В этом месяце нет ни трат, ни бюджетов — анализировать пока нечего.";

    private final AiProperties properties;
    private final AnalyticsService analyticsService;
    private final AdvicePayloadBuilder payloadBuilder;
    private final AiUsageStore store;
    private final GigaChatClient client;
    private final AdvisorPrompt prompt;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /** Один запрос к модели за раз (справедливая очередь). */
    private final Semaphore slot = new Semaphore(1, true);

    public MonthAdvisorService(AiProperties properties, AnalyticsService analyticsService,
                               AdvicePayloadBuilder payloadBuilder, AiUsageStore store, GigaChatClient client,
                               AdvisorPrompt prompt, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.analyticsService = analyticsService;
        this.payloadBuilder = payloadBuilder;
        this.store = store;
        this.client = client;
        this.prompt = prompt;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    public AiAdviceResponse advise(long userId, YearMonth month) {
        if (!properties.enabled()) {
            throw AiException.disabled();
        }
        // Сервер живёт по UTC, а у пользователя может быть и завтра: новый месяц у него наступает раньше
        YearMonth latestMonth = YearMonth.from(clock.instant().atZone(EASTMOST_ZONE));
        if (month.isAfter(latestMonth)) {
            throw new InvalidRequestException("'yearMonth' must not be in the future");
        }
        if (month.isBefore(EARLIEST_MONTH)) {
            throw new InvalidRequestException("'yearMonth' must not be before " + EARLIEST_MONTH);
        }

        AdvicePayload payload = collect(userId, month);
        if (isEmpty(payload)) {
            return new AiAdviceResponse(NO_DATA_TEXT, clock.instant(), false);
        }

        String json = toJson(payload);
        String cacheKey = properties.keyPrefix() + "advice:" + userId + ":" + month + ":" + fingerprint(json);

        Optional<AiUsageStore.CachedAdvice> cached = store.find(cacheKey);
        if (cached.isPresent()) {
            return response(cached.get(), true);
        }

        acquireSlot();
        try {
            // пока мы стояли в очереди, тот же запрос (двойной клик) мог уже получить ответ
            cached = store.find(cacheKey);
            if (cached.isPresent()) {
                return response(cached.get(), true);
            }
            return ask(userId, month, cacheKey, json);
        } finally {
            slot.release();
        }
    }

    private AiAdviceResponse ask(long userId, YearMonth month, String cacheKey, String json) {
        store.consume(userId);
        GigaChatClient.ChatAnswer answer;
        try {
            answer = client.chat(prompt.text(), userMessage(json));
        } catch (AiException e) {
            store.refund(userId);
            throw e;
        }
        log.info("AI advice generated: userId={}, month={}, tokens={}", userId, month, answer.totalTokens());

        AiUsageStore.CachedAdvice advice = new AiUsageStore.CachedAdvice(answer.text(), clock.instant());
        // обрезанный по длине ответ не запоминаем: при следующем запросе у модели есть шанс ответить целиком
        if (!answer.truncated()) {
            store.save(cacheKey, advice);
        }
        return response(advice, false);
    }

    private AdvicePayload collect(long userId, YearMonth month) {
        MonthSummaryDto summary = analyticsService.monthSummary(userId, month);
        List<CategorySpendingDto> spending = analyticsService.spendingByCategory(userId, month.atDay(1), month.atEndOfMonth());
        YearMonth previous = month.minusMonths(1);
        List<CategorySpendingDto> previousSpending = previous.isBefore(EARLIEST_MONTH)
                ? List.of()
                : analyticsService.spendingByCategory(userId, previous.atDay(1), previous.atEndOfMonth());
        LocalDate today = LocalDate.now(clock);
        return payloadBuilder.build(month, today, summary, spending, previousSpending);
    }

    private static boolean isEmpty(AdvicePayload payload) {
        return payload.totalSpent().signum() == 0 && payload.overallBudget() == null && payload.categories().isEmpty();
    }

    /** Данные отделены от инструкций разметкой: в сообщении пользователя только JSON. */
    private static String userMessage(String json) {
        return "Данные за месяц (JSON):\n```json\n" + json + "\n```";
    }

    private void acquireSlot() {
        try {
            if (!slot.tryAcquire(properties.queueTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                throw AiException.busy(Math.max(properties.queueTimeout().toSeconds(), 1));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw AiException.unavailable();
        }
    }

    private String toJson(AdvicePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize the AI payload", e);
        }
    }

    private static String fingerprint(String json) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static AiAdviceResponse response(AiUsageStore.CachedAdvice advice, boolean cached) {
        return new AiAdviceResponse(advice.text(), advice.generatedAt(), cached);
    }
}
