FROM amazoncorretto:23-alpine-jdk
WORKDIR /app
COPY build/libs/one-day-poc-server.jar one-day-poc-server-0.0-SNAPSHOT.jar
EXPOSE 8000
CMD ["java", "-jar", "one-day-poc-server-0.0-SNAPSHOT.jar"]
