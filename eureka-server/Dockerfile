FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN apk add --no-cache curl

COPY eureka-server/build/libs/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
