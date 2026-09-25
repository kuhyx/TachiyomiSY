package eu.kanade.tachiyomi.extension.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal val installedExtension: Extension.Installed = Extension.Installed(
    name = "Ext",
    pkgName = "pkg.one",
    versionName = "1.0",
    versionCode = 3L,
    libVersion = 1.5,
    lang = "en",
    isNsfw = false,
    pkgFactory = null,
    sources = emptyList(),
    icon = null,
    isShared = false,
)

internal val untrustedExtension: Extension.Untrusted = Extension.Untrusted(
    name = "Ext",
    pkgName = "pkg.one",
    versionName = "1.0",
    versionCode = 3L,
    libVersion = 1.5,
    signatureHash = "abc",
)

internal class LoadResultTest {
    @Test
    fun successCarriesTheExtension() {
        val result = LoadResult.Success(installedExtension)
        result.extension.pkgName shouldBe "pkg.one"
        result.copy(extension = installedExtension.copy(pkgName = "pkg.two")).extension.pkgName shouldBe "pkg.two"
        (result == LoadResult.Success(installedExtension)) shouldBe true
        result.hashCode() shouldBe LoadResult.Success(installedExtension).hashCode()
        result.toString().contains("Success") shouldBe true
    }

    @Test
    fun untrustedCarriesTheExtension() {
        val result = LoadResult.Untrusted(untrustedExtension)
        result.extension.signatureHash shouldBe "abc"
        result.copy(extension = untrustedExtension.copy(signatureHash = "def")).extension.signatureHash shouldBe "def"
        (result == LoadResult.Untrusted(untrustedExtension)) shouldBe true
        result.hashCode() shouldBe LoadResult.Untrusted(untrustedExtension).hashCode()
        result.toString().contains("Untrusted") shouldBe true
    }

    @Test
    fun errorIsASingleton() {
        val error: LoadResult = LoadResult.Error
        (error === LoadResult.Error) shouldBe true
        error.toString() shouldBe "Error"
    }
}
