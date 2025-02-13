# Database Module

## Overview

This module handles database-related functionality for the application. It includes:
- **API**: endpoints for database interactions.
- **Config**: database configuration logic.
- **Core**: database module source code and database factory that establishes the connection to the database.
- **Repository**: handles data persistence.
- **Schema**: defines the database schema.

## Setting Up Docker

To run the database locally, you'll need Docker installed. Download it from:

[Docker Download](https://www.docker.com/get-started/)

## Running the Database

Ensure you have Docker installed and running. Then, navigate to the project's root directory and run:

```sh
docker compose up -d
```

## Stopping the Database

```sh
docker compose down
```

## Environment Variables

A .env.example file is included in the project. To set up your environment variables:

1. Copy .env.example to .env
```sh
cp .env.example .env
```
2. Update .env with your configuration. Ensure the correct values are set for database connection and any other required settings.

The .env file should be placed in the project's root directory and should not be committed to version control.


