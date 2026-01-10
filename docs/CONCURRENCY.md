# Concurrency Architecture

## Overview

The Stock API uses the [Actor4k](https://github.com/smyrgeorge/actor4k) library to implement the actor model for thread-safe concurrent access to stock data. This design eliminates race conditions while allowing multiple independent product-location pairs to be processed concurrently.

### Why Actors?

- **Isolation**: Each product-location pair has a dedicated actor that owns its state
- **Sequential Processing**: Messages to a single actor are guaranteed to process sequentially
- **Concurrency**: Multiple actors process messages in parallel without blocking each other
- **Thread Safety**: Eliminates the need for explicit locking within actor message handlers
- **Simplicity**: Clear ownership model makes reasoning about state changes straightforward

## Actor Lifecycle

### Creation (Lazy)

Actors are created on-demand when first accessed via `ActorSystem.get()`. The actor key is derived from the product-location combination.

```kotlin
val actor = ActorSystem.get(
    StockPotActor::class,
    ProductLocation.of(productId, locationId).toString()
)
```

### Activation

When an actor is first created or rehydrated after eviction:

1. `onBeforeActivate()` is called
2. `StockStateManager.initializeState()` runs:
   - Attempts to load a snapshot of the actor's state
   - If found, replays events that occurred after the snapshot
   - Otherwise, replays all events from the event store
3. Actor is ready to process messages

### Message Processing

Messages to an actor are processed sequentially by `onReceive()`:

- `GetValue` - Returns current stock state
- `RecordDelivery` - Records incoming stock
- `RecordSale` - Records outgoing stock
- `RecordMove` - Transfers stock between locations
- `RecordCount` - Adjusts stock based on physical count

Each message is idempotent. Duplicate requests (same `requestId` and content) return the current state without modifying it.

### Eviction

After a configured period of inactivity (`actorExpiresAfter`, default 5 minutes):

1. Actor receives `onShutdown()` call
2. Actor reference is removed from the registry
3. Actor's in-memory state is discarded
4. The next access to that product-location will trigger rehydration

### Rehydration

When accessing an evicted actor:

1. New actor instance is created with the same key
2. `onBeforeActivate()` replays state from event store and snapshots
3. Actor is ready to process new messages
4. External clients see no difference

## Thread Safety Guarantees

### Per-Actor Guarantee

Within a single actor:
- Messages are processed strictly sequentially
- State transitions are atomic from the message handler's perspective
- No two message handlers execute concurrently for the same actor
- Safe to access/modify actor state without locks

### Across Actors

- Different actors can process messages concurrently
- No shared mutable state between actors
- Each actor owns its product-location state exclusively
- Safe for multiple HTTP request handlers to send messages to different actors simultaneously

### Repository Access

Actor4k does not guarantee sequential repository access across different actors:
- Multiple actors may call repository methods concurrently
- Repository implementations must be thread-safe
- Current implementations (`InMemoryStockEventRepository`, `InMemorySnapshotRepository`) use thread-safe data structures

## Structured Concurrency

### Scope Management

- **Actor4k Internal Scope**: Actor4k manages its own `CoroutineScope` internally for processing messages and background cleanup
- **Application-Level Scope**: Ktor provides the top-level scope for HTTP request handling
- **Message Processing**: Each message handler runs in Actor4k's scope (safe for `suspend` functions)

### Shutdown Sequence

1. **Application Shutdown**: Ktor gracefully stops accepting requests
2. **Actor System Shutdown**: `ActorSystem.shutdown()` is called (typically from Koin module cleanup)
3. **In-Flight Message Drain**: Actor4k waits for in-flight messages to complete
4. **Cleanup**: Actor registry is cleared, background tasks are cancelled
5. **Timeout**: If shutdown doesn't complete within timeout (2 seconds), forced termination

### Test Isolation

Between tests:
1. Previous `ActorSystem` is shut down (if running)
2. New `ActorSystem` is created with test-specific configuration
3. Actors are isolated per test run
4. No state leakage between tests

## Configuration

### Production Settings (application.yaml)

```yaml
stockPot:
  unloadAfterInactivity: 5m       # How long before an inactive actor is evicted
  unloadCheckInterval: 30s        # How often to check for expired actors
```

### Test Settings

Tests can override these via `ActorSystemTestConfig`:

```kotlin
ActorSystemTestConfig.setOverrides(
    Overrides(
        unloadAfterInactivity = 100.milliseconds,
        unloadCheckInterval = 50.milliseconds
    )
)
```

This allows tests to verify eviction behavior quickly without waiting 5 minutes.

## Known Limitations & Design Trade-offs

### 1. Eviction Overhead

**Trade-off**: Memory bounded at the cost of rehydration latency

- Inactive actors are evicted to prevent unbounded memory growth
- Reactivation requires replaying events from the event store
- For high-volume product-location pairs, eviction may be more frequent
- Snapshot strategy mitigates by reducing event replay cost

### 2. No Inter-Actor Communication

**Design Decision**: Actors cannot send messages to each other

- Moves between locations are coordinated through repository state, not actor-to-actor messaging
- Both source and destination actors independently process the move event
- Simplifies the actor model and avoids deadlock risks

### 3. Single-Message Batching

**Design Decision**: No message batching within an actor

- Each message is processed individually
- Benefits: simple, predictable ordering, easy to reason about
- Cost: slightly higher message processing overhead compared to batching
- Acceptable for stock operations which are typically low-frequency

### 4. No Persistent Actor Identity

**Design Decision**: Actor instances are not persisted, only their state

- Actor references are only valid within the current application instance
- External services cannot reference actors directly
- All access goes through the HTTP API
- Simplifies deployment and scaling

## Concurrency Patterns

### Pattern 1: Concurrent Updates to Different Locations

```
HTTP Handler 1          HTTP Handler 2
    |                       |
    | GET /api/actor1    | GET /api/actor2
    |                       |
    v                       v
  Actor1 (PROD1/LOC1)  Actor2 (PROD1/LOC2)
    |                       |
    | Process Message    | Process Message
    | (sequentially)      | (concurrently)
    v                       v
  State1                  State2
```

Multiple actors process messages in parallel without blocking each other.

### Pattern 2: Concurrent Updates to Same Location (Serialization)

```
HTTP Handler 1          HTTP Handler 2
    |                       |
    | POST /api/actor1   | POST /api/actor1
    |                       |
    v                       v
  Actor1 (PROD1/LOC1)
    |
    +--- Queue Message 1
    +--- Queue Message 2
    |
    | Process in order
    v
  State (after Msg1) ---> State (after Msg2)
```

Messages to the same actor are queued and processed sequentially.

### Pattern 3: Eviction and Rehydration

```
Time t0: Delivery sent
    |
    v
  Actor1 Active (timestamp updated)
    |
    | 5 minutes of inactivity
    |
Time t5m: Eviction check runs
    |
    v
  Actor1 Removed from registry
    |
    | HTTP request arrives
    |
Time t5m+ε: Reactivation
    |
    v
  New Actor1 created, state replayed from events
    |
    v
  Message processed (client sees current state)
```

## Testing Concurrency Behavior

See `src/test/resources/org/darren/stock/Architecture/` for feature specs covering:

- Concurrent updates to different locations (independent processing)
- Concurrent updates to same location (serialization)
- Actor lifecycle (creation, eviction, rehydration)
- System shutdown and cleanup

Run tests with:

```bash
./gradlew test
```

## Further Reading

- [Actor Model](https://en.wikipedia.org/wiki/Actor_model) - Wikipedia article
- [Actor4k Documentation](https://github.com/smyrgeorge/actor4k) - Project repository
- [Structured Concurrency](https://kotlinlang.org/docs/coroutines-basics.html#structured-concurrency) - Kotlin documentation
