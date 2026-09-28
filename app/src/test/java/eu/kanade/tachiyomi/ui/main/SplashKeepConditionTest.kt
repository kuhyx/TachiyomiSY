package eu.kanade.tachiyomi.ui.main

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** How long the launch splash stays: a minimum, then until ready, capped at a maximum. */
internal class SplashKeepConditionTest {

    @Test
    fun keptForTheMinimumEvenWhenReady() {
        keepSplashOnScreen(elapsed = 0, ready = true) shouldBe true
        keepSplashOnScreen(elapsed = 500, ready = true) shouldBe true
    }

    @Test
    fun readyAfterTheMinimumDropsIt() {
        keepSplashOnScreen(elapsed = 501, ready = true) shouldBe false
    }

    @Test
    fun notReadyKeepsItUntilTheMaximum() {
        keepSplashOnScreen(elapsed = 501, ready = false) shouldBe true
        keepSplashOnScreen(elapsed = 5000, ready = false) shouldBe true
    }

    @Test
    fun theMaximumDropsItAnyway() {
        keepSplashOnScreen(elapsed = 5001, ready = false) shouldBe false
    }
}
