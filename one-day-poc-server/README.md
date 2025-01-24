# one-pay-poc-server: Kotlin + Ktor

This is the server application for the one-day-poc project. It provides a REST API for the client application to send
requests to.
It is written in Kotlin using the Ktor framework.

## Running the server

To set up the server, you need to have the following installed:

- Java 23 or higher

To run the server, execute the following command in the root directory of the project:

```shell
./gradlew build 
./gradlew run
```

The server will be available at http://localhost:8000 by default.