FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY services/circleguard-promotion-service/build/libs/app.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
