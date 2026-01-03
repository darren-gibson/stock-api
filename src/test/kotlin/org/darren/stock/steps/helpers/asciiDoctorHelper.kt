package org.darren.stock.steps.helpers

fun String.removeAsciiDocs(): String {
    val regexToExtractContent = Regex("^\\[.*]\\n-{5}\\n(.*)\\n^-{5}.*", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
    val regexToRemoveFootnotes = Regex($$" \\(\\d+\\)$", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
    val result = regexToExtractContent.matchEntire(this)

    return if (result == null) {
        this
    } else {
        result.groups[1]!!.value.replace(regexToRemoveFootnotes, "")
    }
}
