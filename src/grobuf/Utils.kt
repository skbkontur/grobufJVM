package grobuf

internal class GroBufRandom(seed: Int) {
    private var m_inext = 0
    private var m_inextp = 0
    private val m_seedArray = IntArray(56)

    private fun Int.abs() = if (this >= 0) this else -this

    init {
        @Suppress("INTEGER_OVERFLOW")
        val num4 = if (seed == -0x80000000.toInt()) 0x7fffffff else seed.abs()
        var num2 = 0x9a4ec86 - num4
        m_seedArray[55] = num2
        var num3 = 1
        for (i in 1..54) {
            val index = (21 * i) % 55
            m_seedArray[index] = num3
            num3 = num2 - num3
            if (num3 < 0)
                num3 += 0x7fffffff
            num2 = m_seedArray[index]
        }
        for (j in 1..4) {
            for (k in 1..55) {
                m_seedArray[k] -= m_seedArray[1 + (k + 30) % 55]
                if (m_seedArray[k] < 0)
                    m_seedArray[k] += 0x7fffffff
            }
        }
        m_inext = 0
        m_inextp = 21
    }

    fun next(): Int {
        var inext = m_inext
        var inextp = m_inextp
        if (++inext >= 56)
            inext = 1
        if (++inextp >= 56)
            inextp = 1
        var num = m_seedArray[inext] - m_seedArray[inextp]
        if (num == 0x7fffffff)
            num--
        if (num < 0)
            num += 0x7fffffff
        m_seedArray[inext] = num
        m_inext = inext
        m_inextp = inextp
        return num
    }
}

class HashCalculator(seed: Int, val maxLength: Int) {
    private val randTable: Array<LongArray>

    init {
        val random = GroBufRandom(seed)
        fun next24BitRandom() = (random.next() and 0xFFFFFF).toLong()
        randTable = Array(maxLength * 2) {
            LongArray(256) {
                next24BitRandom() or (next24BitRandom() shl 24) or (next24BitRandom() shl 48)
            }
        }
    }

    internal fun calcHash(str: String): Long {
        if (str.length > maxLength)
            throw IllegalStateException("Names with length greater than $maxLength are not supported")
        var result = 0L
        for (i in str.indices) {
            result = result xor randTable[2 * i][str[i].toInt() and 0xFF]
            result = result xor randTable[2 * i + 1][(str[i].toInt() shr 8) and 0xFF]
        }
        return result
    }

    companion object {
        private val calculator = HashCalculator(314159265, 1000)

        @JvmStatic
        fun calcHash(str: String) = calculator.calcHash(str)
    }
}

internal fun <K, V> concurrentMapOf(vararg pairs: Pair<K, V>): MutableMap<K, V>
        = java.util.concurrent.ConcurrentHashMap<K, V>().apply { putAll(pairs) }