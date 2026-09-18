package eu.kanade.tachiyomi.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import rx.Observable

/** The Android `actual` of [awaitSingle]: one value resolves, anything else fails. */
internal class RxExtensionTest {
    @Test
    fun awaitSingleReturnsTheOnlyValue() = runTest {
        Observable.just(4).awaitSingle() shouldBe 4
        Observable.just("only").awaitSingle() shouldBe "only"
    }

    @Test
    fun awaitSingleRejectsManyValues() = runTest {
        shouldThrow<IllegalArgumentException> { Observable.just(1, 2).awaitSingle() }
    }

    @Test
    fun awaitSingleRejectsNoValue() = runTest {
        shouldThrow<NoSuchElementException> { Observable.empty<Int>().awaitSingle() }
    }

    @Test
    fun awaitSinglePropagatesErrors() = runTest {
        val failing = Observable.error<Int>(IllegalStateException("boom"))
        val error = shouldThrow<IllegalStateException> { failing.awaitSingle() }
        error.message shouldBe "boom"
    }
}
