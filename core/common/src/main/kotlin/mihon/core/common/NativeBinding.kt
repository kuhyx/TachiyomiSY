package mihon.core.common

/**
 * Marks code that binds to a native or firmware-only API and so can never complete on the JVM:
 * the libarchive wrapper (`Archive` loads `libarchive-jni`, built for Android ABIs only) with the
 * public entry points that select it, and the EGL texture query (`javax.microedition` is absent
 * from the unit-test android.jar and from the Robolectric sandbox).
 *
 * Public so the app module can mark its own native loads (the sqlcipher library) the same way.
 * The module's Kover filters exclude declarations carrying this annotation. Everything else is
 * kept behind a seam (`ArchiveNative`, `textureLimitOptions`) and covered against a fake.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
public annotation class NativeBinding
