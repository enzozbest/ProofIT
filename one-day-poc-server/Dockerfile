FROM gradle:jdk23 as build
WORKDIR /app
COPY . .
RUN ./gradlew shadowJar -x test

FROM amazoncorretto:23-alpine-jdk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar one-day-poc-server.jar
CMD ["java", "-jar", "one-day-poc-server.jar"]
EXPOSE 8000