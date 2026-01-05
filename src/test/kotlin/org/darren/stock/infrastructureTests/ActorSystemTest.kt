package org.darren.stock.infrastructureTests

import io.github.smyrgeorge.actor4k.actor.Actor
import io.github.smyrgeorge.actor4k.actor.ActorProtocol
import io.github.smyrgeorge.actor4k.actor.Behavior
import io.github.smyrgeorge.actor4k.actor.impl.SimpleMessage
import io.github.smyrgeorge.actor4k.actor.impl.simpleActorOf
import io.github.smyrgeorge.actor4k.system.ActorSystem
import io.github.smyrgeorge.actor4k.system.ActorSystem.loggerFactory
import io.github.smyrgeorge.actor4k.system.registry.SimpleActorRegistry
import io.github.smyrgeorge.actor4k.util.SimpleLoggerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.darren.stock.domain.ProductLocation
import org.darren.stock.domain.actors.StockPotActor
import org.darren.stock.domain.snapshot.EventCountSnapshotStrategyFactory
import org.darren.stock.persistence.InMemorySnapshotRepository
import org.darren.stock.steps.helpers.TestStockEventRepository
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ActorSystemTest {
    class SimpleActor(
        key: String,
    ) : Actor<ActorProtocol, ActorProtocol.Response>(key) {
        override suspend fun onReceive(m: ActorProtocol): Behavior<ActorProtocol.Response> = Behavior.None()
    }

    @BeforeEach
    fun setup() {
        start()
    }

    @AfterEach
    fun teardown() {
        stop()
    }

    @Test
    fun `test actor system starts and shuts down when there's no actors`() {
    }

    @Test
    fun `test actor system starts and shuts down when there's a simple actor`() {
        runBlocking {
            ActorSystem.get(SimpleActor::class, "test-actor")
        }
    }

    @Test
    fun `test actor system starts and shuts down when there's a StockPot Actor`() {
        runBlocking {
            ActorSystem.get(StockPotActor::class, ProductLocation.of("LOC1", "PROD1").toString())
        }
    }

    @Test
    @Timeout(value = 1)
    fun `actors are shutdown by the ActorSystem`() =
        runBlocking {
            val actor = ActorSystem.get(SimpleActor::class, "test-actor")

            actor.tell(Ping(1))
            assertEquals(1, ActorSystem.registry.size())

            while (ActorSystem.registry.size() > 0) {
                delay(10.milliseconds)
            }

            assertEquals(0, ActorSystem.registry.size())
        }

    data class Ping(
        val count: Int,
    ) : SimpleMessage<Unit>()

    @Disabled("Disabled slow test")
    @Test
    fun `Create a simple actor with simpleActorOf`(): Unit =
        runBlocking {
            val logger = loggerFactory.getLogger(SimpleActor::class)

            val actor =
                simpleActorOf(Unit, capacity = 0) { _, message ->
                    val ping = message as Ping
                    logger.info("start $message with state ${ping.count}")
                    delay(100.milliseconds)
                    logger.info("stop $message with state ${ping.count}")
                }

            for (i in 1..10) {
                logger.info("telling the actor ping $i")
                actor.tell(Ping(i))
            }

            delay(2000.milliseconds)
        }

    private fun stop() {
        runBlocking {
            withTimeout(2.seconds) { ActorSystem.shutdown() }
        }
    }

    private fun start(
        conf: ActorSystem.Conf =
            ActorSystem.Conf(
                registryCleanupEvery = 25.milliseconds,
                actorExpiresAfter = 10.milliseconds,
                shutdownInitialDelay = 0.milliseconds,
                shutdownPollingInterval = 1.milliseconds,
                shutdownFinalDelay = 0.milliseconds,
            ),
    ) {
        // Ensure any previous system is shut down
        runBlocking {
            withTimeout(2.seconds) { ActorSystem.shutdown() }
        }
        ActorSystem
            .conf(conf) // need to do before the registry is created, otherwise settings are ignored.
        val loggerFactory = SimpleLoggerFactory()
        val registry =
            SimpleActorRegistry(loggerFactory)
                .factoryFor(SimpleActor::class) { key ->
                    SimpleActor(key)
                }.factoryFor(StockPotActor::class) { key ->
                    val snapshotStrategyFactory =
                        EventCountSnapshotStrategyFactory(
                            InMemorySnapshotRepository(),
                            5,
                        )
                    StockPotActor(key, TestStockEventRepository(), snapshotStrategyFactory)
                }

        ActorSystem
            .register(loggerFactory)
            .register(registry)
            .start()
    }
}
