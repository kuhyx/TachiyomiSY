package eu.kanade.tachiyomi.data.backup.create.creators

import android.app.Application
import android.content.SharedPreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.source.service.SourceManager

private val everyValueType: Map<String, Any> = mapOf(
    "int" to 1,
    "long" to 2L,
    "float" to 3F,
    "string" to "s",
    "boolean" to true,
    "stringSet" to setOf("a"),
    "other" to 1.5,
    Preference.appStateKey("state") to 4,
    Preference.privateKey("secret") to "hidden",
)

private fun configurableSource(id: Long): ConfigurableSource = mockk<ConfigurableSource>().also {
    every { it.id } returns id
}

internal class PreferenceBackupCreatorTest {

    private val sourceManager = mockk<SourceManager>()
    private val preferenceStore = mockk<PreferenceStore>()
    private val application = mockk<Application>()

    @BeforeEach
    fun setUp() {
        every { preferenceStore.getAll() } returns everyValueType
        every { application.getSharedPreferences(any(), any()) } answers {
            mockk<SharedPreferences>().also { every { it.all } returns everyValueType }
        }
        startKoin {
            modules(
                module {
                    single { sourceManager }
                    single { preferenceStore }
                    single { application }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun creator(): PreferenceBackupCreator =
        PreferenceBackupCreator(sourceManager = sourceManager, preferenceStore = preferenceStore)

    @Test
    fun appDropsAppStateAndPrivate() {
        val prefs = creator().createApp(includePrivatePreferences = false)
        prefs.map { it.key } shouldContainExactly listOf("int", "long", "float", "string", "boolean", "stringSet")
    }

    @Test
    fun appKeepsPrivateWhenAsked() {
        val prefs = creator().createApp(includePrivatePreferences = true)
        prefs.map { it.key } shouldContainExactly listOf(
            "int",
            "long",
            "float",
            "string",
            "boolean",
            "stringSet",
            Preference.privateKey("secret"),
        )
    }

    @Test
    fun everyValueTypeIsMapped() {
        val prefs = creator().createApp(includePrivatePreferences = false).associateBy { it.key }
        prefs.getValue("int").value shouldBe IntPreferenceValue(1)
        prefs.getValue("long").value shouldBe LongPreferenceValue(2L)
        prefs.getValue("float").value shouldBe FloatPreferenceValue(3F)
        prefs.getValue("string").value shouldBe StringPreferenceValue("s")
        prefs.getValue("boolean").value shouldBe BooleanPreferenceValue(true)
        prefs.getValue("stringSet").value shouldBe StringSetPreferenceValue(setOf("a"))
    }

    @Test
    fun injectsCollaboratorsByDefault() {
        PreferenceBackupCreator().createApp(includePrivatePreferences = false).isNotEmpty() shouldBe true
    }

    @Test
    fun sourceSkipsPlainSources() {
        every { sourceManager.getAll() } returns listOf(mockk<Source>())
        creator().createSource(includePrivatePreferences = false) shouldBe emptyList()
    }

    @Test
    fun sourceUsesPreferenceKey() {
        every { sourceManager.getAll() } returns listOf(configurableSource(id = 7L))
        val prefs = creator().createSource(includePrivatePreferences = false)
        prefs.single().sourceKey shouldBe "source_7"
        prefs.single().prefs.map { it.key } shouldContainExactly
            listOf("int", "long", "float", "string", "boolean", "stringSet")
    }

    @Test
    fun sourceDropsEmptyPreferences() {
        every { application.getSharedPreferences(any(), any()) } answers {
            mockk<SharedPreferences>().also { every { it.all } returns emptyMap() }
        }
        every { sourceManager.getAll() } returns listOf(configurableSource(id = 7L))
        creator().createSource(includePrivatePreferences = false) shouldBe emptyList()
    }

    @Test
    fun sourceKeepsPrivateWhenAsked() {
        every { sourceManager.getAll() } returns listOf(configurableSource(id = 8L))
        val prefs = creator().createSource(includePrivatePreferences = true)
        prefs.single().prefs.map { it.key } shouldContainExactly listOf(
            "int",
            "long",
            "float",
            "string",
            "boolean",
            "stringSet",
            Preference.privateKey("secret"),
        )
    }
}
