package eu.kanade.tachiyomi.source.online

import android.app.Application
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import exh.log.EHLogLevel
import exh.pref.DelegateSourcePreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type
import java.nio.file.Files
import java.util.concurrent.Executor
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.isAccessible

/** The User-Agent the injected network preferences hand out (after trimming). */
internal const val USER_AGENT: String = "source-api-test/1.0"

/** An [OkHttpClient] whose only interceptor answers every request from [body] and [code], recording each request. */
internal class CannedServer {
    val requests: MutableList<Request> = mutableListOf()
    var body: String = ""
    var code: Int = 200

    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            requests += request
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("canned")
                .body(body.toResponseBody())
                .build()
        }
        .build()
}

/**
 * Everything the HttpSource chain pulls from [Injekt], served from mocks: the network helper's
 * client is [server], the SY delegate preference reads [delegateSources] live, and [services]
 * holds any further type a test needs served. [install] swaps the global scope in and stubs the
 * Android statics the real `NetworkHelper` constructor touches; [uninstall] restores everything.
 */
internal class SourceHarness {
    var delegateSources: Boolean = true
    val server: CannedServer = CannedServer()
    val services: MutableMap<Type, Any> = mutableMapOf()
    private val tempDir: File = Files.createTempDirectory("source-api-test").toFile()
    val sharedPreferences: SharedPreferences = mockk {
        every { getInt(any(), any()) } returns 0
    }
    val application: Application = mockk {
        every { cacheDir } returns tempDir
        every { packageName } returns "eu.kanade.test"
        every { getSharedPreferences(any(), any()) } returns sharedPreferences
    }
    val cookieJar: AndroidCookieJar = mockk()
    val networkHelper: NetworkHelper = mockk {
        every { client } returns server.client
        every { isDebugBuild } returns false
        every { cookieJar } returns this@SourceHarness.cookieJar
    }
    val networkPreferences: NetworkPreferences = NetworkPreferences(
        mockk<PreferenceStore> {
            every { getBoolean("verbose_logging", any()) } returns preferenceOf(false)
            every { getInt("doh_provider", any()) } returns preferenceOf(-1)
            every { getString("default_user_agent", any()) } returns preferenceOf("  $USER_AGENT  ")
        },
    )
    val delegatePreferences: DelegateSourcePreferences = DelegateSourcePreferences(
        mockk<PreferenceStore> {
            every { getBoolean("eh_delegate_sources", any()) } returns mockk<Preference<Boolean>> {
                every { get() } answers { delegateSources }
            }
            every { getBoolean("use_jp_title", any()) } returns preferenceOf(false)
        },
    )
    private val registrar: InjektRegistrar = mockk {
        every { getInstance<Any>(any<Type>()) } answers { serve(firstArg()) }
    }
    private var previous: InjektScope? = null

    private fun serve(type: Type): Any = services[type] ?: when (type) {
        NetworkHelper::class.java -> networkHelper
        Application::class.java -> application
        NetworkPreferences::class.java -> networkPreferences
        DelegateSourcePreferences::class.java -> delegatePreferences
        else -> error("Injekt type not served by SourceHarness: $type")
    }

    /** Stubs the Android statics, seeds the EH log level and swaps [Injekt] for this harness. */
    fun install() {
        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns mockk(relaxed = true)
        mockkStatic(ContextCompat::class)
        every { ContextCompat.getMainExecutor(any()) } returns Executor { it.run() }
        EHLogLevel.init(application)
        previous = Injekt
        Injekt = InjektScope(registrar)
    }

    /** Restores the previous [Injekt] scope, clears every mock and removes the temp cache dir. */
    fun uninstall() {
        previous?.let { Injekt = it }
        unmockkAll()
        tempDir.deleteRecursively()
    }
}

/** A successful [Response] to [url] carrying [body], for driving parse helpers directly. */
internal fun cannedResponse(body: String, url: String = "https://canned.example/page"): Response =
    Response.Builder()
        .request(Request.Builder().url(url).build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("canned")
        .body(body.toResponseBody())
        .build()

/** A [Preference] that only answers [value]. */
internal fun <T> preferenceOf(value: T): Preference<T> = mockk {
    every { get() } returns value
}

/**
 * Invokes the member [name] that [owner] declares whose parameters accept [args], unwrapping the
 * reflective exception so a throwing member surfaces its own exception. Lets a test reach a
 * deprecated member without a deprecation warning, which the module treats as an error.
 */
internal fun Any.invokeDeclared(owner: KClass<*>, name: String, args: List<Any?> = emptyList()): Any? {
    val function = owner.declaredFunctions.first { candidate ->
        candidate.name == name &&
            candidate.valueParameters.size == args.size &&
            candidate.valueParameters.zip(args).all { (parameter, argument) ->
                // A type-parameter classifier (generic `I`) accepts anything; a class must match the argument.
                val expected = parameter.type.classifier as? KClass<*>
                argument == null || expected == null || expected.isInstance(argument)
            }
    }
    function.isAccessible = true
    return try {
        function.call(this, *args.toTypedArray())
    } catch (e: InvocationTargetException) {
        throw e.targetException
    }
}

/** Reads the property [name] of [owner] on this instance; the reflective read carries no deprecation warning. */
internal fun Any.readMember(owner: KClass<*>, name: String): Any? {
    val property = owner.memberProperties.first { it.name == name }
    property.isAccessible = true
    return property.getter.call(this)
}
