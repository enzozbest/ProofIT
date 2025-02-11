##Authentication Module

This module is responsible for authenticating users. It provides a way to authenticate users using different
authentication methods. The module is also responsible for managing user sessions.

###Authentication Methods

##Cache
This module implements Redis as a centralised cache for authentication requests, reducing the need for calls to the
/api/auth/check endpoint for every routing request to Ktor.

You must install Redis on your system for this to work:

Linux:

```bash
sudo apt update
sudo apt install
sudo systemctl start redis
```

Homebrew:

```bash
brew install redis
brew services start redis
```

Windows (with Docker):

```bash
docker run --name redis-server -p 6379:6379 -d redis
```

You can test that Redis is working by running:

```bash
redis-cli ping
```

The expected response is "PONG"