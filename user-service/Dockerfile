# JAR собирается локальным Gradle; образ только упаковывает артефакт.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN apk add --no-cache curl

COPY user-service/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
