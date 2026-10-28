# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем только pom.xml всех модулей — отдельным слоем, чтобы Docker кэшировал
# скачивание зависимостей и не повторял его при каждом изменении исходников.
COPY pom.xml .
COPY cash-tracker-app/pom.xml cash-tracker-app/pom.xml
COPY cash-tracker-common/pom.xml cash-tracker-common/pom.xml
COPY cash-tracker-transactions/pom.xml cash-tracker-transactions/pom.xml



RUN mvn dependency:go-offline -B

# Теперь копируем реальные исходники всех модулей
COPY cash-tracker-app/src cash-tracker-app/src
COPY cash-tracker-common/src cash-tracker-common/src
COPY cash-tracker-transactions/src cash-tracker-transactions/src



RUN mvn clean package -DskipTests -B

# ---- Final stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Собранный jar лежит в target конкретно модуля cash-tracker-app (finalName: cash-tracker-app)
COPY --from=build /app/cash-tracker-app/target/cash-tracker-app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]