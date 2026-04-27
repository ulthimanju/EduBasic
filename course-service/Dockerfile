FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/course-service.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
