package org.darren.stock.steps.helpers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.actor4k.system.ActorSystem
import kotlin.time.Duration

object ActorSystemTestConfig {
    private val logger = KotlinLogging.logger {}

    data class Overrides(
        val actorExpiresAfter: Duration? = null,
        val registryCleanupEvery: Duration? = null,
    )

    @Volatile
    private var overrides: Overrides? = null

    fun setOverrides(value: Overrides) {
        logger.info { "===> ActorSystemTestConfig.setOverrides called: $value <==" }
        overrides = value
    }

    fun clearOverrides() {
        logger.info { "===> ActorSystemTestConfig.clearOverrides called <==" }
        overrides = null
    }

    fun applyTo(baseConf: ActorSystem.Conf): ActorSystem.Conf {
        val result =
            overrides?.let {
                logger.info { "===> Applying overrides: $it to baseConf: $baseConf <==" }
                baseConf.copy(
                    actorExpiresAfter = it.actorExpiresAfter ?: baseConf.actorExpiresAfter,
                    registryCleanupEvery = it.registryCleanupEvery ?: baseConf.registryCleanupEvery,
                )
            } ?: baseConf
        logger.info { "===> Result configuration: expiresAfter=${result.actorExpiresAfter}, cleanupEvery=${result.registryCleanupEvery} <==" }
        return result
    }
}
