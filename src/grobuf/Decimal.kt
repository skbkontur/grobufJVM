package grobuf

import grobuf.serializers.byteArrayDataOffset
import grobuf.serializers.theUnsafe
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

class Decimal internal constructor(@JvmField val flags: Int,
                                   @JvmField val hi: Int,
                                   @JvmField val lo: Int,
                                   @JvmField val mid: Int)
    : Comparable<Decimal> {

    // The lo, mid, hi, and flags fields contain the representation of the
    // Decimal value. The lo, mid, and hi fields contain the 96-bit integer
    // part of the Decimal. Bits 0-15 (the lower word) of the flags field are
    // unused and must be zero; bits 16-23 contain a value between 0 and 28,
    // indicating the power of 10 to divide the 96-bit integer part
    // by to produce the Decimal value; bits 24-30 are unused and must be zero;
    // and finally bit 31 indicates the sign of the Decimal value, 0 meaning
    // positive and 1 meaning negative.

    val bigDecimal by lazy {
        val magnitude = ByteArray(12)
        theUnsafe.putInt(magnitude, byteArrayDataOffset + 0, lo)
        theUnsafe.putInt(magnitude, byteArrayDataOffset + 4, mid)
        theUnsafe.putInt(magnitude, byteArrayDataOffset + 8, hi)
        reverse(magnitude)
        val signum = if (flags and SIGN_MASK == 0) 1 else -1
        val scale = (flags and SCALE_MASK) ushr SCALE_SHIFT
        BigDecimal(BigInteger(signum, magnitude), scale, Context)
    }

    override fun compareTo(other: Decimal) = bigDecimal.compareTo(other.bigDecimal)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this.javaClass != other.javaClass)
            return false

        other as Decimal

        return compareTo(other) == 0
    }

    override fun hashCode() = bigDecimal.hashCode()

    override fun toString() = bigDecimal.toString()

    operator fun plus(other: Decimal) = fromBigDecimal(bigDecimal.add(other.bigDecimal))
    operator fun plus(other: Int) = plus(valueOf(other))
    operator fun plus(other: Long) = plus(valueOf(other))
    operator fun plus(other: Double) = plus(valueOf(other))
    operator fun plus(other: Float) = plus(valueOf(other))

    operator fun minus(other: Decimal) = fromBigDecimal(bigDecimal.subtract(other.bigDecimal))
    operator fun minus(other: Int) = minus(valueOf(other))
    operator fun minus(other: Long) = minus(valueOf(other))
    operator fun minus(other: Double) = minus(valueOf(other))
    operator fun minus(other: Float) = minus(valueOf(other))

    operator fun times(other: Decimal) = fromBigDecimal(bigDecimal.multiply(other.bigDecimal))
    operator fun times(other: Int) = times(valueOf(other))
    operator fun times(other: Long) = times(valueOf(other))
    operator fun times(other: Double) = times(valueOf(other))
    operator fun times(other: Float) = times(valueOf(other))

    operator fun div(other: Decimal) = fromBigDecimal(bigDecimal.divide(other.bigDecimal))
    operator fun div(other: Int) = div(valueOf(other))
    operator fun div(other: Long) = div(valueOf(other))
    operator fun div(other: Double) = div(valueOf(other))
    operator fun div(other: Float) = div(valueOf(other))

    companion object {
        @JvmStatic
        fun fromBigDecimal(value: BigDecimal): Decimal {
            if (value > MAX_VALUE.bigDecimal)
                return MAX_VALUE
            if (value < MIN_VALUE.bigDecimal)
                return MIN_VALUE
            if (value > NEAR_NEGATIVE_ZERO.bigDecimal
                    && value < NEAR_POSITIVE_ZERO.bigDecimal)
                return ZERO
            val roundedValue = if (value.precision() == Context.precision)
                                   value
                               else
                                   value.round(Context)
            var magnitude = roundedValue.unscaledValue()
            var scale = roundedValue.scale()
            val maxPowerOfTen = powersOfTen.size - 1
            while (scale < -maxPowerOfTen) {
                scale += maxPowerOfTen
                magnitude *= powersOfTen[maxPowerOfTen]
            }
            if (scale < 0) {
                magnitude *= powersOfTen[-scale]
                scale = 0
            }
            assert (scale <= 28, { "Scale $scale is too big" })
            val signum = magnitude.signum()
            if (signum < 0)
                magnitude = magnitude.negate()
            var arr = magnitude.toByteArray()
            assert (arr.size <= 12, { "Magnitude $magnitude is too big" })
            reverse(arr)
            if (arr.size < 12) {
                val tempArr = ByteArray(12)
                theUnsafe.copyMemory(arr, byteArrayDataOffset, tempArr, byteArrayDataOffset, arr.size.toLong())
                arr = tempArr
            }
            val flags = (if (signum >= 0) 0 else SIGN_MASK) or (scale shl SCALE_SHIFT)
            val lo    = theUnsafe.getInt(arr, byteArrayDataOffset + 0)
            val mid   = theUnsafe.getInt(arr, byteArrayDataOffset + 4)
            val hi    = theUnsafe.getInt(arr, byteArrayDataOffset + 8)
            return Decimal(flags, hi, lo, mid)
        }

        @JvmStatic
        fun valueOf(value: Int): Decimal {
            @Suppress("NAME_SHADOWING")
            var value = value
            val flags = if (value >= 0) 0
                        else {
                            value = -value
                            SIGN_MASK
                        }
            return Decimal(flags, 0, value, 0)
        }

        @JvmStatic
        fun valueOf(value: Long): Decimal {
            @Suppress("NAME_SHADOWING")
            var value = value
            val flags = if (value >= 0) 0
                        else {
                            value = -value
                            SIGN_MASK
                        }
            return Decimal(flags, 0, value.toInt(), (value ushr 32).toInt())
        }

        @JvmStatic
        fun valueOf(value: Double) = fromBigDecimal(BigDecimal.valueOf(value))

        @JvmStatic
        fun valueOf(value: Float) = fromBigDecimal(BigDecimal.valueOf(value.toDouble()))

        private fun reverse(arr: ByteArray) {
            for (i in 0 until arr.size / 2) {
                val t = arr[i]
                arr[i] = arr[arr.size - 1 - i]
                arr[arr.size - 1 - i] = t
            }
        }

        private val SIGN_MASK = 0x80000000.toInt()
        private val SCALE_MASK = 0x00FF0000
        private val SCALE_SHIFT = 16

        @JvmField
        val ZERO = Decimal(0, 0, 0, 0)

        @JvmField
        val ONE = Decimal(0, 0, 1, 0)

        @JvmField
        val MINUS_ONE = Decimal(SIGN_MASK, 0, 1, 0)

        @JvmField
        val MAX_VALUE = Decimal(0, -1, -1, -1)

        @JvmField
        val MIN_VALUE = Decimal(SIGN_MASK, -1, -1, -1)

        @JvmField
        val NEAR_NEGATIVE_ZERO = Decimal(SIGN_MASK or (28 shl SCALE_SHIFT), 0, 1, 0)

        @JvmField
        val NEAR_POSITIVE_ZERO = Decimal(28 shl SCALE_SHIFT, 0, 1, 0)

        private val Context = MathContext(29, RoundingMode.HALF_EVEN)

        private fun tenToThe(k: Int): Long {
            var x = 1L
            for (i in 1..k)
                x *= 10
            return x
        }

        private val powersOfTen = Array<BigInteger>(19, { BigInteger.valueOf(tenToThe(it)) } )
    }
}