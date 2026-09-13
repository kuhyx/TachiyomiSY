package tachiyomi.core.common.util.system

import android.graphics.Bitmap
import android.os.Build
import eu.kanade.tachiyomi.util.system.GLUtil
import okio.BufferedSource

/** Decides when a hardware bitmap is safe on this device. */
internal object HardwareBitmaps {
    var hardwareBitmapThreshold: Int = GLUtil.SAFE_TEXTURE_LIMIT

    /**
     * Taken from Coil
     * (coil-core/src/androidMain/kotlin/coil3/util/hardwareBitmaps.kt at 1674d3516f061aeacbe749a435b1924f9648fd41)
     * ---
     * Maintains a list of devices with broken/incomplete/unstable hardware bitmap implementations.
     *
     * Model names are retrieved from
     * [Google's official device list](https://support.google.com/googleplay/answer/1727131?hl=en).
     *
     */
    val HARDWARE_BITMAP_UNSUPPORTED = when (Build.VERSION.SDK_INT) {
        Build.VERSION_CODES.O -> run {
            val model = Build.MODEL ?: return@run false

            // Samsung Galaxy (ALL)
            if (model.removePrefix("SAMSUNG-").startsWith("SM-")) return@run true

            val device = Build.DEVICE ?: return@run false

            return@run device in arrayOf(
                "nora", "nora_8917", "nora_8917_n", // Moto E5
                "james", "rjames_f", "rjames_go", "pettyl", // Moto E5 Play
                "hannah", "ahannah", "rhannah", // Moto E5 Plus

                "ali", "ali_n", // Moto G6
                "aljeter", "aljeter_n", "jeter", // Moto G6 Play
                "evert", "evert_n", "evert_nt", // Moto G6 Plus

                "G3112", "G3116", "G3121", "G3123", "G3125", // Xperia XA1
                "G3412", "G3416", "G3421", "G3423", "G3426", // Xperia XA1 Plus
                "G3212", "G3221", "G3223", "G3226", // Xperia XA1 Ultra

                "BV6800Pro", // BlackView BV6800Pro
                "CatS41", // Cat S41
                "Hi9Pro", // CHUWI Hi9 Pro
                "manning", // Lenovo K8 Note
                "N5702L", // NUU Mobile G3
            )
        }

        Build.VERSION_CODES.O_MR1 -> run {
            val device = Build.DEVICE ?: return@run false

            return@run device in arrayOf(
                "mcv1s", // LG Tribute Empire
                "mcv3", // LG K11
                "mcv5a", // LG Q7
                "mcv7a", // LG Stylo 4

                "A30ATMO", // T-Mobile REVVL 2
                "A70AXLTMO", // T-Mobile REVVL 2 PLUS

                "A3A_8_4G_TMO", // Alcatel 9027W
                "Edison_CKT", // Alcatel ONYX
                "EDISON_TF", // Alcatel TCL XL2
                "FERMI_TF", // Alcatel A501DL
                "U50A_ATT", // Alcatel TETRA
                "U50A_PLUS_ATT", // Alcatel 5059R
                "U50A_PLUS_TF", // Alcatel TCL LX
                "U50APLUSTMO", // Alcatel 5059Z
                "U5A_PLUS_4G", // Alcatel 1X

                "RCT6513W87DK5e", // RCA Galileo Pro
                "RCT6873W42BMF9A", // RCA Voyager
                "RCT6A03W13", // RCA 10 Viking
                "RCT6B03W12", // RCA Atlas 10 Pro
                "RCT6B03W13", // RCA Atlas 10 Pro+
                "RCT6T06E13", // RCA Artemis 10

                "A3_Pro", // Umidigi A3 Pro
                "One", // Umidigi One
                "One_Max", // Umidigi One Max
                "One_Pro", // Umidigi One Pro
                "Z2", // Umidigi Z2
                "Z2_PRO", // Umidigi Z2 Pro

                "Armor_3", // Ulefone Armor 3
                "Armor_6", // Ulefone Armor 6

                "Blackview", // Blackview BV6000
                "BV9500", // Blackview BV9500
                "BV9500Pro", // Blackview BV9500Pro

                "A6L-C", // Nuu A6L-C
                "N5002LA", // Nuu A7L
                "N5501LA", // Nuu A5L

                "Power_2_Pro", // Leagoo Power 2 Pro
                "Power_5", // Leagoo Power 5
                "Z9", // Leagoo Z9

                "V0310WW", // Blu VIVO VI+
                "V0330WW", // Blu VIVO XI

                "A3", // BenQ A3
                "ASUS_X018_4", // Asus ZenFone Max Plus M1 (ZB570TL)
                "C210AE", // Wiko Life
                "fireball", // DROID Incredible 4G LTE
                "ILA_X1", // iLA X1
                "Infinix-X605_sprout", // Infinix NOTE 5 Stylus
                "j7maxlte", // Samsung Galaxy J7 Max
                "KING_KONG_3", // Cubot King Kong 3
                "M10500", // Packard Bell M10500
                "S70", // Altice ALTICE S70
                "S80Lite", // Doogee S80Lite
                "SGINO6", // SGiNO 6
                "st18c10bnn", // Barnes and Noble BNTV650
                "TECNO-CA8", // Tecno CAMON X Pro,
                "SHIFT6m", // SHIFT 6m
            )
        }

        else -> false
    }

    fun canUseHardwareBitmap(bitmap: Bitmap): Boolean {
        return canUseHardwareBitmap(bitmap.width, bitmap.height)
    }

    fun canUseHardwareBitmap(imageSource: BufferedSource): Boolean {
        return with(extractImageOptions(imageSource)) {
            canUseHardwareBitmap(outWidth, outHeight)
        }
    }

    fun canUseHardwareBitmap(width: Int, height: Int): Boolean {
        if (HARDWARE_BITMAP_UNSUPPORTED) return false
        return maxOf(width, height) <= hardwareBitmapThreshold
    }
}
