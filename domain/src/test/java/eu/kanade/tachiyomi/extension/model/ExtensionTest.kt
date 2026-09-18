package eu.kanade.tachiyomi.extension.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.StubSource

internal class ExtensionTest {

    private val store = ExtensionStore(
        indexUrl = "https://store.example/index.min.json",
        name = "Store",
        badgeLabel = "S",
        signingKey = "abc",
        contact = ExtensionStore.Contact(website = "https://store.example", discord = null),
        isLegacy = false,
        extensionListUrl = null,
    )
    private val storeSource =
        Extension.Available.Source(id = 1L, lang = "en", name = "Src", baseUrl = "https://src.example")

    @Test
    fun installedDefaults() {
        val installed = Extension.Installed(
            name = "Ext",
            pkgName = "eu.kanade.tachiyomi.extension.en.ext",
            versionName = "1.4.2",
            versionCode = 42L,
            libVersion = 1.5,
            lang = "en",
            isNsfw = false,
            pkgFactory = null,
            sources = emptyList(),
            icon = null,
            isShared = false,
        )

        installed.hasUpdate shouldBe false
        installed.isObsolete shouldBe false
        installed.store shouldBe null
        installed.isRedundant shouldBe false
        installed.shouldBeInstanceOf<Extension>()
        installed.lang shouldBe "en"
    }

    @Test
    fun installedDataClassSurface() {
        val installed = Extension.Installed(
            name = "Ext",
            pkgName = "pkg",
            versionName = "1.4.2",
            versionCode = 42L,
            libVersion = 1.5,
            lang = "en",
            isNsfw = true,
            pkgFactory = "Factory",
            sources = listOf(StubSource(id = 1L, lang = "en", name = "Src")),
            icon = null,
            hasUpdate = true,
            isObsolete = true,
            isShared = true,
            store = store,
            isRedundant = true,
        )

        installed shouldBe installed.copy()
        installed.copy(versionCode = 43L) shouldNotBe installed
        installed.hashCode() shouldBe installed.copy().hashCode()
        installed.toString().startsWith("Installed(name=Ext, pkgName=pkg, versionName=1.4.2") shouldBe true
        installed.component8() shouldBe "Factory"
        installed.component14() shouldBe store
        installed.component15() shouldBe true
    }

    @Test
    fun availableDataClassSurface() {
        val available = Extension.Available(
            name = "Ext",
            pkgName = "pkg",
            versionName = "1.4.2",
            versionCode = 42L,
            libVersion = 1.5,
            lang = "en",
            isNsfw = false,
            sources = listOf(storeSource),
            apkUrl = "https://store.example/ext.apk",
            iconUrl = "https://store.example/ext.png",
            store = store,
        )

        available shouldBe available.copy()
        available.copy(isNsfw = true) shouldNotBe available
        available.hashCode() shouldBe available.copy().hashCode()
        available.toString() shouldBe "Available(name=Ext, pkgName=pkg, versionName=1.4.2, versionCode=42, " +
            "libVersion=1.5, lang=en, isNsfw=false, sources=[$storeSource], apkUrl=https://store.example/ext.apk, " +
            "iconUrl=https://store.example/ext.png, store=$store)"
        available.component8() shouldBe listOf(storeSource)
        available.component11() shouldBe store
        available.shouldBeInstanceOf<Extension>()
    }

    @Test
    fun storeSourceBecomesStub() {
        val stub = storeSource.toStubSource()

        stub.id shouldBe 1L
        stub.lang shouldBe "en"
        stub.name shouldBe "Src"
        stub.toString() shouldBe "Src (EN)"
    }

    @Test
    fun storeSourceDataClassSurface() {
        storeSource.component1() shouldBe 1L
        storeSource.component2() shouldBe "en"
        storeSource.component3() shouldBe "Src"
        storeSource.component4() shouldBe "https://src.example"
        storeSource shouldBe storeSource.copy()
        storeSource.copy(id = 2L) shouldNotBe storeSource
        storeSource.hashCode() shouldBe storeSource.copy().hashCode()
        storeSource.toString() shouldBe "Source(id=1, lang=en, name=Src, baseUrl=https://src.example)"
    }

    @Test
    fun untrustedDefaults() {
        val untrusted = Extension.Untrusted(
            name = "Ext",
            pkgName = "pkg",
            versionName = "1.4.2",
            versionCode = 42L,
            libVersion = 1.5,
            signatureHash = "deadbeef",
        )

        untrusted.lang shouldBe null
        untrusted.isNsfw shouldBe false
        untrusted.shouldBeInstanceOf<Extension>()
    }

    @Test
    fun untrustedDataClassSurface() {
        val untrusted = Extension.Untrusted(
            name = "Ext",
            pkgName = "pkg",
            versionName = "1.4.2",
            versionCode = 42L,
            libVersion = 1.5,
            signatureHash = "deadbeef",
            lang = "all",
            isNsfw = true,
        )

        untrusted shouldBe untrusted.copy()
        untrusted.copy(signatureHash = "cafe") shouldNotBe untrusted
        untrusted.hashCode() shouldBe untrusted.copy().hashCode()
        untrusted.toString() shouldBe "Untrusted(name=Ext, pkgName=pkg, versionName=1.4.2, versionCode=42, " +
            "libVersion=1.5, signatureHash=deadbeef, lang=all, isNsfw=true)"
        untrusted.component6() shouldBe "deadbeef"
        untrusted.component7() shouldBe "all"
        untrusted.component8() shouldBe true
    }
}
