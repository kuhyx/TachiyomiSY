package mihon.gradle.transforms

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

/**
 * Drops every entry under `META-INF/versions/` from dependency jars before anything on the
 * consuming side sees them. Those entries exist for JDK 9+ class loaders only
 * and Android never reads them, but Android Lint scans every class in a jar
 * and reports `InvalidPackage` on the `java.net.http` use inside jsoup's
 * Java 11 variant. Removing the entries fixes the diagnosis instead of
 * silencing it.
 */
public abstract class StripMultiReleaseTransform : TransformAction<TransformParameters.None> {
    /** The dependency jar as resolved. */
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    public abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        stripMultiRelease(inputArtifact.get().asFile, outputs)
    }
}

/** Passes [jar] through untouched unless it has multi-release entries; then a stripped copy is emitted. */
internal fun stripMultiRelease(jar: File, outputs: TransformOutputs) {
    if (!hasMultiReleaseEntries(jar)) {
        outputs.file(jar)
        return
    }
    copyWithoutMultiRelease(jar, outputs.file(jar.name))
}
