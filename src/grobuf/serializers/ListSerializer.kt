package grobuf.serializers

import grobuf.DataCorruptedException
import grobuf.GroBufTypeCode
import java.util.ArrayList

internal class ListSerializer: FragmentSerializer<ArrayList<*>?>() {

    override fun countSize(context: WriteContext, obj: ArrayList<*>?): Int {
        if (obj == null) return 1
        var size = 1 /* typeCode */ + 4 /* data length */ + 4 /* list size */
        obj.forEach { size += elementSerializer.countSize(context, it) }
        return size
    }

    override fun write(context: WriteContext, obj: ArrayList<*>?) {
        if (obj == null) {
            writeByteSafe(context.result, context.index, GroBufTypeCode.Empty.value)
            context.index++
            return
        }
        val result = context.result
        val index = context.index
        ensureSize(result, index, 9)
        writeByteUnsafe(result, index, GroBufTypeCode.Array.value)
        val start = index + 1
        writeIntUnsafe(result, start + 4, obj.size)
        context.index = index + 9
        obj.forEach { elementSerializer.write(context, it) }
        writeIntUnsafe(result, start, context.index - start - 4)
    }

    override fun read(context: ReadContext): ArrayList<*>? {
        val typeCode = readByteSafe(context.data, context.index)
        context.index++
        if (typeCode == GroBufTypeCode.Empty.value)
            return null
        if (typeCode != GroBufTypeCode.Array.value) {
            skipValue(typeCode.toInt(), context)
            return null
        }
        val data = context.data
        val index = context.index
        ensureSize(data, index, 8)
        val dataLength = readIntUnsafe(data, index)
        val size = readIntUnsafe(data, index + 4)
        context.index = index + 8
        val result = ArrayList<Any?>(size)
        for (i in 0 until size) {
            val element = elementSerializer.read(context)
            result.add(element)
        }
        if (context.index != index + 4 + dataLength)
            throw DataCorruptedException("Bad data length")
        return result
    }

    override fun initialize(serializers: Array<Any?>) {
        @Suppress("UNCHECKED_CAST")
        elementSerializer = serializers[0] as FragmentSerializer<Any?>
    }

    private lateinit var elementSerializer: FragmentSerializer<Any?>
}