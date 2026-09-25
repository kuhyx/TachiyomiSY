package exh.eh.tags

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class CosplayerTest {
    @Test
    fun getTags1SizeFirstLast() {
        val tags = Cosplayer.getTags1()
        tags.size shouldBe 539
        tags.first() shouldBe "cosplayer:9797san"
        tags.last() shouldBe "cosplayer:zyunka mukhina"
    }

    @Test
    fun getTagsConcatenatesLists() {
        val all = Cosplayer.getTags()
        all.size shouldBe 3
        all.sumOf { it.size } shouldBe 539
    }
}
