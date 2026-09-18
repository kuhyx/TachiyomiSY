package eu.kanade.tachiyomi.network

import android.content.Context
import app.cash.quickjs.QuickJs
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JavaScriptEngineTest {
    private lateinit var quickJs: QuickJs

    @BeforeEach
    fun setUp() {
        // QuickJs loads libquickjs.so in its static initialiser, which no JVM can satisfy. Stubbing the
        // package-private loader first lets the class initialise; only then can the mock exist (even
        // instantiating a mock runs the initialiser) and create() be stubbed.
        mockkStatic(LOADER_CLASS)
        val load = Class.forName(LOADER_CLASS).getDeclaredMethod("load")
        load.isAccessible = true
        every { load.invoke(null) } returns null
        mockkStatic(QuickJs::class)
        quickJs = mockk()
        every { QuickJs.create() } returns quickJs
        every { quickJs.close() } just Runs
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun evaluatesAndClosesTheRuntime() {
        every { quickJs.evaluate("1 + 1") } returns 2
        val engine = JavaScriptEngine(mockk<Context>())
        runBlocking { engine.evaluate<Int>("1 + 1") } shouldBe 2
        verify { quickJs.close() }
    }

    @Test
    fun closesRuntimeWhenScriptFails() {
        every { quickJs.evaluate("boom") } throws IllegalArgumentException("SyntaxError")
        val engine = JavaScriptEngine(mockk<Context>())
        val error = runCatching { runBlocking { engine.evaluate<String>("boom") } }.exceptionOrNull()
        error?.message shouldBe "SyntaxError"
        verify { quickJs.close() }
    }

    private companion object {
        const val LOADER_CLASS = "app.cash.quickjs.QuickJsNativeLoader"
    }
}
