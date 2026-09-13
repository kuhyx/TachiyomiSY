package exh.log

import android.content.Context
import androidx.preference.PreferenceManager
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.sy.SYMR

/**
 * How much the E-Hentai code logs.
 *
 * @property nameRes label of the level.
 * @property description explanation of the level.
 */
public enum class EHLogLevel(public val nameRes: StringResource, public val description: StringResource) {
    /** Errors only. */
    MINIMAL(SYMR.strings.log_minimal, SYMR.strings.log_minimal_desc),

    /** Errors plus request summaries. */
    EXTRA(SYMR.strings.log_extra, SYMR.strings.log_extra_desc),

    /** Everything, including bodies. */
    EXTREME(SYMR.strings.log_extreme, SYMR.strings.log_extreme_desc),
    ;

    /** The active level, read once from preferences. */
    public companion object {
        private var curLogLevel: Int? = null

        /** The level chosen in settings. */
        public val currentLogLevel: EHLogLevel get() = values()[curLogLevel!!]

        /** Reads the level from the default preferences. */
        public fun init(context: Context) {
            curLogLevel = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("eh_log_level", MINIMAL.ordinal) // todo
        }

        /** True when [requiredLogLevel] is at or below the active level. */
        public fun shouldLog(requiredLogLevel: EHLogLevel): Boolean = curLogLevel!! >= requiredLogLevel.ordinal
    }
}
