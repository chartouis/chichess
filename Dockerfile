FROM eclipse-temurin:25
WORKDIR /app
COPY gradlew /app/
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.jar
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.properties
COPY build.gradle settings.gradle /app/
COPY src/ /app/src/
RUN chmod +x gradlew
RUN ./gradlew build -x test
EXPOSE 8080
CMD ["java", "-jar", "build/libs/chess-0.0.1-SNAPSHOT.jar"]
