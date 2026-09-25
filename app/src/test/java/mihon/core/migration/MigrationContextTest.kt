package mihon.core.migration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import java.lang.reflect.InvocationTargetException

internal class MigrationContextTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun exposesItsArguments() {
        val context = MigrationContext(dryrun = true, previousVersion = 12)
        context.dryrun shouldBe true
        context.previousVersion shouldBe 12
    }

    @Test
    fun getReadsTheGlobalRegistry() {
        val store = InMemoryPreferenceStore()
        startKoin { modules(module { single<PreferenceStore> { store } }) }
        val context = MigrationContext(dryrun = false, previousVersion = 0)
        context.get<PreferenceStore>() shouldBe store
        context.get<String>().shouldBeNull()
    }

    @Test
    fun requireNamesTheMissingType() {
        startKoin { }
        val context = MigrationContext(dryrun = false, previousVersion = 0)
        val failure = shouldThrow<IllegalStateException> { context.require<PreferenceStore>() }
        failure.message shouldContain PreferenceStore::class.java.name
    }

    // The reified bodies only exist for inlining; calling them as plain methods runs them up to the
    // reified-type intrinsic, which is the closest a test can get to the compiled copies.
    @Test
    fun compiledBodiesRefuseToRun() {
        val context = MigrationContext(dryrun = false, previousVersion = 0)
        for (name in listOf("get", "require")) {
            val method = MigrationContext::class.java.getDeclaredMethod(name)
            method.isAccessible = true
            val failure = shouldThrow<InvocationTargetException> { method.invoke(context) }
            failure.cause!!::class shouldBe UnsupportedOperationException::class
        }
    }
}
