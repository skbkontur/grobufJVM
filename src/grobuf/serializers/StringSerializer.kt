package grobuf.serializers

import grobuf.GroBufTypeCode

internal class StringSerializer : FragmentSerializer<String?>() {

    private val stringValueOffset = unsafe.objectFieldOffset(String::class.java.getDeclaredField("value"))

    override fun countSize(context: WriteContext, obj: String?) =
            if (obj == null) 1
            else 1 /* typeCode */ + 4 /* length */ + obj.length * 2

    override fun write(context: WriteContext, obj: String?) {
        if (obj == null) {
            writeByteSafe(context.result, context.index, GroBufTypeCode.Empty.value)
            context.index++
            return
        }
        writeByteSafe(context.result, context.index, GroBufTypeCode.String.value)
        context.index++
        writeIntSafe(context.result, context.index, obj.length * 2)
        context.index += 4
        writeCharArraySafe(context.result, context.index, unsafe.getObject(obj, stringValueOffset) as CharArray)
        context.index += obj.length * 2
    }

    override fun read(context: ReadContext): String? {
        val typeCode = readByteSafe(context.data, context.index)
        context.index++
        if (typeCode == GroBufTypeCode.Empty.value)
            return null
        if (typeCode != GroBufTypeCode.String.value) {
            skipValue(typeCode.toInt(), context)
            return null
        }
        val length = readIntSafe(context.data, context.index)
        context.index += 4
        val arr = CharArray(length / 2)
        readCharArraySafe(arr, context.index, context.data)
        context.index += length
        val str = createInstance(String::class.java) as String
        unsafe.putObject(str, stringValueOffset, arr)
        return str
    }

    override fun initialize(serializers: Array<Any?>) { }
}