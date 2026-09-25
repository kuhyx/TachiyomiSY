package eu.kanade.tachiyomi.util.view

import android.content.Context
import android.graphics.Rect
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.R
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ViewExtensionsTest {

    private val context: Context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.Theme_Tachiyomi,
    )

    @Test
    fun popupMenusInflateAndDispatch() {
        val view = FrameLayout(context)
        var initialised = false
        val clicked = mutableListOf<Int>()
        val popup = view.popupMenu(
            menuRes = R.menu.download_single,
            initMenu = { initialised = true },
            onMenuItemClick = { clicked += itemId },
        )
        initialised shouldBe true
        popup.menu.size() shouldBe countOf(popup.menu)
        val item = popup.menu.getItem(0)
        popup.setOnMenuItemClickListener(null)
        clicked shouldBe emptyList()
        item.itemId shouldBe popup.menu.getItem(0).itemId
    }

    @Test
    fun popupMenusWorkWithoutAn() {
        val view = FrameLayout(context)
        val popup = view.popupMenu(menuRes = R.menu.download_single) { }
        popup.menu.size() shouldBe countOf(popup.menu)
    }

    // popupMenu is inline, so its own lines belong to the compiled copy that reflection can call.
    @Test
    fun theCompiledPopupMenuBodyRuns() {
        val method = Class.forName("eu.kanade.tachiyomi.util.view.ViewExtensionsKt").declaredMethods
            .single { it.name == "popupMenu" && it.parameterTypes.size == 4 }
        method.isAccessible = true
        val clicked = mutableListOf<Int>()
        val initialised = mutableListOf<Int>()
        val init: (Menu) -> Unit = { initialised += it.size() }
        val onClick: (MenuItem) -> Unit = { clicked += it.itemId }
        val popup = method.invoke(null, FrameLayout(context), R.menu.download_single, init, onClick) as PopupMenu
        initialised.size shouldBe 1
        popup.menu.performIdentifierAction(popup.menu.getItem(0).itemId, 0) shouldBe true
        clicked shouldBe listOf(popup.menu.getItem(0).itemId)
        val withDefault = Class.forName("eu.kanade.tachiyomi.util.view.ViewExtensionsKt").declaredMethods
            .single { it.name == "popupMenu\u0024default" }
        withDefault.isAccessible = true
        withDefault.invoke(null, FrameLayout(context), R.menu.download_single, null, onClick, 2, null)
        initialised.size shouldBe 1
    }

    @Test
    fun visibilityOnScreenNeedsAShown() {
        val hidden: View? = null
        hidden.isVisibleOnScreen() shouldBe false
        val offScreen = mockk<View>()
        every { offScreen.isShown } returns false
        offScreen.isVisibleOnScreen() shouldBe false
        val onScreen = mockk<View>()
        every { onScreen.isShown } returns true
        every { onScreen.getGlobalVisibleRect(any()) } answers {
            firstArg<Rect>().set(0, 0, 10, 10)
            true
        }
        onScreen.isVisibleOnScreen() shouldBe true
        val outside = mockk<View>()
        every { outside.isShown } returns true
        every { outside.getGlobalVisibleRect(any()) } answers {
            firstArg<Rect>().set(-100, -100, -50, -50)
            true
        }
        outside.isVisibleOnScreen() shouldBe false
    }

    private fun countOf(menu: Menu): Int {
        var count = 0
        for (index in 0 until menu.size()) {
            val item: MenuItem = menu.getItem(index)
            if (item.itemId != 0) count++
        }
        return count
    }
}
