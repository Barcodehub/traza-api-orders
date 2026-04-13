FROM eclipse-temurin:17-jre-jammy
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring
ARG JAR_FILE=build/libs/*-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
