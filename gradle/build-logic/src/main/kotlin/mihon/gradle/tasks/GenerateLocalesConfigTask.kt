package mihon.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/** Writes `res/xml/locales_config.xml` from the moko-resources string folders. */
public abstract class GenerateLocalesConfigTask : DefaultTask() {

    /** Injected by Gradle; used to walk the resource tree. */
    @get:Inject
    public abstract val objectFactory: ObjectFactory

    /** Generated resource root; the file lands in its `xml/` folder. */
    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    /** Collects every non-empty `strings.xml` locale and writes the config. */
    @TaskAction
    public fun action() {
        val locales = objectFactory.fileTree()
            .from("src/commonMain/moko-resources")
            .matching { include("**/strings.xml") }
            .asSequence()
            .filterNot { it.readText().contains(emptyResourcesElement) }
            .map {
                it.parentFile.name
                    .replace("base", "en")
                    .replace("-r", "-")
                    .replace("+", "-")
            }
            .distinct()
            .sorted()
            .joinToString("\n") { "|   <locale android:name=\"$it\"/>" }

        val content = """
            |<?xml version="1.0" encoding="utf-8"?>
            |<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
            $locales
            |</locale-config>
        """.trimMargin()

        outputDir.get().file("xml/locales_config.xml").asFile.apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }
}

private val emptyResourcesElement = "<resources>\\s*</resources>|<resources\\s*/>".toRegex()
