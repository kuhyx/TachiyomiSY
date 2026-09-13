package exh.util

import java.util.Locale

public fun Collection<String>.trimAll(): List<String> = map { it.trim() }
public fun Collection<String>.dropBlank(): List<String> = filter { it.isNotBlank() }
public fun Collection<String>.dropEmpty(): List<String> = filter { it.isNotEmpty() }

private val articleRegex by lazy { "^(an|a|the) ".toRegex(RegexOption.IGNORE_CASE) }

public fun String.removeArticles(): String {
    return replace(articleRegex, "")
}

public fun String.trimOrNull(): String? = trim().nullIfBlank()

public fun String.nullIfBlank(): String? = ifBlank { null }

public fun String.capitalize(locale: Locale = Locale.getDefault()): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
