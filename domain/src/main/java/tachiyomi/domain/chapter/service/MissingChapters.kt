package tachiyomi.domain.chapter.service

import tachiyomi.domain.chapter.model.Chapter
import kotlin.math.floor

/**
 * How many whole chapter numbers between 1 and the highest one are absent.
 * Unknown numbers (-1) are ignored and fractions collapse onto their integer,
 * so 16.5 never counts as a gap.
 */
public fun List<Double>.missingChaptersCount(): Int {
    val chapters = this
        // Ignore unknown chapter numbers
        .filterNot { it == -1.0 }
        // Convert to integers, as we cannot check if 16.5 is missing
        .map(Double::toInt)
        // Only keep unique chapters so that -1 or 16 are not counted multiple times
        .distinct()
        .sorted()

    var missingChaptersCount = 0
    var previousChapter = 0 // The actual chapter number, not the array index
    // We go from 0 to lastChapter - Make sure to use the current index instead of the value
    for (currentChapter in chapters) {
        if (currentChapter > previousChapter + 1) {
            // Add the amount of missing chapters
            missingChaptersCount += currentChapter - previousChapter - 1
        }
        previousChapter = currentChapter
    }
    return missingChaptersCount
}

/** Whole chapters between two chapters; 0 when either is null or has no recognised number. */
public fun calculateChapterGap(higherChapter: Chapter?, lowerChapter: Chapter?): Int {
    return if (higherChapter == null || lowerChapter == null) {
        0
    } else if (!higherChapter.isRecognizedNumber || !lowerChapter.isRecognizedNumber) {
        0
    } else {
        calculateChapterGap(higherChapter.chapterNumber, lowerChapter.chapterNumber)
    }
}

/** Whole chapters strictly between two chapter numbers; 0 when either is negative. */
public fun calculateChapterGap(higherChapterNumber: Double, lowerChapterNumber: Double): Int {
    return if (higherChapterNumber < 0.0 || lowerChapterNumber < 0.0) {
        0
    } else {
        floor(higherChapterNumber).toInt() - floor(lowerChapterNumber).toInt() - 1
    }
}
