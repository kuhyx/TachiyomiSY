package exh.source

internal object BlacklistedSources {
    val EHENTAI_EXT_SOURCES = longArrayOf(
        8_100_626_124_886_895_451,
        57_122_881_048_805_941,
        4_678_440_076_103_929_247,
        1_876_021_963_378_735_852,
        3_955_189_842_350_477_641,
        4_348_288_691_341_764_259,
        773_611_868_725_221_145,
        5_759_417_018_342_755_550,
        825_187_715_438_990_384,
        6_116_711_405_602_166_104,
        7_151_438_547_982_231_541,
        2_171_445_159_732_592_630,
        3_032_959_619_549_451_093,
        5_980_349_886_941_016_589,
        6_073_266_008_352_078_708,
        5_499_077_866_612_745_456,
        6_140_480_779_421_365_791,
    )

    val BLACKLISTED_EXT_SOURCES = EHENTAI_EXT_SOURCES

    val BLACKLISTED_EXTENSIONS = arrayOf(
        "eu.kanade.tachiyomi.extension.all.ehentai",
    )

    var HIDDEN_SOURCES = setOf(
        MERGED_SOURCE_ID,
    )
}
