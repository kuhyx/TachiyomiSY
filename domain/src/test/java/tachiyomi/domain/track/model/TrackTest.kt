package tachiyomi.domain.track.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

internal class TrackTest {

    private val row = track()

    @Test
    fun componentsExposeFields() {
        row.component1() shouldBe 1L
        row.component2() shouldBe 10L
        row.component3() shouldBe 1L
        row.component4() shouldBe 100L
        row.component5() shouldBe null
        row.component6() shouldBe "Title"
        row.component7() shouldBe 12.5
        row.component8() shouldBe 20L
        row.component9() shouldBe 1L
        row.component10() shouldBe 8.0
        row.component11() shouldBe "https://tracker.example/100"
        row.component12() shouldBe 0L
        row.component13() shouldBe 0L
        row.component14() shouldBe false
    }

    @Test
    fun dataClassSurface() {
        row shouldBe row.copy()
        row.copy(libraryId = 5L, private = true) shouldNotBe row
        row.hashCode() shouldBe row.copy().hashCode()
        row.toString() shouldBe "Track(id=1, mangaId=10, trackerId=1, remoteId=100, libraryId=null, title=Title, " +
            "lastChapterRead=12.5, totalChapters=20, status=1, score=8.0, " +
            "remoteUrl=https://tracker.example/100, startDate=0, finishDate=0, private=false)"
    }

    @Test
    fun javaSerializationRoundTrip() {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(row) }

        val restored = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }

        restored shouldBe row
    }
}
