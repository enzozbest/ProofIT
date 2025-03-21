FROM gradle:jdk23 as build
WORKDIR /app
COPY . .
RUN ./gradlew build

FROM amazoncorretto:23-alpine-jdk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

RUN apk add --no-cache curl unzip && \
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm -rf aws awscliv2.zip

CMD ["java", "-jar", "app.jar"]
EXPOSE 8000