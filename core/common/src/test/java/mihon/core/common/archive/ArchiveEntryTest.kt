package mihon.core.common.archive

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class ArchiveEntryTest {
    private val entry = ArchiveEntry(name = "dir/page.jpg", isFile = true, isEncrypted = false)

    @Test
    fun exposesComponents() {
        entry.name shouldBe "dir/page.jpg"
        entry.isFile shouldBe true
        entry.isEncrypted shouldBe false
        val (name, isFile, isEncrypted) = entry
        name shouldBe "dir/page.jpg"
        isFile shouldBe true
        isEncrypted shouldBe false
    }

    @Test
    fun equalsAndHashCodeUseAllFields() {
        val same = ArchiveEntry(name = "dir/page.jpg", isFile = true, isEncrypted = false)
        entry shouldBe same
        entry.hashCode() shouldBe same.hashCode()
        entry shouldNotBe entry.copy(name = "other")
        entry shouldNotBe entry.copy(isFile = false)
        entry shouldNotBe entry.copy(isEncrypted = true)
        val nothing: Any? = null
        (entry == nothing) shouldBe false
        val text: Any = "dir/page.jpg"
        (entry == text) shouldBe false
    }

    @Test
    fun copyKeepsUnchangedFields() {
        val copy = entry.copy(isEncrypted = true)
        copy.name shouldBe "dir/page.jpg"
        copy.isFile shouldBe true
        copy.isEncrypted shouldBe true
        entry.copy() shouldBe entry
    }

    @Test
    fun toStringListsFields() {
        entry.toString() shouldBe "ArchiveEntry(name=dir/page.jpg, isFile=true, isEncrypted=false)"
    }
}
