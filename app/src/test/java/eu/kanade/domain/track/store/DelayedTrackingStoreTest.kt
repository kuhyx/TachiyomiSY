package eu.kanade.domain.track.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DelayedTrackingStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = DelayedTrackingStore(context)

    @Test
    fun startsEmpty() {
        store.getItems() shouldBe emptyList()
    }

    @Test
    fun keepsTheHighestChapterPerTrack() {
        store.add(1, 3.0)
        store.add(1, 2.0)
        store.add(2, 1.5)
        store.getItems().sortedBy { it.trackId } shouldBe listOf(
            DelayedTrackingStore.DelayedTrackingItem(trackId = 1, lastChapterRead = 3f),
            DelayedTrackingStore.DelayedTrackingItem(trackId = 2, lastChapterRead = 1.5f),
        )
        store.add(1, 4.0)
        store.getItems().first { it.trackId == 1L }.lastChapterRead shouldBe 4f
    }

    @Test
    fun removesATrack() {
        store.add(1, 3.0)
        store.remove(1)
        store.remove(99)
        store.getItems() shouldBe emptyList()
    }
}
