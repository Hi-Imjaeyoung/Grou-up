FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]