FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN apk add --no-cache curl

COPY event-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
