package mihon.gradle.configurations

import mihon.gradle.extensions.mihonx
import org.gradle.api.Project
import tapmoc.configureJavaCompatibility

/** Java and Kotlin target the JDK pinned in the catalog. */
public fun Project.configureKotlin() {
    configureJavaCompatibility(mihonx.versions.java.get().toInt())
}
