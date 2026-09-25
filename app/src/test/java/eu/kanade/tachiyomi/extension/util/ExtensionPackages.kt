package eu.kanade.tachiyomi.extension.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Bundle
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

/** The metadata keys an extension package carries, as [ExtensionLoader] reads them. */
internal fun extensionMetaData(
    name: String? = "Ext Name",
    libVersion: Float = 1.6f,
    nsfw: Int = 0,
    contentWarning: Int = 0,
    sourceClass: String? = null,
    sourceFactory: String? = null,
): Bundle = Bundle().apply {
    name?.let { putString(ExtensionLoader.METADATA_NAME, it) }
    putFloat(ExtensionLoader.METADATA_EXTENSION_LIB, libVersion)
    putInt(ExtensionLoader.METADATA_NSFW, nsfw)
    putInt(ExtensionLoader.METADATA_CONTENT_WARNING, contentWarning)
    sourceClass?.let { putString("tachiyomi.extension.class", it) }
    sourceFactory?.let { putString(ExtensionLoader.METADATA_SOURCE_FACTORY, it) }
}

/** A [PackageInfo] shaped like an extension APK; [feature] false drops the extension feature flag. */
internal fun extensionPackage(
    pkgName: String = "eu.kanade.tachiyomi.extension.test",
    versionName: String? = "1.6.0",
    versionCode: Int = 4,
    metaData: Bundle? = extensionMetaData(),
    feature: Boolean = true,
    signatures: List<String>? = listOf("cert"),
    multipleSigners: Boolean = false,
    signed: Boolean = true,
    sourceDir: String? = "/data/app/ext.apk",
): PackageInfo = PackageInfo().apply {
    packageName = pkgName
    this.versionName = versionName
    longVersionCode = versionCode.toLong()
    applicationInfo = ApplicationInfo().apply {
        packageName = pkgName
        this.metaData = metaData
        this.sourceDir = sourceDir
        publicSourceDir = sourceDir
    }
    if (feature) {
        reqFeatures = arrayOf(FeatureInfo().apply { name = ExtensionLoader.EXTENSION_FEATURE })
    }
    if (signed) {
        val certs = signatures?.map { Signature(it.toByteArray()) }?.toTypedArray()
        signingInfo = mockk<SigningInfo> {
            every { hasMultipleSigners() } returns multipleSigners
            every { apkContentsSigners } returns if (multipleSigners) certs else null
            every { signingCertificateHistory } returns if (multipleSigners) null else certs
        }
    }
}

/** A [Source] that answers nothing, so a test class can be named by an extension's metadata. */
internal abstract class EmptyTestSource : Source {
    override val id: Long = 1L
    override val name: String = "loaded"
    override val supportsLatest: Boolean = false

    override suspend fun getPopularManga(page: Int): MangasPage = MangasPage(emptyList(), false)

    override suspend fun getLatestUpdates(page: Int): MangasPage = MangasPage(emptyList(), false)

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        MangasPage(emptyList(), false)

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = SMangaUpdate(manga, chapters)

    override suspend fun getPageList(chapter: SChapter): List<Page> = emptyList()
}

/** A [Source] an extension package can name in its metadata; loaded by name through a class loader. */
internal class LoadedTestSource : EmptyTestSource() {
    override val lang: String = "en"
}

/** A second source in another language, so a package can report `all`. */
internal class OtherLangTestSource : EmptyTestSource() {
    override val lang: String = "fr"
}

/** A factory an extension package can name instead of a single source class. */
internal class LoadedTestFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(LoadedTestSource(), OtherLangTestSource())
}

/** Named by a package whose metadata points at something that is neither a source nor a factory. */
internal class NotASourceAtAll

/**
 * The package manager as [ExtensionLoader] uses it, answering from the maps the test fills. The
 * Robolectric shadow copies every [PackageInfo] through a [android.os.Parcel], which a mocked
 * [SigningInfo] cannot survive, so the whole context is mocked instead.
 */
internal class FakePackages {
    private val shared = mutableMapOf<String, PackageInfo>()
    private val archives = mutableMapOf<String, PackageInfo>()

    /** The private extension directory the fake context reports. */
    val filesDir: File = Files.createTempDirectory("ext-loader").toFile()

    /** Every broadcast the code under test sent. */
    val broadcasts: MutableList<Intent> = mutableListOf()

    val packageManager: PackageManager = mockk(relaxed = true)
    val context: Context = mockk(relaxed = true)

    init {
        every { packageManager.getInstalledPackages(any<Int>()) } answers { shared.values.toList() }
        every {
            packageManager.getInstalledPackages(any<PackageManager.PackageInfoFlags>())
        } answers { shared.values.toList() }
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } answers {
            val name = firstArg<String>()
            shared[name] ?: throw PackageManager.NameNotFoundException(name)
        }
        every { packageManager.getPackageArchiveInfo(any(), any<Int>()) } answers { archives[firstArg<String>()] }
        every { packageManager.getApplicationLabel(any()) } returns "Tachiyomi: Labelled"
        every { context.packageManager } returns packageManager
        every { context.filesDir } returns filesDir
        every { context.cacheDir } returns filesDir
        every { context.packageName } returns "eu.kanade.tachiyomi"
        every { context.sendBroadcast(any()) } answers { broadcasts += firstArg<Intent>() }
    }

    /** Installs [info] as a system-wide package. */
    fun installShared(info: PackageInfo) {
        shared[info.packageName] = info
    }

    /** Registers [info] (or nothing, for null) as the package inside the apk at [path]. */
    fun registerArchive(path: String, info: PackageInfo?) {
        if (info == null) {
            archives.remove(path)
        } else {
            archives[path] = info
        }
    }

    /** Removes the temporary private extension directory. */
    fun cleanUp() {
        filesDir.deleteRecursively()
    }
}

/** Whether the owner-write bit is set; unlike [File.canWrite] this does not answer true for root. */
internal fun File.isOwnerWritable(): Boolean =
    PosixFilePermission.OWNER_WRITE in Files.getPosixFilePermissions(toPath())
