package eu.kanade.tachiyomi.util.storage

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowMediaScannerConnection
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
internal class DiskUtilNoMediaTest {
    private lateinit var root: File
    private lateinit var context: Context

    @Before
    fun setUp() {
        root = Files.createTempDirectory("nomedia").toFile()
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun skipsNullDir() {
        DiskUtil.createNoMediaFile(null, context)
        ShadowMediaScannerConnection.getSavedPaths() shouldBe emptySet()
    }

    @Test
    fun skipsMissingDir() {
        val missing = File(root, "missing")
        DiskUtil.createNoMediaFile(UniFile.fromFile(missing), context)
        missing.exists() shouldBe false
        ShadowMediaScannerConnection.getSavedPaths() shouldBe emptySet()
    }

    @Test
    fun leavesExistingMarkerAlone() {
        File(root, DiskUtil.NOMEDIA_FILE).writeText("keep")
        DiskUtil.createNoMediaFile(UniFile.fromFile(root), context)
        File(root, DiskUtil.NOMEDIA_FILE).readText() shouldBe "keep"
        ShadowMediaScannerConnection.getSavedPaths() shouldNotContain root.path
    }

    @Test
    fun createsMarkerAndScans() {
        DiskUtil.createNoMediaFile(UniFile.fromFile(root), context)
        File(root, DiskUtil.NOMEDIA_FILE).exists() shouldBe true
        ShadowMediaScannerConnection.getSavedPaths() shouldContain root.path
    }

    @Test
    fun createsMarkerWithoutContext() {
        DiskUtil.createNoMediaFile(UniFile.fromFile(root), null)
        File(root, DiskUtil.NOMEDIA_FILE).exists() shouldBe true
        ShadowMediaScannerConnection.getSavedPaths() shouldBe emptySet()
    }

    @Test
    fun scanMediaRecordsPath() {
        val file = File(root, "page.jpg").apply { writeBytes(ByteArray(1)) }
        DiskUtil.scanMedia(context, Uri.fromFile(file))
        ShadowMediaScannerConnection.getSavedPaths() shouldContain file.path
    }
}
