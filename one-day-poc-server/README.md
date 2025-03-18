# one-day-poc-server: Kotlin + Ktor

This is the server application for the one-day-poc (ProofIt) project. It provides a REST API for the client application
to send requests to. It is written in Kotlin using the Ktor framework, levaring Kotlin's coroutine-based concurrency
module for a scalable and maintainable application.

# Python Microservice

This module also includes a Python microservice, written using the Flask framework, that is used for similarity search
in the application. This microservice is used to find suitable templates from which the LLM can generate code. The
microservice runs as a background application and typical users and developers should not need to interact with it
directly. However, the source code for it is available at `embeddings/src/main/python`.

## Running the server

To set up the server, you need to have the following installed in your system:

- Java 23 or higher
- Python 3.10 (no other versions are supported)
- Docker (and Docker Compose, if installing separately)
- Redis
- Ollama
- A model of your choice in Ollama. This choice needs to be reflected in your .env file.
- A .env file according to the provided example.env (you must fill in he required variables declared there)

To run the server, execute the following command in the root directory of the project:

Linux and macOS:

```shell
./start.sh
```

Windows:

```shell
start.ps1
```

The provided script will automatically install the required dependencies (e.g. Python/Pip packages) and start both
the Python microservice and the Kotlin server.

The Kotlin server will be available at http://localhost:8000 by default.

The Python microservice will be available at http://localhost:7000 by default.