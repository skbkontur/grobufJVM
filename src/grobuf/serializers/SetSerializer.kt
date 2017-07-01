package grobuf.serializers

import grobuf.DataCorruptedException
import grobuf.GroBufTypeCode
import java.util.TreeSet

internal abstract class SetSerializer<TMap: MutableSet<Any?>> : FragmentSerializer<TMap?>() {

    protected fun countSizeInternal(context: WriteContext, obj: TMap?): Int {
        if (obj == null) return 1
        var size = 1 /* typeCode */ + 4 /* data length */ + 4 /* map size */
        obj.forEach { size += elementSerializer.countSize(context, it) }
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
        writeByteUnsafe(result, index, GroBufTypeCode.Array.value)
        val start = index + 1
        writeIntUnsafe(result, start + 4, obj.size)
        context.index = index + 9
        obj.forEach { elementSerializer.write(context, it) }
        writeIntUnsafe(result, start, context.index - start - 4)
    }

    protected fun readInternal(context: ReadContext): TMap? {
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
        val result = createEmpty(size)
        for (i in 0 until size) {
            val element = elementSerializer.read(context)
            result.add(element)
        }
        if (context.index != index + 4 + dataLength)
            throw DataCorruptedException("Bad data length")
        return result
    }

    override fun initialize(serializers: Array<Any?>) {
        elementSerializer = serializers[0] as FragmentSerializer<Any?>
    }

    private lateinit var elementSerializer: FragmentSerializer<Any?>

    abstract fun createEmpty(size: Int): TMap
}

internal class HashSetSerializer: SetSerializer<HashSet<Any?>>() {

    override fun countSize(context: WriteContext, obj: HashSet<Any?>?) = countSizeInternal(context, obj)

    override fun write(context: WriteContext, obj: HashSet<Any?>?) = writeInternal(context, obj)

    override fun read(context: ReadContext) = readInternal(context)

    override fun createEmpty(size: Int) = HashSet<Any?>(size)
}

internal class TreeSetSerializer: SetSerializer<TreeSet<Any?>>() {

    override fun countSize(context: WriteContext, obj: TreeSet<Any?>?) = countSizeInternal(context, obj)

    override fun write(context: WriteContext, obj: TreeSet<Any?>?) = writeInternal(context, obj)

    override fun read(context: ReadContext) = readInternal(context)

    override fun createEmpty(size: Int) = TreeSet<Any?>()
}