package eu.kanade.tachiyomi.ui.base.delegate

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.ui.category.biometric.TimeRange
import eu.kanade.tachiyomi.ui.security.UnlockActivity
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.isAuthenticationSupported
import eu.kanade.tachiyomi.util.view.setSecureScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.Calendar
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal interface SecureActivityDelegate {
    fun registerSecureActivity(activity: AppCompatActivity)

    companion object {
        // SY -->
        const val LOCK_SUNDAY = 0x40
        const val LOCK_MONDAY = 0x20
        const val LOCK_TUESDAY = 0x10
        const val LOCK_WEDNESDAY = 0x8
        const val LOCK_THURSDAY = 0x4
        const val LOCK_FRIDAY = 0x2
        const val LOCK_SATURDAY = 0x1
        const val LOCK_ALL_DAYS = 0x7F
        // SY <--

        /**
         * Set to true if we need the first activity to authenticate.
         *
         * Always require unlock if app is killed.
         */
        var requireUnlock = true

        fun onApplicationStopped() {
            val preferences = Injekt.get<SecurityPreferences>()
            if (!preferences.useAuthenticator.get()) return

            if (!AuthenticatorUtil.isAuthenticating) {
                // Return if app is closed in locked state
                if (requireUnlock) return
                // Save app close time if lock is delayed
                if (preferences.lockAppAfter.get() > 0) {
                    preferences.lastAppClosed.set(System.currentTimeMillis())
                }
            }
        }

        // SY -->
        private fun canLockNow(preferences: SecurityPreferences): Boolean {
            val today: Calendar = Calendar.getInstance()
            val timeRanges = preferences.authenticatorTimeRanges.get()
                .mapNotNull { TimeRange.fromPreferenceString(it) }
            val canLockNow = if (timeRanges.isNotEmpty()) {
                val now = today.get(Calendar.HOUR_OF_DAY).hours + today.get(Calendar.MINUTE).minutes
                timeRanges.any { now in it }
            } else {
                true
            }

            val lockedDays = preferences.authenticatorDays.get()
            // LOCK_SUNDAY (Calendar.SUNDAY = 1) is the highest bit, LOCK_SATURDAY (7) the lowest.
            val todayMask = 1 shl Calendar.SATURDAY - today.get(Calendar.DAY_OF_WEEK)
            val canLockToday = lockedDays == LOCK_ALL_DAYS || lockedDays and todayMask == todayMask

            return canLockNow && canLockToday
        }
        // SY <--

        /**
         * Checks if unlock is needed when app comes foreground.
         */
        fun onApplicationStart() {
            val preferences = Injekt.get<SecurityPreferences>()
            if (!preferences.useAuthenticator.get()) return

            val lastClosedPref = preferences.lastAppClosed

            // `requireUnlock` can be true on process start or if app was closed in locked state
            if (!AuthenticatorUtil.isAuthenticating && !requireUnlock) {
                requireUnlock =
                    /* SY --> */ canLockNow(preferences) &&
                    /* SY <-- */ when (val lockDelay = preferences.lockAppAfter.get()) {
                        -1 -> false // Never
                        0 -> true // Always
                        else -> lastClosedPref.get() + lockDelay * MILLIS_PER_MINUTE <= System.currentTimeMillis()
                    }
            }

            lastClosedPref.delete()
        }

        fun unlock() {
            requireUnlock = false
        }
    }
}

private const val MILLIS_PER_MINUTE = 60_000L

internal class SecureActivityDelegateImpl : SecureActivityDelegate {

    private val preferences: BasePreferences by injectLazy()
    private val securityPreferences: SecurityPreferences by injectLazy()

    // The observer captures the activity it watches, so the callbacks never see a missing one.
    override fun registerSecureActivity(activity: AppCompatActivity) {
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) = setSecureScreen(activity)

                override fun onResume(owner: LifecycleOwner) = setAppLock(activity)
            },
        )
    }

    // Not private: the observer object calls these, and private members would need synthetic accessors.
    internal fun setSecureScreen(activity: AppCompatActivity) {
        val secureScreenFlow = securityPreferences.secureScreen.changes()
        val incognitoModeFlow = preferences.incognitoMode.changes()
        combine(secureScreenFlow, incognitoModeFlow) { secureScreen, incognitoMode ->
            secureScreen == SecurityPreferences.SecureScreenMode.ALWAYS ||
                (secureScreen == SecurityPreferences.SecureScreenMode.INCOGNITO && incognitoMode)
        }
            .onEach(activity.window::setSecureScreen)
            .launchIn(activity.lifecycleScope)
    }

    internal fun setAppLock(activity: AppCompatActivity) {
        if (!securityPreferences.useAuthenticator.get()) return
        if (activity.isAuthenticationSupported()) {
            if (!SecureActivityDelegate.requireUnlock) return
            activity.startActivity(Intent(activity, UnlockActivity::class.java))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
        } else {
            securityPreferences.useAuthenticator.set(false)
        }
    }
}
