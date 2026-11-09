package dev.zhulidov.cash_tracker.app.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Общие заготовки для тестов ИИ-советника. */
final class AiTestData {

    static final Instant NOW = Instant.parse("2026-10-31T10:00:00Z");

    private AiTestData() {
    }

    static Props props() {
        return new Props();
    }

    /** Настройки с разумными значениями для тестов; меняются по одной. */
    static final class Props {
        private boolean enabled = true;
        private String authKey = "test-key";
        private String caCertPath = "";
        private Duration queueTimeout = Duration.ofSeconds(2);
        private int rateLimitRequests = 3;
        private String keyPrefix = "ai-test:";

        Props enabled(boolean value) {
            this.enabled = value;
            return this;
        }

        Props authKey(String value) {
            this.authKey = value;
            return this;
        }

        Props caCertPath(String value) {
            this.caCertPath = value;
            return this;
        }

        Props queueTimeout(Duration value) {
            this.queueTimeout = value;
            return this;
        }

        Props rateLimitRequests(int value) {
            this.rateLimitRequests = value;
            return this;
        }

        Props keyPrefix(String value) {
            this.keyPrefix = value;
            return this;
        }

        AiProperties build() {
            return new AiProperties(enabled, authKey, "GIGACHAT_API_PERS",
                    "https://oauth.test/api/v2/oauth", "https://api.test/v1", "GigaChat-2-Pro",
                    0.3, 1500, Duration.ofSeconds(5), Duration.ofSeconds(90), queueTimeout,
                    Duration.ofHours(6), rateLimitRequests, Duration.ofHours(1), caCertPath, keyPrefix);
        }
    }

    /** Часы, время которых двигает тест. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
