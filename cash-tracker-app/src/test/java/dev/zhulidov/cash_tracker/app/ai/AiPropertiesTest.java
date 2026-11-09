package dev.zhulidov.cash_tracker.app.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AiPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @EnableConfigurationProperties(AiProperties.class)
    static class Config {
    }

    @Test
    void byDefaultTheAdvisorIsOffAndNeedsNoKey() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            AiProperties properties = context.getBean(AiProperties.class);
            assertFalse(properties.enabled());
            assertEquals("", properties.authKey());
            assertEquals("https://api.giga.chat/v1", properties.apiUrl());
            assertEquals("https://ngw.devices.sberbank.ru:9443/api/v2/oauth", properties.oauthUrl());
            assertEquals("GigaChat-2-Pro", properties.model());
            assertEquals("GIGACHAT_API_PERS", properties.scope());
            assertEquals(Duration.ofSeconds(90), properties.readTimeout());
            assertEquals(Duration.ofHours(6), properties.cacheTtl());
            assertEquals(10, properties.rateLimitRequests());
            assertTrue(properties.caCertPaths().isEmpty());
        });
    }

    @Test
    void environmentStyleNamesAreBound() {
        runner.withPropertyValues("gigachat.enabled=true", "gigachat.auth-key=abc", "gigachat.read-timeout=45s",
                "gigachat.rate-limit-requests=5").run(context -> {
            AiProperties properties = context.getBean(AiProperties.class);
            assertTrue(properties.enabled());
            assertEquals("abc", properties.authKey());
            assertEquals(Duration.ofSeconds(45), properties.readTimeout());
            assertEquals(5, properties.rateLimitRequests());
        });
    }

    @Test
    void enablingWithoutAKeyStopsTheApplicationWithAClearMessage() {
        runner.withPropertyValues("gigachat.enabled=true").run(context -> {
            Throwable failure = context.getStartupFailure();
            assertNotNull(failure);
            assertTrue(rootMessage(failure).contains("GIGACHAT_AUTH_KEY"), rootMessage(failure));
        });
    }

    @Test
    void blankKeyIsTreatedAsMissing() {
        runner.withPropertyValues("gigachat.enabled=true", "gigachat.auth-key=   ").run(context ->
                assertNotNull(context.getStartupFailure()));
    }

    @Test
    void invalidNumbersAreRejectedOnlyWhenTheAdvisorIsOn() {
        runner.withPropertyValues("gigachat.temperature=0").run(context -> assertNull(context.getStartupFailure()));
        runner.withPropertyValues("gigachat.enabled=true", "gigachat.auth-key=k", "gigachat.temperature=0")
                .run(context -> assertNotNull(context.getStartupFailure()));
        runner.withPropertyValues("gigachat.enabled=true", "gigachat.auth-key=k", "gigachat.max-tokens=0")
                .run(context -> assertNotNull(context.getStartupFailure()));
        runner.withPropertyValues("gigachat.enabled=true", "gigachat.auth-key=k", "gigachat.rate-limit-requests=0")
                .run(context -> assertNotNull(context.getStartupFailure()));
        runner.withPropertyValues("gigachat.enabled=true", "gigachat.auth-key=k", "gigachat.read-timeout=0s")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void theKeyMayBePastedWithBasicPrefixAndSpaces() {
        assertEquals("abc==", AiTestData.props().authKey("  Basic abc==  ").build().authKey());
        assertEquals("abc==", AiTestData.props().authKey("basic abc==").build().authKey());
        assertEquals("abc==", AiTestData.props().authKey("abc==").build().authKey());
    }

    @Test
    void trailingSlashOfTheApiUrlIsDropped() {
        runner.withPropertyValues("gigachat.api-url=https://api.giga.chat/v1/").run(context ->
                assertEquals("https://api.giga.chat/v1", context.getBean(AiProperties.class).apiUrl()));
    }

    @Test
    void certificatePathsAreSplitByCommaAndTrimmed() {
        AiProperties properties = AiTestData.props().caCertPath(" /certs/root.pem , /certs/sub.pem ,, ").build();

        assertEquals(java.util.List.of(Path.of("/certs/root.pem"), Path.of("/certs/sub.pem")), properties.caCertPaths());
    }

    private static String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return String.valueOf(root.getMessage());
    }
}
