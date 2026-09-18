package tachiyomi.domain.manga.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.TriState

internal class TriStateTest {

    // applyFilter is inline: ordinary call sites copy its body, so only a reflective call runs
    // (and covers) the compiled method itself.
    private val compiledApplyFilter = Class.forName("tachiyomi.domain.manga.model.TriStateKt")
        .getMethod("applyFilter", TriState::class.java, Function0::class.java)

    private fun applyCompiled(filter: TriState, predicate: () -> Boolean): Boolean =
        compiledApplyFilter.invoke(null, filter, predicate) as? Boolean ?: error("applyFilter returned null")

    @Test
    fun disabledAlwaysPasses() {
        applyFilter(TriState.DISABLED) { false } shouldBe true
        applyFilter(TriState.DISABLED) { true } shouldBe true
    }

    @Test
    fun enabledIsFollowsPredicate() {
        applyFilter(TriState.ENABLED_IS) { true } shouldBe true
        applyFilter(TriState.ENABLED_IS) { false } shouldBe false
    }

    @Test
    fun enabledNotNegatesPredicate() {
        applyFilter(TriState.ENABLED_NOT) { true } shouldBe false
        applyFilter(TriState.ENABLED_NOT) { false } shouldBe true
    }

    @Test
    fun compiledBodyMatchesInlined() {
        applyCompiled(TriState.DISABLED) { false } shouldBe true
        applyCompiled(TriState.DISABLED) { true } shouldBe true
        applyCompiled(TriState.ENABLED_IS) { true } shouldBe true
        applyCompiled(TriState.ENABLED_IS) { false } shouldBe false
        applyCompiled(TriState.ENABLED_NOT) { true } shouldBe false
        applyCompiled(TriState.ENABLED_NOT) { false } shouldBe true
    }
}
