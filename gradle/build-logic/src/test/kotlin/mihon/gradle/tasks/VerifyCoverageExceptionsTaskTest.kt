package mihon.gradle.tasks

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import mihon.gradle.catalogProject
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class VerifyCoverageExceptionsTaskTest {
    @TempDir
    lateinit var dir: File

    private fun counter(type: String, missed: Int) = """<counter type="$type" missed="$missed" covered="5"/>"""

    private fun classElement(name: String, lines: Int?, branches: Int?) = buildString {
        append("""<class name="$name"><method name="m">${counter("BRANCH", 9)}</method>""")
        lines?.let { append(counter("LINE", it)) }
        branches?.let { append(counter("BRANCH", it)) }
        append("</class>")
    }

    private fun report(vararg classes: String): File = dir.resolve("report.xml").apply {
        writeText(
            """<?xml version="1.0"?><!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">""" +
                """<report name="r"><package name="p">${classes.joinToString("")}</package></report>""",
        )
    }

    private fun exceptions(text: String): File = dir.resolve("coverage-exceptions.txt").apply { writeText(text) }

    private fun run(reportFile: File, exceptionsFile: File) {
        val task = catalogProject(dir).tasks.register("check1", VerifyCoverageExceptionsTask::class.java).get()
        task.report.set(reportFile)
        task.exceptions.set(exceptionsFile)
        task.verify()
    }

    @Test
    fun parsesEntriesAndComments() {
        val text = """
            # header

            p.Foo 2 sealed when # trailing
            p/Bar 1 enum when
        """.trimIndent()
        val entries = readExceptions(exceptions(text))
        entries shouldBe mapOf("p/Foo" to 2, "p/Bar" to 1)
    }

    @Test
    fun entryWithoutReasonIsRejected() {
        shouldThrow<IllegalArgumentException> { readExceptions(exceptions("p.Foo 1\n")) }
    }

    @Test
    fun readsClassLevelCounters() {
        val missed = readMissed(
            report(classElement("p/A", 1, 2), classElement("p/B", 0, 0), classElement("p/C", null, 3)),
        )
        missed shouldBe mapOf("p/A" to (1 to 2), "p/C" to (0 to 3))
    }

    @Test
    fun countersMayBeAbsent() {
        readMissed(report(classElement("p/D", 2, null))) shouldBe mapOf("p/D" to (2 to 0))
    }

    @Test
    fun listedMissWithinCapPasses() {
        run(report(classElement("p/Foo", 0, 1), classElement("p/Ok", 0, 0)), exceptions("p.Foo 1 sealed when\n"))
    }

    @Test
    fun missedLineAlwaysFails() {
        val error = shouldThrow<GradleException> {
            run(report(classElement("p/Foo", 1, 1), classElement("p/Plain", 2, 0)), exceptions("p.Foo 1 r\n"))
        }
        error.message shouldContain "p/Foo misses 1 line(s)"
        error.message shouldContain "p/Plain misses 2 line(s)"
    }

    @Test
    fun unlistedBranchFails() {
        val error = shouldThrow<GradleException> {
            run(report(classElement("p/Foo", 0, 1), classElement("p/New", 0, 2)), exceptions("p.Foo 1 r\n"))
        }
        error.message shouldContain "p/New misses 2 branch(es) and has no entry"
    }

    @Test
    fun branchesOverCapFail() {
        val error = shouldThrow<GradleException> { run(report(classElement("p/Foo", 0, 3)), exceptions("p.Foo 1 r\n")) }
        error.message shouldContain "entry allows 1"
    }

    @Test
    fun staleEntryFails() {
        val error = shouldThrow<GradleException> {
            val entries = """
                p.Foo 1 r
                p.Gone 1 r
                p.Lines 1 r
            """.trimIndent()
            run(report(classElement("p/Foo", 0, 1), classElement("p/Lines", 1, 0)), exceptions(entries))
        }
        error.message shouldContain "p/Gone has an entry but misses no branch"
        error.message shouldContain "p/Lines has an entry but misses no branch"
    }
}
