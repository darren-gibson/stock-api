package org.darren.stock.domain.actors

import kotlinx.coroutines.test.runTest
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.domain.Location
import org.darren.stock.domain.ProductLocation
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.events.DeliveryEvent
import org.darren.stock.domain.actors.events.SaleEvent
import org.darren.stock.domain.snapshot.EventCountSnapshotStrategyFactory
import org.darren.stock.domain.snapshot.SnapshotStrategyFactory
import org.darren.stock.persistence.InMemorySnapshotRepository
import org.darren.stock.persistence.InMemoryStockEventRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.time.LocalDateTime

class StockStateManagerTest {
    private lateinit var eventRepository: InMemoryStockEventRepository
    private lateinit var snapshotRepository: InMemorySnapshotRepository
    private lateinit var snapshotStrategyFactory: SnapshotStrategyFactory
    private lateinit var stateManager: StockStateManager

    private val locationId = "loc1"
    private val productId = "prod1"
    private val actorKey = ProductLocation.of(productId, locationId).toString()

    @BeforeEach
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<DateTimeProvider> {
                        object : DateTimeProvider {
                            override fun now() = LocalDateTime.now()
                        }
                    }
                },
            )
        }
        eventRepository = InMemoryStockEventRepository()
        snapshotRepository = InMemorySnapshotRepository()
        snapshotStrategyFactory = EventCountSnapshotStrategyFactory(snapshotRepository, 5)
        stateManager = StockStateManager(locationId, productId, eventRepository, snapshotStrategyFactory)
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `initializeState recreates state from events when no snapshot exists`() =
        runTest {
            // Given: some events in the repository (using non-sequential requestIds)
            val event1 = DeliveryEvent(10.0, "sup1", "ref1", "abc123", "hash1", LocalDateTime.of(2023, 1, 1, 10, 0))
            val event2 = SaleEvent(LocalDateTime.of(2023, 1, 1, 11, 0), 5.0, "xyz789", "hash2")
            eventRepository.insert(locationId, productId, event1)
            eventRepository.insert(locationId, productId, event2)

            // When: initialize state
            stateManager.initializeState()

            // Then: state should reflect the events
            assertEquals(5.0, stateManager.currentState.quantity)
            assertEquals(event2.eventDateTime, stateManager.lastEventTime)
            assertEquals("xyz789", stateManager.lastRequestId)
        }

    @Test
    fun `initializeState loads from valid snapshot and replays subsequent events`() =
        runTest {
            // Given: a snapshot and some events after it (using non-sequential requestIds)
            val snapshotTime = LocalDateTime.of(2023, 1, 1, 10, 0)
            val snapshotState = StockState(Location(locationId), productId, 10.0, 0.0, snapshotTime, "abc123")
            snapshotRepository.saveActorState(actorKey, snapshotState)

            val eventAfter = SaleEvent(LocalDateTime.of(2023, 1, 1, 11, 0), 3.0, "xyz789", "hash2")
            eventRepository.insert(locationId, productId, eventAfter)

            // When: initialize state
            stateManager.initializeState()

            // Then: state reflects snapshot + replayed events
            assertEquals(7.0, stateManager.currentState.quantity)
            assertEquals(eventAfter.eventDateTime, stateManager.lastEventTime)
            assertEquals("xyz789", stateManager.lastRequestId)
        }

    @Test
    fun `processEvent handles in-order event correctly`() =
        runTest {
            // Given: initialized state
            stateManager.initializeState()

            // When: process an in-order event
            val event = DeliveryEvent(10.0, "sup1", "ref1", "abc123", "hash1", LocalDateTime.of(2023, 1, 1, 10, 0))
            val result = stateManager.processEvent(event)

            // Then: state updated
            assertEquals(10.0, result.quantity)
            assertEquals(event.eventDateTime, stateManager.lastEventTime)
            assertEquals("abc123", stateManager.lastRequestId)
        }

    @Test
    fun `processEvent recreates state for out-of-order event`() =
        runTest {
            // Given: state with later event (using non-sequential requestIds to prove ordering is by time, not requestId)
            val laterEvent = DeliveryEvent(10.0, "sup1", "ref1", "xyz789", "hash2", LocalDateTime.of(2023, 1, 1, 12, 0))
            eventRepository.insert(locationId, productId, laterEvent)
            stateManager.initializeState()

            // When: process an earlier event with a "higher" requestId lexicographically
            val earlierEvent = SaleEvent(LocalDateTime.of(2023, 1, 1, 10, 0), 5.0, "abc123", "hash1")
            val result = stateManager.processEvent(earlierEvent)

            // Then: state recreated from all events (ordered by time, not requestId)
            assertEquals(
                10.0,
                result.quantity,
            ) // delivery +10, then sale -5 but since sale is first and negative, capped at 0, then +10
            assertEquals(laterEvent.eventDateTime, stateManager.lastEventTime)
            assertEquals(
                "abc123",
                stateManager.lastRequestId,
            ) // lastRequestId should be from the last persisted event (the one just processed)
        }

    @Test
    fun `processEvent persists event to repository`() =
        runTest {
            // Given: initialized state
            stateManager.initializeState()

            // When: process event
            val event = DeliveryEvent(10.0, "sup1", "ref1", "abc123", "hash1", LocalDateTime.of(2023, 1, 1, 10, 0))
            stateManager.processEvent(event)

            // Then: event is in repository
            val events = eventRepository.getEventsInChronologicalOrder(locationId, productId)
            assertEquals(1, events.count())
            assertEquals(event, events.first())
        }

    @Test
    fun `lastRequestId is set to last persisted request, not lexicographically highest requestId`() =
        runTest {
            // Given: events where last persisted has lexicographically lower requestId
            val event1 = DeliveryEvent(10.0, "sup1", "ref1", "z-high-lex", "hash1", LocalDateTime.of(2023, 1, 1, 10, 0))
            val event2 = SaleEvent(LocalDateTime.of(2023, 1, 1, 11, 0), 3.0, "a-low-lex", "hash2")
            eventRepository.insert(locationId, productId, event1)
            eventRepository.insert(locationId, productId, event2)

            // When: initialize state (triggers recreateStateFromEvents)
            stateManager.initializeState()

            // Then: lastRequestId should be from last persisted event (event2), not lexicographically highest (event1)
            assertEquals(7.0, stateManager.currentState.quantity) // 10 - 3
            assertEquals(event2.eventDateTime, stateManager.lastEventTime)
            assertEquals("a-low-lex", stateManager.lastRequestId) // last persisted, not lex highest
        }

    @Test
    fun `initializeState with no snapshot sets lastRequestId to last persisted request, not chronologically last`() =
        runTest {
            // Given: events received in non-chronological order
            // Sale T0, req-AAA (received first)
            val eventAAA = SaleEvent(LocalDateTime.of(2023, 1, 1, 10, 0), 5.0, "req-AAA", "hash1")
            // Sale T2, req-ZZZ (received second)
            val eventZZZ = SaleEvent(LocalDateTime.of(2023, 1, 1, 12, 0), 3.0, "req-ZZZ", "hash2")
            // Sale T1, req-MMM (received third)
            val eventMMM = SaleEvent(LocalDateTime.of(2023, 1, 1, 11, 0), 2.0, "req-MMM", "hash3")

            // Insert events in reception order (persistence order)
            eventRepository.insert(locationId, productId, eventAAA) // T0, req-AAA
            eventRepository.insert(locationId, productId, eventZZZ) // T2, req-ZZZ
            eventRepository.insert(locationId, productId, eventMMM) // T1, req-MMM

            // When: initialize state with no snapshot (triggers recreateStateFromEvents)
            stateManager.initializeState()

            // Then: state reflects chronological processing with sales not going negative
            // AAA(-5): quantity=0, pending=-5; MMM(-2): quantity=0, pending=-7; ZZZ(-3): quantity=0, pending=-10
            assertEquals(0.0, stateManager.currentState.quantity)
            assertEquals(-10.0, stateManager.currentState.pendingAdjustment)

            // And lastEventTime should be from chronologically last event (ZZZ at T2)
            assertEquals(eventZZZ.eventDateTime, stateManager.lastEventTime)

            // And lastRequestId should be from last persisted event (MMM), not chronologically last (ZZZ)
            assertEquals("req-MMM", stateManager.lastRequestId)
        }

