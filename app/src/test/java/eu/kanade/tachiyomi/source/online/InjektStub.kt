package eu.kanade.tachiyomi.source.online

import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope
import java.lang.reflect.Type
import com.elvishew.xlog.LogLevel as XLogLevel

/** A plain-JVM [Injekt] graph for classes that only pull a few collaborators; [Json] is always served. */
internal class InjektStub {
    val services: MutableMap<Type, Any> = mutableMapOf(Json::class.java to Json { ignoreUnknownKeys = true })
    private val registrar: InjektRegistrar = mockk {
        every { getInstance<Any>(any<Type>()) } answers {
            services[firstArg()] ?: error("Injekt type not served by InjektStub: ${firstArg<Type>()}")
        }
    }
    private var previous: InjektScope? = null

    inline fun <reified T : Any> serve(instance: T) {
        services[T::class.java] = instance
    }

    /** Swaps [Injekt] for this graph and silences XLog. */
    fun install() {
        installSilentXLog()
        previous = Injekt
        Injekt = InjektScope(registrar)
    }

    fun uninstall() {
        previous?.let { Injekt = it }
    }
}

/** Routes XLog to a printer that drops every line, so `xLogD` calls in main code neither crash nor print. */
internal fun installSilentXLog() {
    XLog.init(LogConfiguration.Builder().logLevel(XLogLevel.ALL).build(), SilentPrinter)
}

private object SilentPrinter : Printer {
    override fun println(logLevel: Int, tag: String, msg: String) = Unit
}
