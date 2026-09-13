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
import java.io.InputStream
import java.util.Locale
import kotlin.math.min

/** Splits very tall images into parts the reader can decode one at a time. */
internal object TallImageSplitting {
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
     * Splits tall images to improve performance of reader
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
        if (bitmapRegionDecoder == null) {
            logcat { "Failed to create new instance of BitmapRegionDecoder" }
            return false
        }

        val options = extractImageOptions(imageSource).apply {
            inJustDecodeBounds = false
        }
        val splitDataList = options.splitData

        return try {
            splitDataList.forEach { splitData ->
                val splitImageName = splitImageName(filenamePrefix, splitData.index)
                // Remove pre-existing split if exists (this split shouldn't exist under normal circumstances)
                tmpDir.findFile(splitImageName)?.delete()

                val splitFile = tmpDir.createFile(splitImageName)!!

                val region = Rect(0, splitData.topOffset, splitData.splitWidth, splitData.bottomOffset)

                splitFile.openOutputStream().use { outputStream ->
                    val splitBitmap = bitmapRegionDecoder.decodeRegion(region, options)
                    splitBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    splitBitmap.recycle()
                }
                logcat {
                    "Success: Split #${splitData.index + 1} with topOffset=${splitData.topOffset} " +
                        "height=${splitData.splitHeight} bottomOffset=${splitData.bottomOffset}"
                }
            }
            imageFile.delete()
            true
        } catch (e: Exception) {
            // Image splits were not successfully saved so delete them and keep the original image
            splitDataList
                .map { splitImageName(filenamePrefix, it.index) }
                .forEach { tmpDir.findFile(it)?.delete() }
            logcat(LogPriority.ERROR, e)
            false
        } finally {
            bitmapRegionDecoder.recycle()
        }
    }

    fun splitImageName(filenamePrefix: String, index: Int) = "${filenamePrefix}__${"%03d".format(
        Locale.ENGLISH,
        index + 1,
    )}.jpg"

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
                val range = 0..<partCount
                for (index in range) {
                    // Only continue if the list is empty or there is image remaining
                    if (isNotEmpty() && imageHeight <= last().bottomOffset) break

                    val topOffset = index * optimalSplitHeight
                    var splitHeight = min(optimalSplitHeight, imageHeight - topOffset)

                    if (index == range.last) {
                        val remainingHeight = imageHeight - (topOffset + splitHeight)
                        splitHeight += remainingHeight
                    }

                    add(SplitData(index, topOffset, splitHeight, imageWidth))
                }
            }
        }

    fun getBitmapRegionDecoder(imageStream: InputStream): BitmapRegionDecoder? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(imageStream)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(imageStream, false)
        }
    }

    val optimalImageHeight = getDisplayMaxHeightInPx * 2
}
