package exh.uconfig

import exh.source.ExhPreferences
import io.kotest.matchers.shouldBe
import okhttp3.FormBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference

internal class EhUConfigBuilderTest {
    private val preferences = ExhPreferences(InMemoryPreferenceStore())

    @BeforeEach
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single { preferences } }) }
    }

    @AfterEach
    fun tearDown() = stopKoin()

    private fun FormBody.toMap(): Map<String, String> = (0 until size).associate { name(it) to value(it) }

    private fun build(perks: EHHathPerksResponse = EHHathPerksResponse()) = EhUConfigBuilder().build(perks).toMap()

    @Test
    fun defaultsProduceTheBaseForm() {
        val form = build()
        form["xr"] shouldBe "0"
        form["uh"] shouldBe "0"
        form["tl"] shouldBe "0"
        form["oi"] shouldBe "0"
        form["ft"] shouldBe "0"
        form["wt"] shouldBe "0"
        form["tr"] shouldBe "0"
        form["rc"] shouldBe "0"
        form["dm"] shouldBe "2"
        form["qb"] shouldBe "0"
        form["pp"] shouldBe "1"
        form["apply"] shouldBe "Apply"
        form["xl_1024"] shouldBe ""
        form["xl_1"] shouldBe ""
        form["ct_doujinshi"] shouldBe "0"
        form.size shouldBe 11 + 1 + 2 + 16 * 3 + 10
    }

    @Test
    fun imageQualityMapsEveryValue() {
        mapOf(
            "OVRS_2400" to "5",
            "ovrs_1600" to "4",
            "high" to "3",
            "med" to "2",
            "low" to "1",
            "auto" to "0",
            "weird" to "0",
        ).forEach { (quality, expected) ->
            preferences.imageQuality.set(quality)
            build()["xr"] shouldBe expected
        }
    }

    @Test
    fun hathAndTitleAndOriginalsMap() {
        preferences.useHentaiAtHome.set(2)
        build()["uh"] shouldBe "2"
        preferences.useHentaiAtHome.set(1)
        build()["uh"] shouldBe "1"
        preferences.useHentaiAtHome.set(7)
        build()["uh"] shouldBe "0"
        preferences.useJapaneseTitle.set(true)
        preferences.exhUseOriginalImages.set(true)
        preferences.ehTagFilterValue.set(-500)
        preferences.ehTagWatchingValue.set(300)
        val form = build()
        form["tl"] shouldBe "1"
        form["oi"] shouldBe "1"
        form["ft"] shouldBe "-500"
        form["wt"] shouldBe "300"
    }

    @Test
    fun perksUnlockRowsAndCounts() {
        build(
            EHHathPerksResponse().apply {
                moreThumbs = true
                pagingEnlargementI = true
            },
        ).let {
            it["tr"] shouldBe "1"
            it["rc"] shouldBe "1"
        }
        build(
            EHHathPerksResponse().apply {
                thumbsUp = true
                pagingEnlargementII = true
            },
        ).let {
            it["tr"] shouldBe "2"
            it["rc"] shouldBe "2"
        }
        build(
            EHHathPerksResponse().apply {
                allThumbs = true
                pagingEnlargementIII = true
            },
        ).let {
            it["tr"] shouldBe "3"
            it["rc"] shouldBe "3"
        }
    }

    @Test
    fun languagesAndCategories() {
        preferences.exhSettingsLanguages.set(
            listOf("true*true*true") + List(15) { "false*true*false" } + listOf("false*false*true"),
        )
        preferences.exhEnabledCategories.set("true,false,true,false,true,false,true,false,true,false")
        val form = build()
        // Japanese has no "original" checkbox.
        form.containsKey("xl_0") shouldBe false
        form["xl_1024"] shouldBe "checked"
        form["xl_2048"] shouldBe "checked"
        form["xl_1"] shouldBe ""
        form["xl_1025"] shouldBe "checked"
        form["xl_2049"] shouldBe ""
        form["xl_255"] shouldBe ""
        form["xl_2303"] shouldBe "checked"
        form["ct_doujinshi"] shouldBe "1"
        form["ct_manga"] shouldBe "0"
        form["ct_misc"] shouldBe "0"
    }

    private fun Preference<String>.set(lines: List<String>) = set(lines.joinToString("\n"))

    @Test
    fun defaultGridHasSeventeenRows() {
        preferences.exhSettingsLanguages.get().split("\n").size shouldBe EhLanguage.entries.size
    }
}
