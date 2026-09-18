package tachiyomi.core.common.util.system

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import com.hippo.unifile.UniFile
import logcat.LogPriority
import okio.Buffer
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil.SplitData
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import kotlin.math.min

private const val JPEG_QUALITY = 100

/** Splits very tall images into parts the reader can decode one at a time. */
internal object TallImageSplitting {
    val BitmapFactory.Options.splitData
        get(): List<SplitData> {
            val imageHeight = outHeight
            val imageWidth = outWidth

            val partCount = TallImageSplitCalculator.calculatePartCount(imageHeight, optimalImageHeight)
            val optimalSplitHeight = imageHeight / partCount

            logcat {
                "Generating SplitData for image (height: $imageHeight): " +
                    "$partCount parts @ ${optimalSplitHeight}px height per part"
            }

            return buildList {
                // partCount parts of imageHeight / partCount never overrun the image, so every
                // index has image remaining.
                repeat(partCount) { index ->
                    val topOffset = index * optimalSplitHeight
                    var splitHeight = min(optimalSplitHeight, imageHeight - topOffset)

                    if (index == partCount - 1) {
                        val remainingHeight = imageHeight - (topOffset + splitHeight)
                        splitHeight += remainingHeight
                    }

                    add(SplitData(index, topOffset, splitHeight, imageWidth))
                }
            }
        }

    val optimalImageHeight = getDisplayMaxHeightInPx * 2

    /**
     * Check whether the image is considered a tall image.
     *
     * @return true if the height:width ratio is greater than 3.
     */
    fun isTallImage(imageSource: BufferedSource): Boolean {
        val options = extractImageOptions(imageSource)
        return TallImageSplitCalculator.shouldSplit(
            imageWidth = options.outWidth,
            imageHeight = options.outHeight,
            optimalImageHeight = optimalImageHeight,
        )
    }

    /**
     * Splits tall images to improve performance of reader.
     */
    fun splitTallImage(
        tmpDir: UniFile,
        imageFile: UniFile,
        filenamePrefix: String,
    ): Boolean {
        val imageSource = imageFile.openInputStream().use { Buffer().readFrom(it) }
        if (ImageTypeDetection.isAnimatedAndSupported(imageSource) || !isTallImage(imageSource)) {
            return true
        }

        val bitmapRegionDecoder = getBitmapRegionDecoder(imageSource.peek().inputStream())
        return if (bitmapRegionDecoder == null) {
            logcat { "Failed to create new instance of BitmapRegionDecoder" }
            false
        } else {
            val options = extractImageOptions(imageSource).apply {
                inJustDecodeBounds = false
            }
            writeSplits(tmpDir, imageFile, filenamePrefix, bitmapRegionDecoder, options)
        }
    }

    // Writes every split next to the original and deletes the original; on any failure the
    // partial splits are removed and the original is kept.
    private fun writeSplits(
        tmpDir: UniFile,
        imageFile: UniFile,
        filenamePrefix: String,
        decoder: BitmapRegionDecoder,
        options: BitmapFactory.Options,
    ): Boolean {
        val splitDataList = options.splitData
        return try {
            splitDataList.forEach { splitData -> writeSplit(tmpDir, filenamePrefix, decoder, options, splitData) }
            imageFile.delete()
            true
        } catch (e: IOException) {
            discardSplits(tmpDir, filenamePrefix, splitDataList, e)
        } catch (e: IllegalArgumentException) {
            discardSplits(tmpDir, filenamePrefix, splitDataList, e)
        } catch (e: IllegalStateException) {
            discardSplits(tmpDir, filenamePrefix, splitDataList, e)
        } finally {
            decoder.recycle()
        }
    }

    private fun writeSplit(
        tmpDir: UniFile,
        filenamePrefix: String,
        decoder: BitmapRegionDecoder,
        options: BitmapFactory.Options,
        splitData: ImageUtil.SplitData,
    ) {
        val splitImageName = splitImageName(filenamePrefix, splitData.index)
        // Remove pre-existing split if exists (this split shouldn't exist under normal circumstances)
        tmpDir.findFile(splitImageName)?.delete()

        val splitFile = tmpDir.createFile(splitImageName) ?: throw IOException("Cannot create $splitImageName")

        val region = Rect(0, splitData.topOffset, splitData.splitWidth, splitData.bottomOffset)

        splitFile.openOutputStream().use { outputStream ->
            val splitBitmap = decoder.decodeRegion(region, options) ?: throw IOException("Cannot decode $region")
            splitBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            splitBitmap.recycle()
        }
        logcat {
            "Success: Split #${splitData.index + 1} with topOffset=${splitData.topOffset} " +
                "height=${splitData.splitHeight} bottomOffset=${splitData.bottomOffset}"
        }
    }

    private fun discardSplits(
        tmpDir: UniFile,
        filenamePrefix: String,
        splitDataList: List<ImageUtil.SplitData>,
        e: Exception,
    ): Boolean {
        // Image splits were not successfully saved so delete them and keep the original image
        splitDataList
            .map { splitImageName(filenamePrefix, it.index) }
            .forEach { tmpDir.findFile(it)?.delete() }
        logcat(LogPriority.ERROR, e)
        return false
    }

    fun splitImageName(filenamePrefix: String, index: Int) = "${filenamePrefix}__${"%03d".format(
        Locale.ENGLISH,
        index + 1,
    )}.jpg"

    fun getBitmapRegionDecoder(imageStream: InputStream): BitmapRegionDecoder? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(imageStream)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(imageStream, false)
        }
    }
}
