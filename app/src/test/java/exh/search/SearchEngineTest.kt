package exh.search

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

internal class SearchEngineTest {
    private val engine = SearchEngine()

    private fun text(value: String, exact: Boolean = false) = Text().apply {
        components += StringTextComponent(value)
        this.exact = exact
    }

    @Test
    fun escapeLikeEscapesSpecials() {
        SearchEngine.escapeLike("""a\b_c%d""") shouldBe """a\\b\_c\%d"""
    }

    @Test
    fun noNamespaceNoComponentIsNull() {
        engine.textToSubQueries(null, null).shouldBeNull()
    }

    @Test
    fun namespaceOnlyMatchesNamespace() {
        val (sql, params) = engine.textToSubQueries("art_ist", null).shouldNotBeNull()
        sql shouldContain "namespace LIKE ?"
        sql shouldNotContain "search_tags.name LIKE ?"
        params shouldBe listOf("art\\_ist")
    }

    @Test
    fun namespaceWithLenientComponent() {
        val (sql, params) = engine.textToSubQueries("artist", text("foo")).shouldNotBeNull()
        val like = "search_tags.name LIKE ?"
        sql shouldContain "AND ($like OR $like OR $like OR $like)"
        params shouldBe listOf("artist", "foo%", " foo ", " foo", "foo ")
    }

    @Test
    fun namespaceWithExactComponent() {
        val (sql, params) = engine.textToSubQueries("artist", text("foo", exact = true)).shouldNotBeNull()
        sql shouldContain "AND (search_tags.name LIKE ?)"
        params shouldBe listOf("artist", "foo")
    }

    @Test
    fun componentOnlyUnionsTitles() {
        val (sql, params) = engine.textToSubQueries(null, text("foo", exact = true)).shouldNotBeNull()
        sql shouldContain "UNION"
        sql shouldContain "search_titles"
        params shouldBe listOf("foo", "%foo%")
    }

    @Test
    fun queryToSqlJoinsAndExcludes() {
        val included = text("foo")
        val excluded = text("bar").apply { excluded = true }
        val emptyNamespace = Namespace("artist", Text())
        val nullTagNamespace = Namespace("group", null)
        val taggedNamespace = Namespace("parody", text("x"))
        val (sql, params) = engine.queryToSql(
            listOf(included, excluded, emptyNamespace, nullTagNamespace, taggedNamespace),
        )
        sql shouldContain "INNER JOIN"
        sql shouldContain "i0"
        sql shouldContain "i3"
        sql shouldContain "NOT IN"
        sql shouldContain "ORDER BY manga_id"
        params.size shouldBe 5 + 5 + 1 + 1 + 5
    }

    @Test
    fun queryToSqlUploaderAndEmpty() {
        val uploader = Namespace("uploader", text("some_one"))
        val (sql, params) = engine.queryToSql(listOf(uploader))
        sql shouldContain "meta.uploader LIKE ?"
        params shouldBe listOf("some\\_one")
        val (emptySql, emptyParams) = engine.queryToSql(emptyList())
        emptySql shouldNotContain "WHERE"
        emptyParams shouldBe emptyList()
    }

    @Test
    fun unknownComponentThrows() {
        shouldThrow<IllegalStateException> { engine.queryToSql(listOf(QueryComponent())) }
    }

    @Test
    fun parseQueryCachesAndWildcard() {
        val first = engine.parseQuery("a*")
        first shouldBeSameInstanceAs engine.parseQuery("a*")
        (first.single() as Text).components.size shouldBe 2
        val literal = engine.parseQuery("b*", enableWildcard = false)
        (literal.single() as Text).components.size shouldBe 1
    }
}
