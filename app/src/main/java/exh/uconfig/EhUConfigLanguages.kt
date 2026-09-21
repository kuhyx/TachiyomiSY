package exh.uconfig

// One entry per e-hentai language row: original / translated / rewrite checkbox keys.
internal abstract class BaseLanguage(val values: List<Boolean>) {
    abstract val translatedKey: String
    abstract val rewriteKey: String

    open val configs: List<LanguageConfigItem>
        get() = listOf(
            LanguageConfigItem(translatedKey, values[1]),
            LanguageConfigItem(rewriteKey, values[2]),
        )

    internal class LanguageConfigItem(override val key: String, value: Boolean) : ConfigItem {
        override val value = if (value) "checked" else ""
    }
}

internal abstract class Language(values: List<Boolean>) : BaseLanguage(values) {
    abstract val originalKey: String

    override val configs: List<LanguageConfigItem>
        get() = listOf(
            LanguageConfigItem(originalKey, values[0]),
            LanguageConfigItem(translatedKey, values[1]),
            LanguageConfigItem(rewriteKey, values[2]),
        )
}

internal class Japanese(values: List<Boolean>) : BaseLanguage(values) {
    override val translatedKey: String = "xl_1024"
    override val rewriteKey: String = "xl_2048"
}

internal class English(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_1"
    override val translatedKey: String = "xl_1025"
    override val rewriteKey: String = "xl_2049"
}

internal class Chinese(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_10"
    override val translatedKey: String = "xl_1034"
    override val rewriteKey: String = "xl_2058"
}

internal class Dutch(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_20"
    override val translatedKey: String = "xl_1044"
    override val rewriteKey: String = "xl_2068"
}

internal class French(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_30"
    override val translatedKey: String = "xl_1054"
    override val rewriteKey: String = "xl_2078"
}

internal class German(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_40"
    override val translatedKey: String = "xl_1064"
    override val rewriteKey: String = "xl_2088"
}

internal class Hungarian(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_50"
    override val translatedKey: String = "xl_1074"
    override val rewriteKey: String = "xl_2098"
}

internal class Italian(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_60"
    override val translatedKey: String = "xl_1084"
    override val rewriteKey: String = "xl_2108"
}

internal class Korean(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_70"
    override val translatedKey: String = "xl_1094"
    override val rewriteKey: String = "xl_2118"
}

internal class Polish(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_80"
    override val translatedKey: String = "xl_1104"
    override val rewriteKey: String = "xl_2128"
}

internal class Portuguese(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_90"
    override val translatedKey: String = "xl_1114"
    override val rewriteKey: String = "xl_2138"
}

internal class Russian(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_100"
    override val translatedKey: String = "xl_1124"
    override val rewriteKey: String = "xl_2148"
}

internal class Spanish(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_110"
    override val translatedKey: String = "xl_1134"
    override val rewriteKey: String = "xl_2158"
}

internal class Thai(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_120"
    override val translatedKey: String = "xl_1144"
    override val rewriteKey: String = "xl_2168"
}

internal class Vietnamese(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_130"
    override val translatedKey: String = "xl_1154"
    override val rewriteKey: String = "xl_2178"
}

internal class NotAvailable(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_254"
    override val translatedKey: String = "xl_1278"
    override val rewriteKey: String = "xl_2302"
}

internal class Other(values: List<Boolean>) : Language(values) {
    override val originalKey: String = "xl_255"
    override val translatedKey: String = "xl_1279"
    override val rewriteKey: String = "xl_2303"
}
