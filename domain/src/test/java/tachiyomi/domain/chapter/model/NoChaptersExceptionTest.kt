package tachiyomi.domain.chapter.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

internal class NoChaptersExceptionTest {

    @Test
    fun isAPlainException() {
        val exception = NoChaptersException()
        exception.shouldBeInstanceOf<Exception>()
        exception.message shouldBe null
        exception.cause shouldBe null
    }
}
