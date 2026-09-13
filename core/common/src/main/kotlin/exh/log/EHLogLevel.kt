package exh.log

import android.content.Context
import androidx.preference.PreferenceManager
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.sy.SYMR

public enum class EHLogLevel(public val nameRes: StringResource, public val description: StringResource) {
    MINIMAL(SYMR.strings.log_minimal, SYMR.strings.log_minimal_desc),
    EXTRA(SYMR.strings.log_extra, SYMR.strings.log_extra_desc),
    EXTREME(SYMR.strings.log_extreme, SYMR.strings.log_extreme_desc),
    ;

    public companion object {
        private var curLogLevel: Int? = null

        public val currentLogLevel: EHLogLevel get() = values()[curLogLevel!!]

        public fun init(context: Context) {
            curLogLevel = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("eh_log_level", MINIMAL.ordinal) // todo
        }

        public fun shouldLog(requiredLogLevel: EHLogLevel): Boolean {
            return curLogLevel!! >= requiredLogLevel.ordinal
        }
    }
}
