package dev.zhulidov.cash_tracker.app.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin.NewTopics topics(){
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name("register-events").partitions(3).build()
        );
    }
}
