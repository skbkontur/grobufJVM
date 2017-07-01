package grobuf.serializers

import grobuf.DataCorruptedException
import grobuf.GroBufTypeCode
import java.util.*

internal abstract class MapSerializer<TMap: MutableMap<Any?, Any?>> : FragmentSerializer<TMap?>() {

    protected fun countSizeInternal(context: WriteContext, obj: TMap?): Int {
        if (obj == null) return 1
        var size = 1 /* typeCode */ + 4 /* data length */ + 4 /* map size */
        obj.forEach { key, value ->
            size += keySerializer.countSize(context, key)
            size += valueSerializer.countSize(context, value)
        }
        return size
    }

    protected fun writeInternal(context: WriteContext, obj: TMap?) {
        if (obj == null) {
            writeByteSafe(context.result, context.index, GroBufTypeCode.Empty.value)
            context.index++
            return
        }
        val result = context.result
        val index = context.index
        ensureSize(result, index, 9)
        writeByteUnsafe(result, index, GroBufTypeCode.Dictionary.value)
        val start = index + 1
        writeIntUnsafe(result, start + 4, obj.size)
        context.index = index + 9
        obj.forEach { key, value ->
            keySerializer.write(context, key)
            valueSerializer.write(context, value)
        }
        writeIntUnsafe(result, start, context.index - start - 4)
    }

    protected fun readInternal(context: ReadContext): TMap? {
        val typeCode = readByteSafe(context.data, context.index)
        context.index++
        if (typeCode == GroBufTypeCode.Empty.value)
            return null
        if (typeCode != GroBufTypeCode.Dictionary.value) {
            skipValue(typeCode.toInt(), context)
            return null
        }
        val data = context.data
        val index = context.index
        ensureSize(data, index, 8)
        val dataLength = readIntUnsafe(data, index)
        val size = readIntUnsafe(data, index + 4)
        context.index = index + 8
        val result = createEmpty(size)
        for (i in 0 until size) {
            val key = keySerializer.read(context)
            val value = valueSerializer.read(context)
            result.put(key, value)
        }
        if (context.index != index + 4 + dataLength)
            throw DataCorruptedException("Bad data length")
        return result
    }

    override fun initialize(serializers: Array<Any?>) {
        keySerializer = serializers[0] as FragmentSerializer<Any?>
        valueSerializer = serializers[1] as FragmentSerializer<Any?>
    }

    private lateinit var keySerializer: FragmentSerializer<Any?>
    private lateinit var valueSerializer: FragmentSerializer<Any?>

    abstract fun createEmpty(size: Int): TMap
}

internal class HashMapSerializer : MapSerializer<HashMap<Any?, Any?>>() {

    override fun countSize(context: WriteContext, obj: HashMap<Any?, Any?>?) = countSizeInternal(context, obj)

    override fun write(context: WriteContext, obj: HashMap<Any?, Any?>?) = writeInternal(context, obj)

    override fun read(context: ReadContext) = readInternal(context)

    override fun createEmpty(size: Int): HashMap<Any?, Any?> {
        return HashMap(size)
    }
}

internal class LinkedHashMapSerializer : MapSerializer<LinkedHashMap<Any?, Any?>>() {
    override fun countSize(context: WriteContext, obj: LinkedHashMap<Any?, Any?>?) = countSizeInternal(context, obj)

    override fun write(context: WriteContext, obj: LinkedHashMap<Any?, Any?>?) = writeInternal(context, obj)

    override fun read(context: ReadContext) = readInternal(context)

    override fun createEmpty(size: Int): LinkedHashMap<Any?, Any?> {
        return LinkedHashMap(size)
    }
}

internal class TreeMapSerializer : MapSerializer<TreeMap<Any?, Any?>>() {
    override fun countSize(context: WriteContext, obj: TreeMap<Any?, Any?>?) = countSizeInternal(context, obj)

    override fun write(context: WriteContext, obj: TreeMap<Any?, Any?>?) = writeInternal(context, obj)

    override fun read(context: ReadContext) = readInternal(context)

    override fun createEmpty(size: Int): TreeMap<Any?, Any?> {
        return TreeMap()
    }
}