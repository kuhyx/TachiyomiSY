package exh.search

import java.util.Locale

// Short namespace names users may type before ':', keyed by alias.
private val NAMESPACE_ALIASES: Map<String, String> = buildMap {
    fun alias(canonical: String, vararg aliases: String) = aliases.forEach { put(it, canonical) }
    alias("artist", "a")
    alias("character", "c", "char")
    alias("female", "f")
    alias("group", "g", "creator", "circle")
    alias("language", "l", "lang")
    alias("male", "m")
    alias("parody", "p", "series")
    alias("reclass", "r")
}

/** Splits one search string into [QueryComponent]s; a parser holds state, so use each once. */
internal class QueryParser(private val enableWildcard: Boolean) {
    private val result = mutableListOf<QueryComponent>()
    private var inQuotes = false
    private val queuedRawText = StringBuilder()
    private val queuedText = mutableListOf<TextComponent>()
    private var namespace: Namespace? = null
    private var nextIsExcluded = false
    private var nextIsExact = false

    fun parse(query: String): List<QueryComponent> {
        query.lowercase(Locale.getDefault()).forEach(::feed)
        flushAll()
        return result.toList()
    }

    private fun feed(char: Char) {
        val wildcard = wildcardFor(char)
        when {
            char == '"' -> inQuotes = !inQuotes
            wildcard != null -> queueWildcard(wildcard)
            char == '-' && !inQuotes && queuedRawText.atWordStart() -> nextIsExcluded = true
            char == '$' -> nextIsExact = true
            char == ':' -> startNamespace()
            char == ' ' && !inQuotes -> flushAll()
            else -> queuedRawText.append(char)
        }
    }

    private fun wildcardFor(char: Char): TextComponent? = when {
        !enableWildcard -> null
        char == '?' || char == '_' -> SingleWildcard(char.toString())
        char == '*' || char == '%' -> MultiWildcard(char.toString())
        else -> null
    }

    private fun queueWildcard(wildcard: TextComponent) {
        flushText()
        queuedText += wildcard
    }

    private fun startNamespace() {
        flushText()
        val name = flushToText().rawTextOnly()
        namespace = Namespace(NAMESPACE_ALIASES[name] ?: name, null)
    }

    private fun flushText() {
        if (queuedRawText.isNotEmpty()) {
            queuedText += StringTextComponent(queuedRawText.toString())
            queuedRawText.setLength(0)
        }
    }

    private fun flushToText(): Text = Text().apply {
        components += queuedText
        queuedText.clear()
    }

    private fun flushAll() {
        flushText()
        if (queuedText.isEmpty() && namespace == null) return
        val component = namespace?.also { it.tag = flushToText() } ?: flushToText()
        namespace = null
        component.excluded = nextIsExcluded
        component.exact = nextIsExact
        result += component
    }

    // A leading '-' only excludes when it starts a word.
    private fun StringBuilder.atWordStart(): Boolean = isBlank() || last() == ' '
}
