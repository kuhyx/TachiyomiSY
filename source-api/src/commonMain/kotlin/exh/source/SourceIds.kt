package exh.source

// Lewd source IDs
/** Base of the ids given to the built-in adult sources. */
public const val LEWD_SOURCE_SERIES: Long = 6900L

/** E-Hentai. */
public const val EH_SOURCE_ID: Long = LEWD_SOURCE_SERIES + 1

/** ExHentai. */
public const val EXH_SOURCE_ID: Long = LEWD_SOURCE_SERIES + 2

/** Pururin. */
public const val PURURIN_SOURCE_ID: Long = 2_221_515_250_486_218_861

/** Tsumino. */
public const val TSUMINO_SOURCE_ID: Long = 6_707_338_697_138_388_238

/** 8muses. */
public const val EIGHTMUSES_SOURCE_ID: Long = 1_802_675_169_972_965_535

/** HBrowse. */
public const val HBROWSE_SOURCE_ID: Long = 1_401_584_337_232_758_222

/** The virtual source of merged manga. */
public const val MERGED_SOURCE_ID: Long = LEWD_SOURCE_SERIES + 69

// The ids the built-in nhentai, Tsumino and HBrowse sources had before they became delegated
// sources; migrations and backup restores rewrite them.

/** nhentai before delegation. */
public const val LEGACY_NHENTAI_SOURCE_ID: Long = LEWD_SOURCE_SERIES + 7

/** Tsumino before delegation. */
public const val LEGACY_TSUMINO_SOURCE_ID: Long = LEWD_SOURCE_SERIES + 9

/** HBrowse before delegation. */
public const val LEGACY_HBROWSE_SOURCE_ID: Long = LEWD_SOURCE_SERIES + 12
