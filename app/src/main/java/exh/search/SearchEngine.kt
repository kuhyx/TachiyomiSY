package exh.search

internal class SearchEngine {
    private val queryCache = mutableMapOf<String, List<QueryComponent>>()

    fun textToSubQueries(
        namespace: String?,
        component: Text?,
    ): Pair<String, List<String>>? {
        val maybeLenientComponent = component?.let {
            if (!it.exact) {
                it.asLenientTagQueries()
            } else {
                listOf(it.asQuery())
            }
        }
        val componentTagQuery = maybeLenientComponent?.let {
            val params = mutableListOf<String>()
            it.joinToString(separator = " OR ", prefix = "(", postfix = ")") { q ->
                params += q
                "search_tags.name LIKE ?"
            } to params
        }
        return when {
            namespace != null -> {
                var query =
                    """
                        (SELECT ${"manga_id"} AS $COL_MANGA_ID FROM ${"search_tags"}
                        WHERE ${"namespace"} IS NOT NULL
                        AND ${"namespace"} LIKE ?
                    """.trimIndent()
                val params = mutableListOf(escapeLike(namespace))
                if (componentTagQuery != null) {
                    query += "\n    AND ${componentTagQuery.first}"
                    params += componentTagQuery.second
                }

                "$query)" to params
            }
            component != null -> {
                // Match title + tags
                val tagQuery =
                    """
                        SELECT ${"manga_id"} AS $COL_MANGA_ID FROM ${"search_tags"}
                        WHERE ${componentTagQuery!!.first}
                    """.trimIndent() to componentTagQuery.second

                val titleQuery =
                    """
                        SELECT ${"manga_id"} AS $COL_MANGA_ID FROM ${"search_titles"}
                        WHERE ${"title"} LIKE ?
                    """.trimIndent() to listOf(component.asLenientTitleQuery())

                "(${tagQuery.first} UNION ${titleQuery.first})".trimIndent() to
                    tagQuery.second + titleQuery.second
            }
            else -> {
                null
            }
        }
    }

    fun queryToSql(q: List<QueryComponent>): Pair<String, List<String>> {
        val wheres = mutableListOf<String>()
        val whereParams = mutableListOf<String>()

        val include = mutableListOf<Pair<String, List<String>>>()
        val exclude = mutableListOf<Pair<String, List<String>>>()

        q.forEach { component ->
            if (component is Namespace && component.namespace == "uploader") {
                wheres += "meta.uploader LIKE ?"
                whereParams += component.tag!!.rawTextEscapedForLike()
            } else {
                val bucket = if (component.excluded) exclude else include
                subQueryFor(component)?.let(bucket::add)
            }
        }

        val completeParams = mutableListOf<String>()
        var baseQuery =
            """
                SELECT ${"manga_id"}
                FROM ${"search_metadata"} meta
            """.trimIndent()

        include.forEachIndexed { index, pair ->
            baseQuery += "\n" +
                """
                    INNER JOIN ${pair.first} i$index
                    ON i$index.$COL_MANGA_ID = meta.${"manga_id"}
                """.trimIndent()
            completeParams += pair.second
        }

        exclude.forEach {
            wheres += """
                (meta.${"manga_id"} NOT IN ${it.first})
            """.trimIndent()
            whereParams += it.second
        }
        if (wheres.isNotEmpty()) {
            completeParams += whereParams
            baseQuery += "\nWHERE\n"
            baseQuery += wheres.joinToString("\nAND\n")
        }
        baseQuery += "\nORDER BY manga_id"

        return baseQuery to completeParams
    }

    private fun subQueryFor(component: QueryComponent): Pair<String, List<String>>? = when (component) {
        is Text -> textToSubQueries(null, component)
        // A namespace with no tag text matches every entry that has the namespace at all.
        is Namespace -> textToSubQueries(component.namespace, component.tag?.takeIf { it.components.isNotEmpty() })
        else -> error("Unknown query component!")
    }

    fun parseQuery(query: String, enableWildcard: Boolean = true): List<QueryComponent> =
        queryCache.getOrPut(query) { QueryParser(enableWildcard).parse(query) }

    companion object {
        private const val COL_MANGA_ID = "cmid"

        fun escapeLike(string: String): String {
            return string.replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("%", "\\%")
        }
    }
}
