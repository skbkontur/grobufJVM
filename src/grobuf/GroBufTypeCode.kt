package grobuf

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

enum class GroBufTypeCode(val value: Byte, val length: Int, val erasedType: Class<*>? = null) {
    Empty(0, 0),
    Object(1, -1),
    Array(2, -1, kotlin.Array<Any?>::class.java),
    Int8(3, 1, java.lang.Byte::class.java),
    UInt8(4, 1, java.lang.Byte::class.java),
    Int16(5, 2, java.lang.Short::class.java),
    UInt16(6, 2, java.lang.Short::class.java),
    Int32(7, 4, java.lang.Integer::class.java),
    UInt32(8, 4, java.lang.Integer::class.java),
    Int64(9, 8, java.lang.Long::class.java),
    UInt64(10, 8, java.lang.Long::class.java),
    Single(11, 4, java.lang.Float::class.java),
    Double(12, 8, java.lang.Double::class.java),
    Decimal(13, 16, grobuf.Decimal::class.java),
    String(14, -1, kotlin.String::class.java),
    Guid(15, 16, UUID::class.java),
    Enum(16, 8),
    Boolean(17, 1, java.lang.Boolean::class.java),
    //DateTimeOld(18), not supported for now.
    Int8Array(19, -1, ByteArray::class.java),
    UInt8Array(20, -1, ByteArray::class.java),
    Int16Array(21, -1, ShortArray::class.java),
    UInt16Array(22, -1, ShortArray::class.java),
    Int32Array(23, -1, IntArray::class.java),
    UInt32Array(24, -1, IntArray::class.java),
    Int64Array(25, -1, LongArray::class.java),
    UInt64Array(26, -1, LongArray::class.java),
    SingleArray(27, -1, FloatArray::class.java),
    DoubleArray(28, -1, kotlin.DoubleArray::class.java),
    BooleanArray(29, -1, kotlin.BooleanArray::class.java),
    Dictionary(30, -1, LinkedHashMap::class.java),
    DateTimeNew(31, 8, Date::class.java),
    //Reference(32),
    //DateTimeOffset(33),
    Tuple(34, -1)
    //CustomData(-1, -1)
    ;

    companion object {
        val lengths = IntArray(256)
        init {
            enumValues<GroBufTypeCode>().forEach { lengths[it.value.toInt()] = it.length }
        }
    }
}

private val groBufTypeCodeMap = mapOf(
        Byte::class.java                to GroBufTypeCode.Int8,
        java.lang.Byte::class.java      to GroBufTypeCode.Int8,
        Short::class.java               to GroBufTypeCode.Int16,
        java.lang.Short::class.java     to GroBufTypeCode.Int16,
        Int::class.java                 to GroBufTypeCode.Int32,
        java.lang.Integer::class.java   to GroBufTypeCode.Int32,
        Long::class.java                to GroBufTypeCode.Int64,
        java.lang.Long::class.java      to GroBufTypeCode.Int64,
        Char::class.java                to GroBufTypeCode.UInt16,
        java.lang.Character::class.java to GroBufTypeCode.UInt16,
        Boolean::class.java             to GroBufTypeCode.Boolean,
        java.lang.Boolean::class.java   to GroBufTypeCode.Boolean,
        Float::class.java               to GroBufTypeCode.Single,
        java.lang.Float::class.java     to GroBufTypeCode.Single,
        Double::class.java              to GroBufTypeCode.Double,
        java.lang.Double::class.java    to GroBufTypeCode.Double,
        ByteArray::class.java           to GroBufTypeCode.Int8Array,
        ShortArray::class.java          to GroBufTypeCode.Int16Array,
        IntArray::class.java            to GroBufTypeCode.Int32Array,
        LongArray::class.java           to GroBufTypeCode.Int64Array,
        CharArray::class.java           to GroBufTypeCode.UInt16Array,
        BooleanArray::class.java        to GroBufTypeCode.BooleanArray,
        FloatArray::class.java          to GroBufTypeCode.SingleArray,
        DoubleArray::class.java         to GroBufTypeCode.DoubleArray,
        String::class.java              to GroBufTypeCode.String,
        Decimal::class.java             to GroBufTypeCode.Decimal,
        Date::class.java                to GroBufTypeCode.DateTimeNew,
        UUID::class.java                to GroBufTypeCode.Guid,
        HashMap::class.java             to GroBufTypeCode.Dictionary,
        LinkedHashMap::class.java       to GroBufTypeCode.Dictionary,
        TreeMap::class.java             to GroBufTypeCode.Dictionary,
        ArrayList::class.java           to GroBufTypeCode.Array,
        HashSet::class.java             to GroBufTypeCode.Array,
        TreeSet::class.java             to GroBufTypeCode.Array
)

internal val Class<*>.groBufTypeCode: GroBufTypeCode
    get() = groBufTypeCodeMap[this] ?: (when {
        isArray                         -> GroBufTypeCode.Array
        isEnum                          -> GroBufTypeCode.Enum
        superclass == Tuple::class.java -> GroBufTypeCode.Tuple
        else                            -> GroBufTypeCode.Object
    })