package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 17f

// The old rotation-type preference indexed these orientations, 1-based (0 was unused).
private val LEGACY_ROTATION_TYPES = listOf(
    ReaderOrientation.FREE,
    ReaderOrientation.FREE,
    ReaderOrientation.PORTRAIT,
    ReaderOrientation.LANDSCAPE,
    ReaderOrientation.LOCKED_PORTRAIT,
    ReaderOrientation.LOCKED_LANDSCAPE,
)

internal class ResetReaderSettingsMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        withIOContext { migrate(context) }
        return true
    }

    private fun migrate(context: Application) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // Migrate Rotation and Viewer values to default values for viewer_flags
        val newOrientation = LEGACY_ROTATION_TYPES
            .getOrElse(prefs.getInt("pref_rotation_type_key", 1)) { ReaderOrientation.FREE }
            .flagValue

        // Reading mode flag and prefValue is the same value
        val newReadingMode = prefs.getInt("pref_default_viewer_key", 1)

        prefs.edit {
            putInt("pref_default_orientation_type_key", newOrientation)
            remove("pref_rotation_type_key")
            putInt("pref_default_reading_mode_key", newReadingMode)
            remove("pref_default_viewer_key")
        }
    }
}
