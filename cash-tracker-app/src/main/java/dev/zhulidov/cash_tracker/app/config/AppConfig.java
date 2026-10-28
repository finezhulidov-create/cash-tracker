package dev.zhulidov.cash_tracker.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /**
     * Единый источник времени. Серверное «сейчас» — всегда UTC, поэтому поведение не зависит от
     * часового пояса машины; в тестах подменяется фиксированными часами.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
