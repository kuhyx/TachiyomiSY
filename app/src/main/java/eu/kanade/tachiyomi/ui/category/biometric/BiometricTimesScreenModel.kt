package eu.kanade.tachiyomi.ui.category.biometric

import android.app.Application
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.preference.plusAssign
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class BiometricTimesScreenModel(
    private val preferences: SecurityPreferences = Injekt.get(),
) : StateScreenModel<BiometricTimesScreenState>(BiometricTimesScreenState.Loading) {

    private val _events: Channel<BiometricTimesEvent> = Channel(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    init {
        screenModelScope.launchIO {
            // todo usecase
            preferences.authenticatorTimeRanges.changes()
                .collectLatest { times ->
                    val context = Injekt.get<Application>()
                    mutableState.update {
                        BiometricTimesScreenState.Success(
                            timeRanges = times.toList()
                                .mapNotNull(TimeRange::fromPreferenceString)
                                .map { TimeRangeItem(it, it.getFormattedString(context)) },
                        )
                    }
                }
        }
    }

    /**
     * Adds a new lock time range to the preferences.
     *
     * @param timeRange The range to add.
     */
    fun createTimeRange(timeRange: TimeRange) {
        // todo usecase
        screenModelScope.launchIO {
            // Do not allow duplicate categories.
            if (timeRangeConflicts(timeRange)) {
                _events.send(BiometricTimesEvent.TimeConflicts)
            } else {
                preferences.authenticatorTimeRanges += timeRange.toPreferenceString()
            }
        }
    }

    /**
     * Deletes the given time range from the preferences.
     *
     * @param timeRange The range to delete.
     */
    fun deleteTimeRanges(timeRange: TimeRangeItem) {
        // todo usecase
        screenModelScope.launchIO {
            val state = state.value as? BiometricTimesScreenState.Success
            if (state != null) {
                preferences.authenticatorTimeRanges.set(
                    state.timeRanges.filterNot { it == timeRange }.map { it.timeRange.toPreferenceString() }.toSet(),
                )
            }
        }
    }

    // Returns true if a category with the given name already exists.
    private fun timeRangeConflicts(timeRange: TimeRange): Boolean {
        val state = state.value as? BiometricTimesScreenState.Success ?: return false
        return state.timeRanges.any { timeRange.conflictsWith(it.timeRange) }
    }

    fun showDialog(dialog: BiometricTimesDialog) {
        mutableState.update {
            when (it) {
                BiometricTimesScreenState.Loading -> it
                is BiometricTimesScreenState.Success -> it.copy(dialog = dialog)
            }
        }
    }

    fun dismissDialog() {
        mutableState.update {
            when (it) {
                BiometricTimesScreenState.Loading -> it
                is BiometricTimesScreenState.Success -> it.copy(dialog = null)
            }
        }
    }
}

internal sealed class BiometricTimesEvent {
    // On the root so a collector toasts every event without an always-true `is` check.
    abstract val stringRes: StringResource

    sealed class LocalizedMessage(override val stringRes: StringResource) : BiometricTimesEvent()
    data object TimeConflicts : LocalizedMessage(SYMR.strings.biometric_lock_time_conflicts)
    data object InternalError : LocalizedMessage(MR.strings.internal_error)
}

internal sealed class BiometricTimesDialog {
    data object Create : BiometricTimesDialog()
    data class Delete(val timeRange: TimeRangeItem) : BiometricTimesDialog()
}

internal sealed class BiometricTimesScreenState {

    @Immutable
    data object Loading : BiometricTimesScreenState()

    @Immutable
    data class Success(
        val timeRanges: List<TimeRangeItem>,
        val dialog: BiometricTimesDialog? = null,
    ) : BiometricTimesScreenState() {

        val isEmpty: Boolean
            get() = timeRanges.isEmpty()
    }
}
