package grobuf.serializers

import grobuf.Decimal
import grobuf.GroBufTypeCode

internal class DecimalSerializer : FragmentSerializer<Decimal?>() {

    override fun countSize(context: WriteContext, obj: Decimal?) =
            if (obj == null) 1
            else 1 /* typeCode */ + 16

    override fun write(context: WriteContext, obj: Decimal?) {
        if (obj == null) {
            writeByteSafe(context.result, context.index, GroBufTypeCode.Empty.value)
            context.index++
            return
        }
        val index = context.index
        val result = context.result
        ensureSize(result, index, 17)
        writeByteUnsafe(result, index, GroBufTypeCode.Decimal.value)
        writeIntUnsafe(result, index + 1 + 0 , obj.flags)
        writeIntUnsafe(result, index + 1 + 4 , obj.hi)
        writeIntUnsafe(result, index + 1 + 8 , obj.lo)
        writeIntUnsafe(result, index + 1 + 12, obj.mid)
        context.index = index + 17
    }

    override fun read(context: ReadContext): Decimal? {
        val typeCode = readByteSafe(context.data, context.index)
        when (typeCode) {
            GroBufTypeCode.Empty.value -> {
                context.index++
                return null
            }

            GroBufTypeCode.Decimal.value -> {
                val index = context.index
                val data = context.data
                ensureSize(data, index, 17)
                val flags = readIntUnsafe(data, index + 1 + 0)
                val hi    = readIntUnsafe(data, index + 1 + 4)
                val lo    = readIntUnsafe(data, index + 1 + 8)
                val mid   = readIntUnsafe(data, index + 1 + 12)
                context.index = index + 17
                return Decimal(flags, hi, lo, mid)
            }

            GroBufTypeCode.Int8.value,
            GroBufTypeCode.UInt8.value -> return Decimal.valueOf(byteSerializer.read(context).toInt())

            GroBufTypeCode.Int16.value,
            GroBufTypeCode.UInt16.value -> return Decimal.valueOf(shortSerializer.read(context).toInt())

            GroBufTypeCode.Int32.value,
            GroBufTypeCode.UInt32.value -> return Decimal.valueOf(intSerializer.read(context))

            GroBufTypeCode.Int64.value,
            GroBufTypeCode.UInt64.value -> return Decimal.valueOf(longSerializer.read(context))

            GroBufTypeCode.Boolean.value -> return if (booleanSerializer.read(context)) Decimal.ONE else Decimal.ZERO

            GroBufTypeCode.Single.value -> return Decimal.valueOf(floatSerializer.read(context))

            GroBufTypeCode.Double.value -> return Decimal.valueOf(doubleSerializer.read(context))

            else -> {
                context.index++
                skipValue(typeCode.toInt(), context)
                return Decimal.ZERO
            }
        }
    }

    private lateinit var byteSerializer    : FragmentSerializer<Byte>
    private lateinit var shortSerializer   : FragmentSerializer<Short>
    private lateinit var intSerializer     : FragmentSerializer<Int>
    private lateinit var longSerializer    : FragmentSerializer<Long>
    private lateinit var booleanSerializer : FragmentSerializer<Boolean>
    private lateinit var floatSerializer   : FragmentSerializer<Float>
    private lateinit var doubleSerializer  : FragmentSerializer<Double>

    @Suppress("UNCHECKED_CAST")
    override fun initialize(serializers: Array<Any?>) {
        byteSerializer    = serializers[0] as FragmentSerializer<Byte>
        shortSerializer   = serializers[1] as FragmentSerializer<Short>
        intSerializer     = serializers[2] as FragmentSerializer<Int>
        longSerializer    = serializers[3] as FragmentSerializer<Long>
        booleanSerializer = serializers[4] as FragmentSerializer<Boolean>
        floatSerializer   = serializers[5] as FragmentSerializer<Float>
        doubleSerializer  = serializers[6] as FragmentSerializer<Double>
    }

    companion object {
        val requiredTypes = listOf(
                Byte::class.java,
                Short::class.java,
                Int::class.java,
                Long::class.java,
                Boolean::class.java,
                Float::class.java,
                Double::class.java
        )
    }
}