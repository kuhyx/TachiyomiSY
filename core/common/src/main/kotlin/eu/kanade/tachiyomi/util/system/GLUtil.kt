package eu.kanade.tachiyomi.util.system

import mihon.core.common.NativeBinding
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import kotlin.math.max

/**
 * OpenGL texture limits, which bound hardware bitmap sizes.
 *
 * The object is a [NativeBinding]: both properties are EGL queries against the device's GPU.
 * The stepping of the settings choices is [textureLimitOptions], which is pure.
 */
@NativeBinding
public object GLUtil {
    /** The GPU's maximum texture size. */
    public val DEVICE_TEXTURE_LIMIT: Int by lazy {
        // Get EGL Display
        val egl = EGLContext.getEGL() as EGL10
        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

        // Initialise
        val version = IntArray(2)
        egl.eglInitialize(display, version)

        // Query total number of configurations
        val totalConfigurations = IntArray(1)
        egl.eglGetConfigs(display, null, 0, totalConfigurations)

        // Query actual list configurations
        val configurationsList = arrayOfNulls<EGLConfig>(totalConfigurations[0])
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations)

        val textureSize = IntArray(1)
        var maximumTextureSize = 0

        // Iterate through all the configurations to located the maximum texture size
        for (i in 0..<totalConfigurations[0]) {
            // Only need to check for width since opengl textures are always squared
            egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize)

            // Keep track of the maximum texture size
            if (maximumTextureSize < textureSize[0]) maximumTextureSize = textureSize[0]
        }

        // Release
        egl.eglTerminate(display)

        // Return largest texture size found (after making it a multiplier of [Multiplier]), or default
        max(maximumTextureSize, SAFE_TEXTURE_LIMIT)
    }

    /** A size every device handles. */
    public const val SAFE_TEXTURE_LIMIT: Int = 2048

    /** Choices offered in settings, from safe up to the device limit. */
    public val CUSTOM_TEXTURE_LIMIT_OPTIONS: List<Int> by lazy { textureLimitOptions(DEVICE_TEXTURE_LIMIT) }
}

/** [deviceLimit] first, then every multiple of 1024 below it down to [GLUtil.SAFE_TEXTURE_LIMIT]. */
internal fun textureLimitOptions(deviceLimit: Int): List<Int> {
    val steps = deviceLimit / MULTIPLIER
    return buildList(steps) {
        add(deviceLimit)
        for (step in steps downTo 2) {
            val value = step * MULTIPLIER
            if (value >= deviceLimit) continue
            add(value)
        }
    }
}

private const val MULTIPLIER: Int = 1024
