# Stock API

Lightweight stock management API (Ktor + Koin) used for demo and integration tests.

This repository contains:
- Ktor HTTP endpoints for sales, deliveries, moves and counts
- Idempotency support for state-changing operations using a Caffeine-backed in-memory cache
- Tests: JUnit 5 and Cucumber feature specs

## Quick start

Prerequisites:
- JDK 17+
- Gradle wrapper (`./gradlew`)

Run locally:
```bash
./gradlew clean build
./gradlew test
```

Run the application:
```bash
./gradlew run
# or
java -jar build/libs/stock-api.jar
```

## Idempotency

- In-memory idempotency store uses Caffeine with a 24h TTL by default.
- Request bodies are fingerprinted (SHA-256) and stored with the cached response.
- Duplicate requests with the same `requestId` and identical content return the cached 2xx response.
- Reuse of a `requestId` with different request content returns `409 Conflict`.
- Only successful responses (2xx) are cached; 4xx and 5xx are not cached.

Configuration for TTL and cache size lives in code; consider extracting to `application.yaml` for production.

## Publishing to GitHub (recommended)

If you have the GitHub CLI (`gh`) configured, run:

```bash
# create a repo under your account and push the current repo as `origin`
gh repo create --public --source=. --remote=origin --push
```

If you prefer the web UI:
1. Create a new repository on GitHub with the desired name.
2. Add the remote and push:
```bash
git remote add origin git@github.com:<your-username>/stock-api.git
git push -u origin master
```

## CI

This repo includes a GitHub Actions workflow that runs `./gradlew build` on pushes and pull requests.

## Contributing

See `CONTRIBUTING.md` for guidelines.

## License

This project is licensed under the MIT License — see the `LICENSE` file for details.

