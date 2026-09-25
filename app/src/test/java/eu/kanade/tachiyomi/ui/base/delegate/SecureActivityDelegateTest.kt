package eu.kanade.tachiyomi.ui.base.delegate

import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Calendar

internal class SecureActivityDelegateTest {
    private val prefs = SecurityPreferences(MapPreferenceStore())

    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { prefs } }) }
        prefs.useAuthenticator.set(true)
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        SecureActivityDelegate.requireUnlock = true
        AuthenticatorUtil.isAuthenticating = false
    }

    private fun start(): Boolean {
        SecureActivityDelegate.unlock()
        SecureActivityDelegate.onApplicationStart()
        return SecureActivityDelegate.requireUnlock
    }

    @Test
    fun stoppedWithoutLockIsANoOp() {
        prefs.useAuthenticator.set(false)
        SecureActivityDelegate.unlock()
        prefs.lockAppAfter.set(5)
        SecureActivityDelegate.onApplicationStopped()
        prefs.lastAppClosed.isSet().shouldBeFalse()
    }

    @Test
    fun stoppedRecordsCloseTime() {
        SecureActivityDelegate.onApplicationStopped()
        prefs.lastAppClosed.isSet().shouldBeFalse()
        SecureActivityDelegate.unlock()
        AuthenticatorUtil.isAuthenticating = true
        SecureActivityDelegate.onApplicationStopped()
        prefs.lastAppClosed.isSet().shouldBeFalse()
        AuthenticatorUtil.isAuthenticating = false
        SecureActivityDelegate.onApplicationStopped()
        prefs.lastAppClosed.isSet().shouldBeFalse()
        prefs.lockAppAfter.set(5)
        SecureActivityDelegate.onApplicationStopped()
        prefs.lastAppClosed.isSet().shouldBeTrue()
    }

    @Test
    fun startWithoutLockKeepsState() {
        prefs.useAuthenticator.set(false)
        start().shouldBeFalse()
    }

    @Test
    fun startHonoursTheLockDelay() {
        prefs.lockAppAfter.set(-1)
        start().shouldBeFalse()
        prefs.lockAppAfter.set(0)
        start().shouldBeTrue()
        prefs.lockAppAfter.set(10)
        prefs.lastAppClosed.set(System.currentTimeMillis())
        start().shouldBeFalse()
        prefs.lastAppClosed.isSet().shouldBeFalse()
        prefs.lastAppClosed.set(0)
        start().shouldBeTrue()
    }

    @Test
    fun startSkipsWhileAuthenticating() {
        AuthenticatorUtil.isAuthenticating = true
        start().shouldBeFalse()
        AuthenticatorUtil.isAuthenticating = false
        SecureActivityDelegate.requireUnlock = true
        SecureActivityDelegate.onApplicationStart()
        SecureActivityDelegate.requireUnlock.shouldBeTrue()
    }

    @Test
    fun timeRangesGateTheLock() {
        prefs.authenticatorTimeRanges.set(setOf("0,1440", "garbage"))
        start().shouldBeTrue()
        // An inverted range contains nothing, so no time of day can lock.
        prefs.authenticatorTimeRanges.set(setOf("600,300"))
        start().shouldBeFalse()
    }

    @Test
    fun weekdaysGateTheLock() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val todayMask = 1 shl Calendar.SATURDAY - today
        prefs.authenticatorDays.set(todayMask)
        start().shouldBeTrue()
        prefs.authenticatorDays.set(SecureActivityDelegate.LOCK_ALL_DAYS xor todayMask)
        start().shouldBeFalse()
    }

    @Test
    fun weekdayMasksMatchCalendar() {
        val masks = listOf(
            SecureActivityDelegate.LOCK_SUNDAY,
            SecureActivityDelegate.LOCK_MONDAY,
            SecureActivityDelegate.LOCK_TUESDAY,
            SecureActivityDelegate.LOCK_WEDNESDAY,
            SecureActivityDelegate.LOCK_THURSDAY,
            SecureActivityDelegate.LOCK_FRIDAY,
            SecureActivityDelegate.LOCK_SATURDAY,
        )
        masks.reduce(Int::or) shouldBe SecureActivityDelegate.LOCK_ALL_DAYS
        masks.first() shouldBe (1 shl Calendar.SATURDAY - Calendar.SUNDAY)
    }
}
