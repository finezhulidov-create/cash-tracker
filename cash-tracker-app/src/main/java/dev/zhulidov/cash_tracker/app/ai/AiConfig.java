package dev.zhulidov.cash_tracker.app.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /**
     * Клиент GigaChat с таймаутами (без них зависший ответ держал бы поток сервера) и, если задан
     * gigachat.ca-cert-path, с доверием к сертификатам Минцифры. Если функция выключена, никаких файлов
     * не читается и в сеть ничего не уходит.
     */
    @Bean
    public GigaChatClient gigaChatClient(AiProperties properties, Clock clock) {
        HttpClient.Builder http = HttpClient.newBuilder().connectTimeout(properties.connectTimeout());
        if (properties.enabled() && !properties.caCertPaths().isEmpty()) {
            http.sslContext(TlsSupport.sslContext(properties.caCertPaths()));
            log.info("GigaChat: additional CA certificates loaded from {}", properties.caCertPaths());
        }
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http.build());
        factory.setReadTimeout(properties.readTimeout());
        log.info("AI advisor is {}", properties.enabled() ? "enabled (model " + properties.model() + ")" : "disabled");
        return new GigaChatClient(properties, RestClient.builder().requestFactory(factory), clock);
    }
}
