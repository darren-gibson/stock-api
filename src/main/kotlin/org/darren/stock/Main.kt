//package org.darren.stock
//
//import org.darren.stock.domain.actors.TrackedStockPotMessages
//import org.darren.stock.domain.actors.TrackedStockPotActor.Companion.trackedStockPotActor
//import io.ktor.server.application.*
//import io.ktor.server.engine.*
//import io.ktor.server.netty.*
//import kotlinx.coroutines.CompletableDeferred
//import kotlinx.coroutines.runBlocking
//import java.time.LocalDateTime
//
//private const val locationId = "1"
//private const val productId = "1"
//
//fun main() = runBlocking<Unit> {
////    val channel = trackedStockPotActor(locationId, productId)
////
////    channel.send(TrackedStockPotMessages.DeliveryEvent(LocalDateTime.now(), 100.0))
////    channel.send(TrackedStockPotMessages.SaleEvent(LocalDateTime.now(), 1.0))
////
////    val deferred = CompletableDeferred<Double>()
////
////    channel.send(TrackedStockPotMessages.GetValue(deferred))
////
////    println(deferred.await()) // prints "99"
////
////    channel.close()
//
//    embeddedServer(
//        Netty,
//        port = 8080, // This is the port on which Ktor is listening
//        host = "0.0.0.0",
//        module = Application::module
//    ).start(wait = true)
//}
//
//
//fun Application.module() {
//    configureRouting()
//}
//
//
