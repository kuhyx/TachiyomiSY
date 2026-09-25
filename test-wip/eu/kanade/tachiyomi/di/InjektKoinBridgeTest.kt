package eu.kanade.tachiyomi.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import logcat.LogPriority
import logcat.LogcatLogger
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level
import org.robolectric.RobolectricTestRunner
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.get

/** Two plain values and a counter, registered through each of the bridge's helpers. */
private class SampleModule : InjektModule {
    var built = 0

    override fun InjektRegistrar.registerInjectables() {
        addSingleton("fixed")
        addSingletonFactory { built++ }
        addFactory { StringBuilder("fresh") }
    }
}

@RunWith(RobolectricTestRunner::class)
internal class InjektKoinBridgeTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val logged = mutableListOf<Pair<LogPriority, String>>()
    private var loggable = true

    private val logger = object : LogcatLogger {
        override fun isLoggable(priority: LogPriority, tag: String): Boolean = loggable

        override fun log(priority: LogPriority, tag: String, message: String) {
            logged += priority to message
        }
    }

    @Before
    fun setUp() {
        LogcatLogger.install()
        LogcatLogger.loggers += logger
    }

    @After
    fun tearDown() {
        stopKoin()
        LogcatLogger.loggers -= logger
        LogcatLogger.uninstall()
    }

    @Test
    fun helpersRegisterEachKind() {
        val module = SampleModule()
        Injekt.importModule(module)
        InjektKoinBridge.startKoin(context)
        Injekt.get<String>() shouldBe "fixed"
        Injekt.get<Int>() shouldBe 0
        Injekt.get<Int>() shouldBe 0
        module.built shouldBe 1
        Injekt.get<StringBuilder>() shouldNotBeSameInstanceAs Injekt.get<StringBuilder>()
        Injekt.get<Application>() shouldBeSameInstanceAs context
    }

    @Test
    fun moduleIsReusedPerKey() {
        val module = SampleModule()
        InjektKoinBridge.getModule(module) shouldBeSameInstanceAs InjektKoinBridge.getModule(module)
    }

    @Test
    fun loggerMapsEveryLevel() {
        InjektKoinBridge.startKoin(context)
        val koinLogger = GlobalContext.get().logger
        Level.entries.forEach { koinLogger.display(it, "msg-$it") }
        logged.filter { it.second.startsWith("msg-") } shouldBe listOf(
            LogPriority.DEBUG to "msg-DEBUG",
            LogPriority.INFO to "msg-INFO",
            LogPriority.WARN to "msg-WARNING",
            LogPriority.ERROR to "msg-ERROR",
            LogPriority.VERBOSE to "msg-NONE",
        )
    }

    @Test
    fun quietLoggerDropsMessages() {
        InjektKoinBridge.startKoin(context)
        loggable = false
        GlobalContext.get().logger.display(Level.ERROR, "dropped")
        LogcatLogger.loggers -= logger
        LogcatLogger.uninstall()
        GlobalContext.get().logger.display(Level.ERROR, "also dropped")
        LogcatLogger.install()
        LogcatLogger.loggers += logger
        logged.none { it.second.contains("dropped") } shouldBe true
    }
}
