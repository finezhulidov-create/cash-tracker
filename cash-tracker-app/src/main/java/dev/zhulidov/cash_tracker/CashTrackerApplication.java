package dev.zhulidov.cash_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CashTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashTrackerApplication.class, args);
    }

}