//    @Test
//    fun `automatic snapshots are taken every N events`() =
//        runTest {
//            // Given: initialized state with event count snapshot strategy (every 3 events)
//            val snapshotStrategy = EventCountSnapshotStrategy(3)
//            val testSnapshotManager =
//                SnapshotManager(
//                    actorKey = actorKey,
//                    snapshotStrategy = snapshotStrategy,
//                    snapshotRepository = snapshotRepository,
//                )
//            val testStateManager = StockStateManager(locationId, productId, eventRepository, testSnapshotManager)
//            testStateManager.initializeState()
//
//            // When: process events that should trigger snapshots
//            val event1 = DeliveryEvent(10.0, "sup1", "ref1", "req1", "hash1", LocalDateTime.of(2023, 1, 1, 10, 0))
//            val event2 = DeliveryEvent(5.0, "sup1", "ref2", "req2", "hash2", LocalDateTime.of(2023, 1, 1, 11, 0))
//            val event3 = DeliveryEvent(3.0, "sup1", "ref3", "req3", "hash3", LocalDateTime.of(2023, 1, 1, 12, 0)) // Should trigger snapshot
//            val event4 = DeliveryEvent(2.0, "sup1", "ref4", "req4", "hash4", LocalDateTime.of(2023, 1, 1, 13, 0))
//
//            testStateManager.processEvent(event1)
//            testStateManager.processEvent(event2)
//            testStateManager.processEvent(event3) // Event count = 3, should take snapshot
//            testStateManager.processEvent(event4) // Event count = 4, no snapshot
//
//            // Then: snapshot should have been taken after 3 events
//            val actorKey = ProductLocation.of(productId, locationId).toString()
//            val savedSnapshot = snapshotRepository.loadActorState(actorKey)
//
//            assertNotNull(savedSnapshot)
//            assertEquals(18.0, savedSnapshot!!.quantity) // 10 + 5 + 3 (snapshot taken after 3rd event)
//        }
}
