package exh.util

import java.util.Locale

/** Every string trimmed. */
public fun Collection<String>.trimAll(): List<String> = map { it.trim() }

/** Without blank strings. */
public fun Collection<String>.dropBlank(): List<String> = filter { it.isNotBlank() }

/** Without empty strings. */
public fun Collection<String>.dropEmpty(): List<String> = filter { it.isNotEmpty() }

private val articleRegex by lazy { "^(an|a|the) ".toRegex(RegexOption.IGNORE_CASE) }

/** Without leading English articles, for sorting. */
public fun String.removeArticles(): String = replace(articleRegex, "")

/** Trimmed, or null when blank. */
public fun String.trimOrNull(): String? = trim().nullIfBlank()

/** This string, or null when blank. */
public fun String.nullIfBlank(): String? = ifBlank { null }

/** With the first character upper-cased in [locale]. */
public fun String.capitalize(locale: Locale = Locale.getDefault()): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
