package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.base.RaisedTag
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.isAccessible

/** A [Response] to [url] carrying [body] and [code], for driving parse helpers directly. */
internal fun cannedResponse(body: String, url: String = "https://canned.example/page", code: Int = 200): Response =
    Response.Builder()
        .request(Request.Builder().url(url).build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("canned")
        .body(body.toResponseBody())
        .build()

/** [html] parsed with [location] as the document location (what `asJsoup` sets from the request url). */
internal fun jsoup(html: String, location: String): Document = Jsoup.parse(html, location)

/**
 * The test resource at [path] (relative to `src/test/resources`) as text: from the classpath under
 * Gradle, else from the module directory (the test loop runs with `app/` as working directory).
 */
internal fun fixture(path: String): String {
    val resource = CannedServer::class.java.classLoader?.getResource(path)
    if (resource != null) return resource.readText()
    val file = listOf("src/test/resources/$path", "app/src/test/resources/$path").map(::File).firstOrNull(File::isFile)
    return checkNotNull(file) { "missing fixture $path" }.readText()
}

internal fun sManga(url: String, title: String = "title"): SManga = SManga(url = url, title = title)

internal fun sChapter(url: String, name: String = "chapter"): SChapter = SChapter(url = url, name = name)

internal fun rawTag(namespace: String?, name: String, type: Int): RaisedTag =
    RaisedTag(namespace = namespace, name = name, type = type)

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
