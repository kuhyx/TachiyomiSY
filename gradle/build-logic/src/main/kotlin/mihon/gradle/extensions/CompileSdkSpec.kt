package mihon.gradle.extensions

import com.android.build.api.dsl.CompileSdkSpec
import com.android.build.api.dsl.CompileSdkVersion
import org.gradle.api.provider.Provider

/** Parses `major` or `major.minor` from the catalog into a compile SDK release. */
internal fun CompileSdkSpec.releaseOf(version: Provider<String>): CompileSdkVersion {
    val (major, minor) = version.get().split(".", limit = 2).let {
        it[0].toInt() to it.getOrNull(1)?.toInt()
    }
    return release(major) { minorApiLevel = minor }
}
