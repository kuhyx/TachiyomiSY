package eu.kanade.tachiyomi.network

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

internal class HttpExceptionTest {
    @Test
    fun carriesCodeInMessage() {
        val exception = HttpException(404)
        exception.code shouldBe 404
        exception.message shouldBe "HTTP error 404"
        exception.shouldBeInstanceOf<IllegalStateException>()
    }
}
