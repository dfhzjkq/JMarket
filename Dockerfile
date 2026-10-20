# ---------- Стадия 1: сборка ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests -B

# ---------- Стадия 2: запуск ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/jmarket-0.0.1.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
