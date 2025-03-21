FROM gradle:jdk23 as build
WORKDIR /app
COPY . .
RUN ./gradlew build

FROM amazoncorretto:23-alpine-jdk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
CMD ["java, "-jar", "app.jar"] 
EXPOSE 8000
