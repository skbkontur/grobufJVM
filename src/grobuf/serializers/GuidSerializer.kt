package grobuf.serializers

import grobuf.GroBufTypeCode
import java.util.*

internal class GuidSerializer : FragmentSerializer<UUID?>() {

    override fun countSize(context: WriteContext, obj: UUID?) =
            if (obj == null) 1
            else 1 /* type code */ + 16

    override fun write(context: WriteContext, obj: UUID?) {
        if (obj == null) {
            writeByteSafe(context.result, context.index, GroBufTypeCode.Empty.value)
            context.index++
            return
        }
        val result = context.result
        val index = context.index
        ensureSize(result, index, 17)
        writeByteUnsafe(result, index, GroBufTypeCode.Guid.value)
        writeLongUnsafe(result, index + 1 + 0, obj.leastSignificantBits)
        writeLongUnsafe(result, index + 1 + 8, obj.mostSignificantBits)
        context.index = index + 17
    }

    override fun read(context: ReadContext): UUID? {
        val typeCode = readByteSafe(context.data, context.index)
        context.index++
        if (typeCode == GroBufTypeCode.Empty.value)
            return null
        if (typeCode != GroBufTypeCode.Guid.value) {
            skipValue(typeCode.toInt(), context)
            return null
        }
        val data = context.data
        val index = context.index
        ensureSize(data, index, 16)
        val lo = readLongUnsafe(data, index + 0)
        val hi = readLongUnsafe(data, index + 8)
        context.index  = index + 16
        return UUID(hi, lo)
    }

    override fun initialize(serializers: Array<Any?>) { }
}