package org.darren.stock

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import java.util.Collections

class TestLogAppender : AppenderBase<ILoggingEvent>() {
    var layout: PatternLayout? = null

    companion object {
        val events = Collections.synchronizedList(mutableListOf<String>())
    }

    override fun start() {
        layout?.start()
        super.start()
    }

    override fun append(event: ILoggingEvent) {
        if (!isStarted) return
        val formatted = layout?.doLayout(event) ?: event.message
        events.add(formatted)
    }
}
