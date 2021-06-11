class Op(val value: Int, private val mask: Int = 0xffff) : Comparable<Op> {
    val kk: UByte
        get() = value.toUByte()

    val nnn: UShort
        get() = (value and 0xfff).toUShort()

    val x: Int
        get() = value shr 8 and 0xf

    val y: Int
        get() = value shr 4 and 0xf

    val n: Int
        get() = value and 0xf

    override fun compareTo(other: Op): Int =
        (this.mask and other.mask).let { m -> this.value and m - other.value and m }

    override fun equals(other: Any?): Boolean = other is Op && this.compareTo(other) == 0

    override fun hashCode(): Int = value.hashCode() xor mask.hashCode()
}
