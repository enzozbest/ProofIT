# Authentication Module

The **Authentication Module** handles user authentication and session management within the application. It supports
multiple authentication methods, including OAuth and JWT validation, and integrates Redis as a caching layer to optimize
performance.

## Features

- **User Authentication**: Supports OAuth (Amazon Cognito) and JWT validation.
- **Session Management**: Uses cookies to manage authenticated sessions securely.
- **Caching with Redis**: Reduces authentication request overhead by storing session data in a centralized cache.
- **Secure Access Control**: Implements token-based authentication and role-based authorization.

---

## Authentication Methods

This module implements authentication via OAuth and JWT validation, ensuring secure user login and session persistence.

### OAuth Authentication

- Configures OAuth settings using **Amazon Cognito**.
- Manages authentication redirects and token exchange.

### JWT Validation

- Uses **JWKS** (JSON Web Key Set) to verify JWTs.
- Extracts and validates user claims from JWTs.

---

## Redis Caching for Authentication

The module leverages **Redis** as a caching layer to reduce authentication overhead. The `/api/auth/check` endpoint
first checks Redis for authentication states before sending a validation request to other endpoints in order to improve
performance.

### Installation

You need to install Redis on your system for this feature to work.

#### Linux:

```bash
sudo apt update
sudo apt install redis
sudo systemctl start redis
```

#### macOS (Homebrew):

```bash
brew install redis
brew services start redis
```

#### Windows (Using Docker):

```bash
docker run --name redis-server -p 6379:6379 -d redis
```

### Testing Redis Connection

To verify that Redis is running, execute:

```bash
redis-cli ping
```

The expected response is:

```
PONG
```

---

## Authentication Endpoints

The following API endpoints are available in this module:

| Endpoint                 | Description                                           |
|--------------------------|-------------------------------------------------------|
| **`/api/auth`**          | Initiates the authentication process.                 |
| **`/api/auth/callback`** | Handles the OAuth callback after user authentication. |
| **`/api/auth/logout`**   | Logs out the user by clearing their session.          |
| **`/api/auth/check`**    | Checks Redis for cached data or redirects             |
| **`/api/auth/validate`** | Validates JWTs through JWKS.                          |
| **`/api/auth/me`**       | Retrieves authenticated user information.             |

---

## Session Management

- Sessions are managed using **Ktor Sessions**.
- The authentication session is stored in an `AuthenticatedSession` cookie.
- Session cookies are **HTTP-only, secure, and expire after 1 hour**.

---

## Security Considerations

- **JWT Validation**: The module uses a JWKS endpoint to fetch the public key for verifying JWTs.
- **Role-Based Access Control**: User roles (e.g., `admin`) are validated via JWT claims.
- **Secure Cookies**: Session cookies are protected with HTTP-only and secure flags.
- **Redis Security**: Ensure that your Redis server is configured securely and not exposed to external access.

---

## Configuration

The authentication module reads configuration from a JSON file (default: `auth/src/main/resources/cognito.json`). This
file should contain:

```json
{
  "name": "Cognito",
  "jwtIssuer": "your-cognito-jwks-url",
  "urlProvider": "https://your-cognito-domain/oauth2",
  "providerLookup": {
    "name": "Amazon Cognito",
    "authorizeUrl": "https://your-cognito-domain/oauth2/authorize",
    "accessTokenUrl": "https://your-cognito-domain/oauth2/token",
    "clientId": "your-client-id",
    "clientSecret": "your-client-secret",
    "defaultScopes": [
      "email",
      "openid",
      "profile"
    ]
  }
}
```

---

## Contributing

Contributions to improve this module are welcome! Please ensure that your changes adhere to best security practices.

---
