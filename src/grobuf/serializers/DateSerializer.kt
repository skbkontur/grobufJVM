package grobuf.serializers

import grobuf.GroBufTypeCode
import java.util.*

internal class DateSerializer : FragmentSerializer<Date?>() {

    private val TICKS_PER_MILLI = 10000
    private val TICKS_PER_SECOND = TICKS_PER_MILLI * 1000
    private val TICKS_PER_DAY = TICKS_PER_SECOND.toLong() * 60 * 60 * 24
    private val DAYS_TILL_UNIX_TIME = 719162
    private val TICKS_TILL_UNIX_TIME = DAYS_TILL_UNIX_TIME.toLong() * TICKS_PER_DAY
    @Suppress("INTEGER_OVERFLOW")
    private val LOCAL_MASK    = 0x7FFFFFFFFFFFFFFF + 1
    private val UTC_MASK      = 0x4000000000000000
    private val TICKS_MASK    = 0x3FFFFFFFFFFFFFFF
    private val TICKS_CEILING = 0x4000000000000000

    override fun countSize(context: WriteContext, obj: Date?) =
            if (obj == null) 1
            else 1 /* type code*/ + 8

    override fun write(context: WriteContext, obj: Date?) {
        if (obj == null) {
            writeByteSafe(context.result, context.index, GroBufTypeCode.Empty.value)
            context.index++
            return
        }
        val result = context.result
        val index = context.index
        ensureSize(result, index, 9)
        writeByteUnsafe(result, index, GroBufTypeCode.DateTimeNew.value)
        val millis = obj.time
        val ticks = millis * TICKS_PER_MILLI + TICKS_TILL_UNIX_TIME
        writeLongUnsafe(result, index + 1, ticks or UTC_MASK)
        context.index = index + 9
    }

    override fun read(context: ReadContext): Date? {
        val typeCode = readByteSafe(context.data, context.index)
        context.index++
        if (typeCode == GroBufTypeCode.Empty.value)
            return null
        if (typeCode != GroBufTypeCode.DateTimeNew.value) {
            skipValue(typeCode.toInt(), context)
            return null
        }
        var ticks = readLongSafe(context.data, context.index)
        context.index += 8
        val local = ticks and LOCAL_MASK != 0L
        ticks = ticks and TICKS_MASK
        if (local) {
            if (ticks > TICKS_CEILING - TICKS_PER_DAY)
                ticks -= TICKS_CEILING
        }
        val millis = (ticks - TICKS_TILL_UNIX_TIME) / TICKS_PER_MILLI
        return Date(millis)
    }

    override fun initialize(serializers: Array<Any?>) { }
}