package eu.kanade.tachiyomi.ui.base

import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import cafe.adriel.voyager.core.model.ScreenModelStore
import cafe.adriel.voyager.core.screen.Screen
import kotlin.coroutines.ContinuationInterceptor
import kotlin.reflect.KClass

private const val POLL_MS = 10_000L

/** Whether a node with [label] as its text or content description exists right now. */
internal fun ComposeTestRule.labelShown(label: String): Boolean =
    onAllNodes(hasText(label) or hasContentDescription(label), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .isNotEmpty()

/**
 * Waits for [condition] by settling the UI and sleeping in turns. Unlike `waitUntil`, every round lets
 * work that other threads posted to the main looper run, which a spinner's endless animation otherwise
 * starves (a paging load finishing on IO, a model's state emitted from `launchIO`).
 */
internal fun ComposeTestRule.poll(what: () -> String, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + POLL_MS
    while (true) {
        waitForIdle()
        if (condition()) return
        if (System.currentTimeMillis() > deadline) throw AssertionError(what())
        Thread.sleep(20)
    }
}

/** [poll]s until a node with [label] as text or content description exists. */
internal fun ComposeTestRule.pollLabel(label: String) = poll({ "no \"$label\" among ${texts()}" }) {
    labelShown(label)
}

/** [poll]s until no node shows [label]. */
internal fun ComposeTestRule.pollGone(label: String) = poll({ "\"$label\" still shown" }) {
    !labelShown(label)
}

private fun ComposeTestRule.texts(): List<String> =
    onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }

/** Reads the (usually private) property [name] of the Kotlin `object` [owner]. */
internal fun readObjectMember(owner: KClass<*>, name: String): Any? {
    val field = owner.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(owner.objectInstance)
}

/**
 * Unsticks Compose's process-wide [AndroidUiDispatcher.Main] (paging collects through it). It schedules one
 * trampoline run on the main looper and dispatches nothing more until that run happens; Robolectric drops the
 * looper's queue between tests, so a run left pending by an earlier test would starve every later one.
 */
internal fun resetUiDispatcher() {
    val dispatcher = AndroidUiDispatcher.Main[ContinuationInterceptor] as AndroidUiDispatcher
    fun field(name: String) = AndroidUiDispatcher::class.java.getDeclaredField(name).also { it.isAccessible = true }
    synchronized(checkNotNull(field("lock").get(dispatcher))) {
        (field("toRunTrampolined").get(dispatcher) as ArrayDeque<*>).clear()
        (field("toRunOnFrame").get(dispatcher) as MutableList<*>).clear()
        field("scheduledTrampolineDispatch").setBoolean(dispatcher, false)
        field("scheduledFrameDispatch").setBoolean(dispatcher, false)
    }
}

/**
 * Clicks the clickable showing [label] through its semantics action (so it also reaches nodes laid out
 * below a small test window); the last match when the label repeats. Falls back to a plain click on the
 * last node carrying the label when nothing clickable merges it.
 */
internal fun ComposeTestRule.clickLabel(label: String) {
    val labelled = hasText(label) or hasContentDescription(label)
    val clickable = onAllNodes(labelled and hasClickAction())
    if (clickable.fetchSemanticsNodes().isNotEmpty()) {
        clickable.onLast().performSemanticsAction(SemanticsActions.OnClick)
    } else {
        onAllNodes(labelled, useUnmergedTree = true).onLast().performClick()
    }
    waitForIdle()
}

/**
 * Forgets the screen models Voyager cached for [screens]. The home tabs are objects, so their keys never change:
 * a model left behind would be handed, with the previous test's collaborators, to the next test composing the tab.
 */
internal fun disposeScreenModels(vararg screens: Screen) = screens.forEach(ScreenModelStore::onDispose)
