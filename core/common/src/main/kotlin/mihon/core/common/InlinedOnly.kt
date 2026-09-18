package mihon.core.common

/**
 * Marks a reified inline function whose logic lives in a non-reified overload.
 *
 * The compiled copy of such a function opens with the reified-type marker, which throws, so
 * no coverage probe in it can ever fire; the body is measured wherever it is inlined into
 * main code. The module's Kover filters exclude declarations carrying this annotation. Keep
 * the annotated body a one-line delegation so nothing testable hides behind it.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
internal annotation class InlinedOnly
