package grobuf.serializers

import grobuf.GroBufTypeCode
import grobuf.groBufTypeCode

internal class AnySerializer : FragmentSerializer<Any?>() {

    private val serializers = arrayOfNulls<FragmentSerializer<Any?>>(256)

    override fun countSize(context: WriteContext, obj: Any?): Int {
        val typeCode = if (obj == null) GroBufTypeCode.Empty else obj::class.java.groBufTypeCode
        return serializers[typeCode.value.toInt() and 0xFF]?.countSize(context, obj) ?: 1
    }

    override fun write(context: WriteContext, obj: Any?) {
        val typeCode = if (obj == null) GroBufTypeCode.Empty else obj::class.java.groBufTypeCode
        val serializer = serializers[typeCode.value.toInt() and 0xFF]
        if (serializer == null) {
            writeByteSafe(context.result, context.index, GroBufTypeCode.Empty.value)
            context.index++
            return
        }
        serializer.write(context, obj)
    }

    override fun read(context: ReadContext): Any? {
        val typeCode = readByteSafe(context.data, context.index)
        val serializer = serializers[typeCode.toInt() and 0xFF]
        if (serializer == null) {
            context.index++
            skipValue(typeCode.toInt(), context)
            return null
        }
        return serializer.read(context)
    }

    override fun initialize(serializers: Array<Any?>) {
        val requiredTypes = enumValues<GroBufTypeCode>()
                .sortedBy { it.value }
                .filter { it.erasedType != null }
        for (index in requiredTypes.indices) {
            @Suppress("UNCHECKED_CAST")
            this.serializers[requiredTypes[index].value.toInt() and 0xFF] = serializers[index] as FragmentSerializer<Any?>
        }
    }

    companion object {
        val requiredTypes = enumValues<GroBufTypeCode>()
                .sortedBy { it.value }
                .map { it.erasedType }
                .filterNotNull()
    }
}