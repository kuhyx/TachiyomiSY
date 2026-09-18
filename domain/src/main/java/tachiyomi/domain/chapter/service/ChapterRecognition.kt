package tachiyomi.domain.chapter.service

/**
 * Extracts a chapter number from a chapter name. In the examples below,
 * `-R>` reads "the regex turns this into".
 */
public object ChapterRecognition {

    private const val NUMBER_PATTERN = """([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?"""
    private const val UNKNOWN = -1.0
    private const val NOT_A_CHAPTER = -2.0
    private const val EXTRA = 0.99
    private const val OMAKE = 0.98
    private const val SPECIAL = 0.97
    private const val ALPHA_SCALE = 10.0
    private const val WHOLE_GROUP = 1
    private const val DECIMAL_GROUP = 2
    private const val ALPHA_GROUP = 3

    // All cases with Ch.xx: Mokushiroku Alice Vol.1 Ch. 4: Misrepresentation -R> 4
    private val basic = Regex("""(?<=ch\.) *$NUMBER_PATTERN""")

    // Bleach 567: Down With Snowwhite -R> 567
    private val number = Regex(NUMBER_PATTERN)

    // Removes unwanted tags: Prison School 12 v.1 vol004 version1243 volume64 -R> Prison School 12
    private val unwanted = Regex("""\b(?:v|ver|vol|version|volume|season|s)[^a-z]?[0-9]+""")

    // Removes unwanted whitespace: One Piece 12 special -R> One Piece 12special
    private val unwantedWhiteSpace = Regex("""\s(?=extra|special|omake)""")

    /**
     * The chapter number in [chapterName] once [mangaTitle] is stripped, or
     * [chapterNumber] when the source already knew it (-2 marks a non-chapter,
     * anything above -1 is a number); -1 when nothing in the name looks like one.
     */
    public fun parseChapterNumber(
        mangaTitle: String,
        chapterName: String,
        chapterNumber: Double? = null,
    ): Double {
        if (chapterNumber != null && (chapterNumber == NOT_A_CHAPTER || chapterNumber > UNKNOWN)) {
            return chapterNumber
        }
        val cleanChapterName = chapterName.lowercase()
            // Remove manga title from chapter title.
            .replace(mangaTitle.lowercase(), "")
            .trim()
            // Remove comma's or hyphens.
            .replace(',', '.')
            .replace('-', '.')
            // Remove unwanted white spaces.
            .replace(unwantedWhiteSpace, "")
        val numberMatch = number.findAll(cleanChapterName)
        return when {
            numberMatch.none() -> chapterNumber ?: UNKNOWN
            numberMatch.count() > 1 -> numberAmongSeveral(cleanChapterName) ?: numberOf(numberMatch.first())
            else -> numberOf(numberMatch.first())
        }
    }

    // With several numbers in the name, prefer a ch.xx one, then the first left after dropping tags.
    private fun numberAmongSeveral(cleanChapterName: String): Double? {
        val name = unwanted.replace(cleanChapterName, "")
        val match = basic.find(name) ?: number.find(name)
        return match?.let(::numberOf)
    }

    private fun numberOf(match: MatchResult): Double {
        val initial = match.groupValues[WHOLE_GROUP].toDouble()
        return initial + fraction(match.groupValues[DECIMAL_GROUP], match.groupValues[ALPHA_GROUP])
    }

    // The fractional part: an explicit decimal, a named extra, or a letter suffix (x.a -> x.1).
    private fun fraction(decimal: String, alpha: String): Double = when {
        decimal.isNotEmpty() -> decimal.toDouble()
        alpha.isEmpty() -> 0.0
        alpha.contains("extra") -> EXTRA
        alpha.contains("omake") -> OMAKE
        alpha.contains("special") -> SPECIAL
        else -> alpha.trimStart('.').singleOrNull()?.let(::alphaFraction) ?: 0.0
    }

    // x.a -> x.1, x.b -> x.2, ... x.i -> x.9; later letters carry nothing.
    private fun alphaFraction(alpha: Char): Double {
        val ordinal = alpha.code - ('a'.code - 1)
        return if (ordinal >= ALPHA_SCALE) 0.0 else ordinal / ALPHA_SCALE
    }
}
