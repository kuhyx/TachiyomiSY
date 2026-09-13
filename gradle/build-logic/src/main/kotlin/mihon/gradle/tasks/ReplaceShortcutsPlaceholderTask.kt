package mihon.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/** Copies `shortcuts.xml` into generated resources with `\${applicationId}` filled in. */
public abstract class ReplaceShortcutsPlaceholderTask : DefaultTask() {

    /** The application id of the variant being built. */
    @get:Input
    public abstract val applicationId: Property<String>

    /** The template shortcuts file. */
    @get:InputFile
    public abstract val shortcutsFile: RegularFileProperty

    /** Generated resource root; the file lands in its `xml/` folder. */
    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    /** Performs the substitution and writes the result. */
    @TaskAction
    public fun action() {
        val content = shortcutsFile.asFile.get()
            .readText()
            .replace($$"${applicationId}", applicationId.get())

        outputDir.get().file("xml/shortcuts.xml").asFile.apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }
}
