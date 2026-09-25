package exh.md.utils

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MdLangTest {
    @Test
    fun everyEntryHasCodes() {
        MdLang.entries.size shouldBe 45
        MdLang.entries.forEach { lang ->
            lang.lang.isNotBlank() shouldBe true
            lang.extLang.isNotBlank() shouldBe true
        }
        MdLang.ENGLISH.extLang shouldBe "en"
        MdLang.PORTUGUESE_BR.extLang shouldBe "pt-BR"
        MdLang.CHINESE_SIMPLIFIED.extLang shouldBe "zh-Hans"
        MdLang.SPANISH_LATAM.extLang shouldBe "es-419"
        MdLang.FILIPINO.extLang shouldBe "fil"
        MdLang.CHINESE_TRAD.extLang shouldBe "zh-Hant"
        MdLang.KAZAKH.lang shouldBe "kk"
    }

    @Test
    fun fromIsoCode() {
        MdLang.fromIsoCode("ja") shouldBe MdLang.JAPANESE
        MdLang.fromIsoCode("zh-hk") shouldBe MdLang.CHINESE_TRAD
        MdLang.fromIsoCode("xx").shouldBeNull()
    }

    @Test
    fun fromExt() {
        MdLang.fromExt("pt-BR") shouldBe MdLang.PORTUGUESE_BR
        MdLang.fromExt("en") shouldBe MdLang.ENGLISH
        MdLang.fromExt("pt-br").shouldBeNull()
    }
}
