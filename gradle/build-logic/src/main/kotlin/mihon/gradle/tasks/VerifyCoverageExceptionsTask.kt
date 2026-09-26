package mihon.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The one sanctioned gap in 100% branch coverage, enforced per class: an exhaustive `when` over a
 * sealed type or enum keeps a synthetic "no subtype matched" arm that no input reaches, and
 * replacing it with `else` would trade a compile-time exhaustiveness check for coverage (decided
 * 2026-09-26). Each such class is listed in the module's `coverage-exceptions.txt` with the exact
 * number of branches it may miss and why.
 *
 * Fails when a class misses a line, misses a branch without an entry, misses more branches than
 * its entry allows, or has an entry but misses nothing -- a stale entry is a lie about the code.
 */
public abstract class VerifyCoverageExceptionsTask : DefaultTask() {

    /** Kover's XML report (JaCoCo format) for the verified variant. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val report: RegularFileProperty

    /** `<class> <max-missed-branches> <reason>` per line; `#` starts a comment. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val exceptions: RegularFileProperty

    /** Compares every class in the report with its entry and fails on the first mismatch set. */
    @TaskAction
    public fun verify() {
        val allowed = readExceptions(exceptions.get().asFile)
        val missed = readMissed(report.get().asFile)
        val problems = buildList {
            missed.forEach { (name, counts) ->
                val (lines, branches) = counts
                if (lines > 0) add("$name misses $lines line(s); lines have no exceptions")
                val cap = allowed[name]
                when {
                    branches > 0 && cap == null -> add("$name misses $branches branch(es) and has no entry")
                    cap != null && branches > cap -> add("$name misses $branches branch(es), entry allows $cap")
                }
            }
            allowed.keys.filter { (missed[it]?.second ?: 0) == 0 }
                .forEach { add("$it has an entry but misses no branch: delete the entry") }
        }
        if (problems.isNotEmpty()) {
            throw GradleException("Coverage exceptions violated:\n  " + problems.joinToString("\n  "))
        }
    }
}

// class, allowed missed branches, reason
private const val ENTRY_FIELDS = 3

/** Parses `coverage-exceptions.txt` into class name -> allowed missed branches. */
internal fun readExceptions(file: File): Map<String, Int> = file.readLines()
    .map { it.substringBefore('#').trim() }
    .filter { it.isNotEmpty() }
    .associate { line ->
        val parts = line.split(Regex("\\s+"), limit = ENTRY_FIELDS)
        require(parts.size == ENTRY_FIELDS) { "coverage-exceptions entry needs class, count and reason: '$line'" }
        parts[0].replace('.', '/') to parts[1].toInt()
    }

/** Class name -> (missed lines, missed branches) from the class-level counters of a JaCoCo report. */
internal fun readMissed(file: File): Map<String, Pair<Int, Int>> {
    val factory = DocumentBuilderFactory.newInstance()
    // The report names JaCoCo's DTD, which is not shipped; never fetch it.
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    val classes = factory.newDocumentBuilder().parse(file).getElementsByTagName("class")
    return (0 until classes.length).map { classes.item(it)!! as Element }.associate { element ->
        val counters = element.childNodes.let { nodes -> (0 until nodes.length).map { nodes.item(it) } }
            .filterIsInstance<Element>()
            .filter { it.tagName == "counter" }
            .associate { it.getAttribute("type") to it.getAttribute("missed").toInt() }
        element.getAttribute("name") to ((counters["LINE"] ?: 0) to (counters["BRANCH"] ?: 0))
    }.filterValues { (lines, branches) -> lines > 0 || branches > 0 }
}
