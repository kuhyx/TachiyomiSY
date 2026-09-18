package tachiyomi.domain.source.model

/**
 * One pin state bit of a source; combined into a [Pins] value.
 *
 * @property code The bit this state contributes to a [Pins] code.
 */
public sealed class Pin(public val code: Int) {
    /** Not pinned. */
    public data object Unpinned : Pin(UNPINNED)

    /** Pinned to the top of the source list. */
    public data object Pinned : Pin(PINNED)

    /** Shown in the pinned section (cleared for the "last used" and category copies). */
    public data object Actual : Pin(ACTUAL)

    private companion object {
        const val UNPINNED = 0b00
        const val PINNED = 0b01
        const val ACTUAL = 0b10
    }
}

/**
 * A set of [Pin] bits.
 *
 * @property code The or-ed [Pin.code]s of the set; no bits by default.
 */
public data class Pins(val code: Int = Pin.Unpinned.code) {

    /** The named [Pins] values and the factory that builds one from bits. */
    public companion object {
        /** No pin bits. */
        public val unpinned: Pins = from(Pin.Unpinned)

        /** Pinned and shown in the pinned section. */
        public val pinned: Pins = from(Pin.Pinned, Pin.Actual)

        /** The union of [pins]. */
        public fun from(vararg pins: Pin): Pins = Pins(pins.fold(0) { code, pin -> code or pin.code })
    }
}

/** Whether every bit of [pin] is set. */
public operator fun Pins.contains(pin: Pin): Boolean = pin.code and code == pin.code

/** This set with [pin]'s bits set. */
public operator fun Pins.plus(pin: Pin): Pins = Pins(code or pin.code)

/** This set with [pin]'s bits toggled (cleared when set, as the callers use it). */
public operator fun Pins.minus(pin: Pin): Pins = Pins(code xor pin.code)
