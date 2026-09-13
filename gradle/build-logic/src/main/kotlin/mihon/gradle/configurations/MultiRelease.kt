package mihon.gradle.configurations

import mihon.gradle.transforms.StripMultiReleaseTransform
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.transform.TransformSpec
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer

/** Whether a jar has had its entries under `META-INF/versions/` removed. */
public val MULTI_RELEASE_STRIPPED: Attribute<Boolean> =
    Attribute.of("multiReleaseStripped", Boolean::class.javaObjectType)

/**
 * Serves every resolved dependency jar through [StripMultiReleaseTransform].
 *
 * The Gradle-documented pattern: jars start out `multiReleaseStripped=false`,
 * the transform turns that into `true`, and every resolvable configuration
 * asks for `true`, so the stripped copy is what compilation and Android Lint
 * get. Artifacts of other types (aar, classes) carry no such attribute and
 * pass through as they are.
 *
 * The Action bodies are named extensions rather than inline: detekt's type
 * resolution runs without the sam-with-receiver plugin, and a non-Unit call on
 * the implicit receiver inside such a lambda crashes its IgnoredReturnValue rule.
 */
public fun Project.stripMultiReleaseJars() {
    dependencies.attributesSchema.attribute(MULTI_RELEASE_STRIPPED)
    dependencies.artifactTypes.maybeCreate(ArtifactTypeDefinition.JAR_TYPE).attributes.multiReleaseStripped(false)
    dependencies.registerTransform(StripMultiReleaseTransform::class.java) { jarToStrippedJar() }
    configurations.configureEach { requestStrippedJars() }
}

private fun TransformSpec<TransformParameters.None>.jarToStrippedJar() {
    from.jar().multiReleaseStripped(false)
    to.jar().multiReleaseStripped(true)
}

private fun Configuration.requestStrippedJars() {
    if (isCanBeResolved) {
        attributes.multiReleaseStripped(true)
    }
}

private fun AttributeContainer.multiReleaseStripped(value: Boolean): AttributeContainer =
    attribute(MULTI_RELEASE_STRIPPED, value)

private fun AttributeContainer.jar(): AttributeContainer =
    attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
