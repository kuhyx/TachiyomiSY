package eu.kanade.tachiyomi.ui.main

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import eu.kanade.tachiyomi.util.system.dpToPx

private const val SPLASH_EXIT_ANIM_DURATION = 400L // ms

// Sets custom splash screen exit animation on devices prior to Android 12.
// When custom animation is used, status and navigation bar color will be set to transparent and will be restored
// after the animation is finished.
@Suppress("Deprecation")
internal fun MainActivity.setSplashScreenExitAnimation(splashScreen: SplashScreen?) {
    val root = findViewById<View>(android.R.id.content)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && splashScreen != null) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        splashScreen.setOnExitAnimationListener { splashProvider ->
            // For some reason the SplashScreen applies (incorrect) Y translation to the iconView
            splashProvider.iconView.translationY = 0F

            val activityAnim = ValueAnimator.ofFloat(1F, 0F).apply {
                interpolator = LinearOutSlowInInterpolator()
                duration = SPLASH_EXIT_ANIM_DURATION
                addUpdateListener { va ->
                    val value = va.animatedValue as Float
                    root.translationY = value * 16.dpToPx
                }
            }

            val splashAnim = ValueAnimator.ofFloat(1F, 0F).apply {
                interpolator = FastOutSlowInInterpolator()
                duration = SPLASH_EXIT_ANIM_DURATION
                addUpdateListener { va ->
                    val value = va.animatedValue as Float
                    splashProvider.view.alpha = value
                }
                doOnEnd {
                    splashProvider.remove()
                }
            }

            activityAnim.start()
            splashAnim.start()
        }
    }
}
