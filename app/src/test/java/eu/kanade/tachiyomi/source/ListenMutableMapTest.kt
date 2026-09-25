package eu.kanade.tachiyomi.source

import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ListenMutableMapTest {
    private var notified = 0
    private val backing = mutableMapOf<String, Int>()
    private val map = ListenMutableMap(backing) { notified++ }

    @Test
    fun putNotifiesOnlyNewKeys() {
        map.put("a", 1).shouldBeNull()
        notified shouldBe 1
        map.put("a", 2) shouldBe 1
        notified shouldBe 1
        backing shouldContainExactly mapOf("a" to 2)
    }

    @Test
    fun putAllAlwaysNotifies() {
        map.putAll(mapOf("a" to 1))
        map.putAll(emptyMap())
        notified shouldBe 2
    }

    @Test
    fun removeNotifiesOnlyPresentKeys() {
        map["a"] = 1
        map.remove("missing").shouldBeNull()
        notified shouldBe 1
        map.remove("a") shouldBe 1
        notified shouldBe 2
    }

    @Test
    fun clearNotifiesAndDelegates() {
        map["a"] = 1
        map.clear()
        notified shouldBe 2
        backing.isEmpty() shouldBe true
        map.size shouldBe 0
        map.containsKey("a") shouldBe false
    }
}
