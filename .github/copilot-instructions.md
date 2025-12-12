# Copilot instructions for `stock-api`

These instructions are intended to keep newly generated code consistent with the existing Kotlin/Ktor/Koin style and architecture in this repository.

## Tech + defaults

- Language: Kotlin (Kotlin style: `kotlin.code.style=official`).
- Server: Ktor (Netty engine) with `ContentNegotiation` + `kotlinx.serialization` JSON.
- DI: Koin.
- Tests: JUnit 5 + Cucumber (BDD style step definitions + suite runner).
- Logging: logback + `kotlin-logging`.

## Build + runtime

- Build: Gradle Kotlin DSL (`build.gradle.kts`).
- JVM toolchain: `kotlin { jvmToolchain(23) }`.
- Server entrypoint: `application { mainClass = "org.darren.stock.ktor.ApplicationKt" }`.
- Ktor config is in `src/main/resources/application.yaml` (module + port).
- Koin properties are in `src/main/resources/koin.properties`.

## Project structure and placement

- Put production code under `src/main/kotlin/org/darren/stock/...`.
- Keep HTTP endpoints in `src/main/kotlin/org/darren/stock/ktor`.
- Keep domain logic under `src/main/kotlin/org/darren/stock/domain`.
- Keep persistence adapters under `src/main/kotlin/org/darren/stock/persistence`.
- Keep shared utilities under `src/main/kotlin/org/darren/stock/util`.
- Put tests under `src/test/kotlin/org/darren/stock/...`.

When adding a new endpoint, mirror the existing pattern:
- One file per endpoint group in `org.darren.stock.ktor`.
- An `object` with a `fun Routing.<name>()` extension.
- Register it from `Application.module { routing { ... } }` in `org.darren.stock.ktor.Application`.

## Ktor endpoint conventions

- Use `call.parameters["..."]!!` consistently (existing code assumes required path params are present).
- For request bodies, use `call.receive<...DTO>()`.
- For responses, use `call.respond(status, dto)`.
- Prefer `kotlinx.serialization` DTOs (`@Serializable`).
- Keep request/response DTOs **nested inside** the endpoint `object` and mark them `private` unless reused elsewhere.
- Use `DateSerializer` for `LocalDateTime` fields:
  - `@Serializable(with = DateSerializer::class) val <field>: LocalDateTime`

### Error handling conventions

- Do not introduce ad-hoc exception-to-HTTP mappings inside endpoints unless there is a strong reason.
- Prefer routing errors through `StatusPages` in `Application.kt`:
  - Known mappings currently include `LocationNotFoundException`, `LocationNotTrackedException`, and `BadRequestException` cases.
- When returning a simple error, use `ErrorDTO(status = "...")` with the existing shape.

**Operation-specific rule for untracked locations**

- Reads (e.g., `GET`): allow `LocationNotTrackedException` to flow to `StatusPages` so the server can redirect to the first tracked parent (current behavior).
- Writes (e.g., `POST`/`PUT`/`PATCH`/`DELETE`): catch `LocationNotTrackedException` in the endpoint and return `HttpStatusCode.BadRequest` with `ErrorDTO("LocationNotTracked")` (do not redirect writes).

If you need a new error response shape, **ask first** (see “Ask before changing” section).

## Koin / dependency injection conventions

- DI is started in `main()` via `startKoin { fileProperties(); modules(...) }`.
- Configuration values are read from `koin.properties` via `getProperty("...")`.
- In Ktor routing code, dependencies are often obtained via:
  - `val x by inject<Type>(Type::class.java)`
  - or `val x = inject<Type>(Type::class.java).value`

Follow the existing pattern in the local file; do not mix styles within the same file.

## Serialization / JSON conventions

- Server JSON config uses `Json { decodeEnumsCaseInsensitive = true }`.
- Client JSON config uses `Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }`.
- Standardize on `kotlinx.serialization` for all new request/response DTOs and JSON handling.
- Jackson is available as a test dependency (for json-unit assertions) but should not be used in production code.
- If you add new enums used in JSON, ensure unknown values are handled safely (see `JsonSerializerTest`).

## Logging conventions

- Prefer `kotlin-logging` (`KotlinLogging.logger {}`) for loggers.
- For outbound HTTP calls, wrap calls with `LoggingHelper.wrapHttpCallWithLogging(logger) { ... }`.

## Coroutines / concurrency

- Do not introduce new uses of `GlobalScope`.
- If you touch `StockSystem` or actors, prefer structured concurrency and explicit lifecycle management.
- If you need to add concurrency-sensitive state (maps/caches), prefer thread-safe structures or guard with a `Mutex`.

If an API change would require rewriting actor/lifecycle behavior, **ask first**.

## Tests conventions

- Most behavior tests are Cucumber step definitions under `src/test/kotlin/org/darren/stock/steps`.
- The suite runner is `src/test/kotlin/org/darren/stock/RunSuiteTests.kt`.

When adding a feature/endpoints:
- Prefer adding/adjusting step definitions and feature coverage consistent with existing tests.
- For low-level serialization or utility behavior, add small JUnit tests (see `JsonSerializerTest`).

## Formatting + naming

- Prefer concise, intention-revealing names.
- Keep DTO fields aligned with JSON field names.
- Keep files small and focused; avoid "god" endpoint files.

## Code quality tools

- **Spotless with ktlint**: Automated code formatting enforcing official Kotlin style (`./gradlew spotlessApply` to format, `./gradlew spotlessCheck` to verify)
- **Detekt**: Static code analysis for quality checks (`./gradlew detekt`)
- Both tools are configured in `build.gradle.kts` with project-specific rules in `detekt.yml`
- **Always run `./gradlew spotlessApply` after making code changes** to ensure consistent formatting

## Ask before changing (important)

Ask the user before you do any of the following:

- Introducing a new dependency (Gradle `dependencies { ... }`).
- Changing the error contract (`ErrorDTO`, `MissingFieldsDTO`, `InvalidValuesDTO`) or adding new top-level error DTOs.
- Changing endpoint paths or response shapes (this impacts docs/tests/clients).
- Refactoring DI style (e.g., switching to Ktor’s Koin plugin or removing `KoinJavaComponent.inject`).
- Reworking concurrency/actor model in `StockSystem`.

When unsure about domain behavior, ask for:
- Expected HTTP status codes + error payloads.
- Whether to validate location existence and/or “tracked” status for the specific operation.
- Whether to include child locations by default for new stock-read endpoints.

## URI/path design

- Prioritize domain clarity over global URI standardisation.
- Prefer extending existing patterns for the specific domain area rather than refactoring older paths.

## Recommended improvements (backlog)

These are not required for new code generation, but are worth considering:

- ~~Add automated formatting/linting (e.g., `ktlint` or `spotless`) and/or static analysis (`detekt`) to enforce the existing style consistently.~~ **DONE**: Added Spotless with ktlint and detekt
- ~~Remove duplicate `tasks.test { useJUnitPlatform() }` block in `build.gradle.kts`.~~ **DONE**
- Consider extracting Koin modules from `main()` into dedicated files for clearer composition/testing.
- Address concurrency TODOs in `StockSystem` (thread safety + lifecycle of stock pots) and remove `GlobalScope` usage if feasible.
- Consider making route path conventions consistent (e.g., `Move` route path segmenting vs other endpoints) if clients allow.
- Consider scoping `LocationNotTrackedException` redirects to reads only (e.g., redirect on `GET`, fail on writes) to match the operation-specific rule.
