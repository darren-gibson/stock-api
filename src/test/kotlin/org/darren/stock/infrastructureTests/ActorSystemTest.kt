package org.darren.stock.infrastructureTests

import ProductLocation
import io.github.smyrgeorge.actor4k.actor.Actor
import io.github.smyrgeorge.actor4k.actor.ActorProtocol
import io.github.smyrgeorge.actor4k.actor.Behavior
import io.github.smyrgeorge.actor4k.system.ActorSystem
import io.github.smyrgeorge.actor4k.system.registry.SimpleActorRegistry
import io.github.smyrgeorge.actor4k.util.SimpleLoggerFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.darren.stock.domain.actors.StockPotActor
import org.darren.stock.persistence.InMemoryStockEventRepository
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class ActorSystemTest {
    class SimpleActor(key: String) : Actor<ActorProtocol, ActorProtocol.Response>(key) {
        override suspend fun onReceive(m: ActorProtocol): Behavior<ActorProtocol.Response> {
            TODO("Not yet implemented")
        }
    }

//    class StockPotActor(key: String) :
//        Actor<StockPotProtocol, StockPotProtocol.Reply>(key) {
//        private    val productLocation = ProductLocation.parse(key)
//        private   val locationId = productLocation.locationId
//        private   val productId = productLocation.productId
//
//        override suspend fun onReceive(m: StockPotProtocol): Behavior<StockPotProtocol.Reply> {
//            TODO("Not yet implemented")
//        }
//
////        override suspend fun onReceive(m: ActorProtocol): Behavior<ActorProtocol.Response> {
////            TODO("Not yet implemented")
////        }
//    }

    @Test
    fun `test actor system starts and shuts down when there's no actors`() {
        start()
        stop()
    }

    @Test
    fun `test actor system starts and shuts down when there's a simple actor`() {
        start()
        runBlocking {
            ActorSystem.get(SimpleActor::class, "test-actor")
        }
        stop()
    }

    @Test
    fun `test actor system starts and shuts down when there's a StockPot Actor`() {
        start()
        runBlocking {
            ActorSystem.get(StockPotActor::class, ProductLocation.of("LOC1", "PROD1").toString())
        }
        stop()
    }

    private fun stop() {
        runBlocking {
            withTimeout(2.seconds) {  ActorSystem.shutdown() }
        }
    }

    private fun start() {
        val loggerFactory = SimpleLoggerFactory()
        val registry = SimpleActorRegistry(loggerFactory).factoryFor(SimpleActor::class) { key ->
            SimpleActor(key)
        }.factoryFor(StockPotActor::class) { key ->
            StockPotActor(key, InMemoryStockEventRepository())
        }
        ActorSystem.register(loggerFactory).register(registry).start()
    }
}