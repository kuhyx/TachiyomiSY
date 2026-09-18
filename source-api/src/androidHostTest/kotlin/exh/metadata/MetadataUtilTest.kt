package exh.metadata

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.math.pow

internal class MetadataUtilTest {
    @Test
    fun bytesBelowUnitSi() {
        MetadataUtil.humanReadableByteCount(bytes = 0, si = true) shouldBe "0 B"
        MetadataUtil.humanReadableByteCount(bytes = 999, si = true) shouldBe "999 B"
    }

    @Test
    fun bytesBelowUnitBinary() {
        MetadataUtil.humanReadableByteCount(bytes = 1000, si = false) shouldBe "1000 B"
        MetadataUtil.humanReadableByteCount(bytes = 1023, si = false) shouldBe "1023 B"
    }

    @Test
    fun siUnitsUseDecimalPrefixes() {
        val expected = listOf("1.5 kB", "1.5 MB", "1.5 GB", "1.5 TB", "1.5 PB", "1.5 EB")
        expected.forEachIndexed { index, text ->
            val bytes = 1.5 * 1000.0.pow(index + 1)
            withClue(text) { MetadataUtil.humanReadableByteCount(bytes.toLong(), si = true) shouldBe text }
        }
    }

    @Test
    fun binaryUnitsUseIecPrefixes() {
        val expected = listOf("1.5 KiB", "1.5 MiB", "1.5 GiB", "1.5 TiB", "1.5 PiB", "1.5 EiB")
        expected.forEachIndexed { index, text ->
            val bytes = 1.5 * 1024.0.pow(index + 1)
            withClue(text) { MetadataUtil.humanReadableByteCount(bytes.toLong(), si = false) shouldBe text }
        }
    }

    @Test
    fun exactUnitBoundary() {
        MetadataUtil.humanReadableByteCount(bytes = 1000, si = true) shouldBe "1.0 kB"
        MetadataUtil.humanReadableByteCount(bytes = 1024, si = false) shouldBe "1.0 KiB"
    }

    @Test
    fun parseEveryKnownUnit() {
        val cases = mapOf(
            "2 GB" to 2.0 * 1_000_000_000,
            "2 GiB" to 2.0 * 1_073_741_824,
            "2 MB" to 2.0 * 1_000_000,
            "2 MiB" to 2.0 * 1_048_576,
            "2 KB" to 2.0 * 1_000,
            "2 KiB" to 2.0 * 1_024,
            "1.5 KB" to 1_500.0,
        )
        cases.forEach { (text, bytes) ->
            withClue(text) { MetadataUtil.parseHumanReadableByteCount(text) shouldBe bytes }
        }
    }

    @Test
    fun parseUnknownUnitIsNull() {
        MetadataUtil.parseHumanReadableByteCount("2 TB") shouldBe null
        MetadataUtil.parseHumanReadableByteCount("2 B") shouldBe null
    }

    @Test
    fun ongoingSuffixesCoverBrackets() {
        MetadataUtil.ONGOING_SUFFIX.size shouldBe 15
        MetadataUtil.ONGOING_SUFFIX.count { it.contains("ongoing") } shouldBe 5
        MetadataUtil.ONGOING_SUFFIX.count { it.contains("incomplete") } shouldBe 5
        MetadataUtil.ONGOING_SUFFIX.count { it.contains("wip") } shouldBe 5
    }

    @Test
    fun exDateFormatIsMinutePrecision() {
        val date = LocalDateTime.of(2024, 3, 7, 9, 5, 59)
        MetadataUtil.EX_DATE_FORMAT.format(date) shouldBe "2024-03-07 09:05"
    }
}
