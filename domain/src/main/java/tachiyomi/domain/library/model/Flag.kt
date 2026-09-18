package tachiyomi.domain.library.model

/** A value whose bits are OR-ed into a flags word. */
public interface Flag {
    /** The bits this flag sets. */
    public val flag: Long
}

/** A value that owns a range of bits in a flags word. */
public interface Mask {
    /** The bits this value may occupy; every other bit is left alone. */
    public val mask: Long
}

/** A flag that replaces the bits under its [mask] instead of OR-ing into them. */
public interface FlagWithMask : Flag, Mask

/** Whether this flags word holds [other]: an exact match, or a match under the mask for a [Mask]. */
public operator fun Long.contains(other: Flag): Boolean {
    return if (other is Mask) {
        other.flag == this and other.mask
    } else {
        other.flag == this
    }
}

/** This flags word with [other] applied: OR-ed in, or replacing the bits under the mask for a [Mask]. */
public operator fun Long.plus(other: Flag): Long {
    return if (other is Mask) {
        this and other.mask.inv() or (other.flag and other.mask)
    } else {
        this or other.flag
    }
}

/** [flag] with [other] applied: OR-ed in, or replacing the bits under the mask for a [Mask]. */
public operator fun Flag.plus(other: Flag): Long {
    return if (other is Mask) {
        this.flag and other.mask.inv() or (other.flag and other.mask)
    } else {
        this.flag or other.flag
    }
}
