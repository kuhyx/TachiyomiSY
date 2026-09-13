package mihon.gradle.tasks

import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class TasksTest {
    @TempDir
    lateinit var dir: File

    private fun strings(locale: String, body: String) {
        val file = File(dir, "src/commonMain/moko-resources/$locale/strings.xml")
        file.parentFile.mkdirs()
        file.writeText(body)
    }

    @Test
    fun localesConfigListsLocales() {
        strings("base", "<resources><string name=\"a\">a</string></resources>")
        strings("pl-rPL", "<resources><string name=\"a\">a</string></resources>")
        strings("zh+Hant", "<resources><string name=\"a\">a</string></resources>")
        strings("empty", "<resources/>")
        strings("blank", "<resources>\n</resources>")
        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val task = project.tasks.register("locales", GenerateLocalesConfigTask::class.java).get()
        task.outputDir.set(File(dir, "out"))
        task.action()
        File(dir, "out/xml/locales_config.xml").readText() shouldBe """
            |<?xml version="1.0" encoding="utf-8"?>
            |<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
            |   <locale android:name="en"/>
            |   <locale android:name="pl-PL"/>
            |   <locale android:name="zh-Hant"/>
            |</locale-config>
        """.trimMargin()
    }

    @Test
    fun shortcutsPlaceholderIsReplaced() {
        val template = File(dir, "shortcuts.xml")
        template.writeText("<shortcut android:targetPackage=\"\${applicationId}\"/>")
        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val task = project.tasks.register("shortcuts", ReplaceShortcutsPlaceholderTask::class.java).get()
        task.applicationId.set("eu.kanade.tachiyomi.sy")
        task.shortcutsFile.set(template)
        task.outputDir.set(File(dir, "out"))
        task.action()
        val expected = "<shortcut android:targetPackage=\"eu.kanade.tachiyomi.sy\"/>"
        File(dir, "out/xml/shortcuts.xml").readText() shouldBe expected
    }
}
