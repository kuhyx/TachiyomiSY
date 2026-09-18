package exh.log

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.i18n.sy.SYMR

/** Writes the companion's private `curLogLevel`, which has no public reset. */
internal fun setCurrentLogLevel(ordinal: Int?) {
    EHLogLevel::class.java.getDeclaredField("curLogLevel").apply { isAccessible = true }.set(null, ordinal)
}

internal class EHLogLevelTest {
    @AfterEach
    fun tearDown() {
        setCurrentLogLevel(null)
    }

    @Test
    fun levelsCarryTheirStrings() {
        EHLogLevel.MINIMAL.nameRes shouldBe SYMR.strings.log_minimal
        EHLogLevel.MINIMAL.description shouldBe SYMR.strings.log_minimal_desc
        EHLogLevel.EXTRA.nameRes shouldBe SYMR.strings.log_extra
        EHLogLevel.EXTRA.description shouldBe SYMR.strings.log_extra_desc
        EHLogLevel.EXTREME.nameRes shouldBe SYMR.strings.log_extreme
        EHLogLevel.EXTREME.description shouldBe SYMR.strings.log_extreme_desc
    }

    @Test
    fun currentLevelReadsOrdinal() {
        setCurrentLogLevel(EHLogLevel.EXTRA.ordinal)
        EHLogLevel.currentLogLevel shouldBe EHLogLevel.EXTRA
    }

    @Test
    fun currentLevelRequiresInit() {
        setCurrentLogLevel(null)
        assertThrows<NullPointerException> { EHLogLevel.currentLogLevel }
    }

    @Test
    fun shouldLogComparesOrdinals() {
        setCurrentLogLevel(EHLogLevel.EXTRA.ordinal)
        EHLogLevel.shouldLog(EHLogLevel.MINIMAL) shouldBe true
        EHLogLevel.shouldLog(EHLogLevel.EXTRA) shouldBe true
        EHLogLevel.shouldLog(EHLogLevel.EXTREME) shouldBe false
    }
}
