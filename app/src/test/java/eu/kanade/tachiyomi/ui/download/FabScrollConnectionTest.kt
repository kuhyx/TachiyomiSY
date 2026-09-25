package eu.kanade.tachiyomi.ui.download

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class FabScrollConnectionTest {
    private val inner = mockk<NestedScrollConnection> {
        every { onPreScroll(any(), any()) } returns Offset(1f, 1f)
        every { onPostScroll(any(), any(), any()) } returns Offset(2f, 2f)
        coEvery { onPreFling(any()) } returns Velocity(3f, 3f)
        coEvery { onPostFling(any(), any()) } returns Velocity(4f, 4f)
    }
    private val behavior = mockk<TopAppBarScrollBehavior> { every { nestedScrollConnection } returns inner }
    private val expanded = mutableListOf<Boolean>()
    private val connection = FabScrollConnection(behavior) { expanded += it }

    @Test
    fun scrollDirectionDrivesTheFab() {
        connection.onPreScroll(Offset(0f, -5f), NestedScrollSource.UserInput) shouldBe Offset(1f, 1f)
        connection.onPreScroll(Offset(0f, 5f), NestedScrollSource.UserInput)
        connection.onPreScroll(Offset.Zero, NestedScrollSource.UserInput)
        expanded shouldBe listOf(false, true, true)
    }

    @Test
    fun theRestGoesToTheAppBar() = runBlocking {
        connection.onPostScroll(Offset.Zero, Offset.Zero, NestedScrollSource.UserInput) shouldBe Offset(2f, 2f)
        connection.onPreFling(Velocity.Zero) shouldBe Velocity(3f, 3f)
        connection.onPostFling(Velocity.Zero, Velocity.Zero) shouldBe Velocity(4f, 4f)
    }
}
