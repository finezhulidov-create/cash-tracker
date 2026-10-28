package dev.zhulidov.cash_tracker.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cash Tracker API")
                        .version("v1")
                        .description("""
                                Личный трекер финансов: категории, траты с разбивкой по категориям, \
                                месячные бюджеты и аналитика.

                                **Авторизация.** Получите токен через `POST /auth/login` или `POST /auth/register` \
                                и передавайте его в заголовке `Authorization: Bearer <токен>` \
                                (в Swagger UI: кнопка Authorize).

                                **Ошибки.** Все ошибки приходят в одном формате: \
                                `status`, `message`, `timestamp`, `errorCode` и, для ошибок валидации, \
                                `errors` (поле → сообщение). Чужие ресурсы отдаются как 404."""))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT из ответа /auth/login или /auth/register")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
