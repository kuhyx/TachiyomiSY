package exh.uconfig

/** The e-hentai front-page categories, in the order the categories preference stores them. */
internal enum class EhCategory(val configKey: String) {
    DOUJINSHI("ct_doujinshi"),
    MANGA("ct_manga"),
    ARTIST_CG("ct_artistcg"),
    GAME_CG("ct_gamecg"),
    WESTERN("ct_western"),
    NON_H("ct_non-h"),
    IMAGE_SET("ct_imageset"),
    COSPLAY("ct_cosplay"),
    ASIAN_PORN("ct_asianporn"),
    MISC("ct_misc"),
}

/** The e-hentai gallery languages, in the order the language-filter preference stores them. */
internal enum class EhLanguage {
    JAPANESE,
    ENGLISH,
    CHINESE,
    DUTCH,
    FRENCH,
    GERMAN,
    HUNGARIAN,
    ITALIAN,
    KOREAN,
    POLISH,
    PORTUGUESE,
    RUSSIAN,
    SPANISH,
    THAI,
    VIETNAMESE,
    NOT_AVAILABLE,
    OTHER,
}
