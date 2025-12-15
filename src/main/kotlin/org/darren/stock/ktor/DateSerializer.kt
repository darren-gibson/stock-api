package org.darren.stock.ktor

import kotlinx.serialization.KSerializer
import java.time.LocalDateTime
import org.darren.stock.util.DateSerializer as UtilDateSerializer

@Deprecated("Use org.darren.stock.util.DateSerializer instead")
object DateSerializer : KSerializer<LocalDateTime> by UtilDateSerializer
