# Stock API

![CI](https://github.com/darren-gibson/stock-api/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

Lightweight stock management API (Ktor + Koin) used for demo and integration tests.

This repository contains:
- Ktor HTTP endpoints for sales, deliveries, moves and counts
- Domain-level idempotency support for state-changing operations using event sourcing
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

- Domain-level idempotency using event sourcing prevents duplicate state changes
- Request bodies are fingerprinted (SHA-256) and checked against event history
- Duplicate requests with the same `requestId` and identical content are rejected with `409 Conflict`
- Domain metrics track idempotency hits and misses using OpenTelemetry
- Only state-changing operations (sales, deliveries, moves, counts) support idempotency

Idempotency is enforced at the domain layer using actor event sourcing for reliable duplicate detection.

## Concurrency and Actor Model

The API uses the [Actor4k](https://github.com/smyrgeorge/actor4k) library to implement the actor model for thread-safe concurrent access to stock data:

- **Actor per product-location**: Each unique product-location pair has a dedicated actor that manages its state independently
- **Sequential message processing**: Messages to a single actor are processed sequentially, eliminating race conditions
- **Concurrent actors**: Multiple actors process messages in parallel without blocking each other
- **Automatic lifecycle**: Actors are created on-demand and evicted after inactivity (configurable, default 5 minutes)
- **Event sourcing**: State is persisted as events; actors rehydrate from event history when reactivated

For detailed architecture information, see [docs/CONCURRENCY.md](docs/CONCURRENCY.md).

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

