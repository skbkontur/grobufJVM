package grobuf

import java.util.*

enum class GroBufTypeCode(val value: Byte, val length: Int) {
    Empty(0, 1),
    Object(1, -1),
    Array(2, -1),
    Int8(3, 1),
    UInt8(4, 1),
    Int16(5, 2),
    UInt16(6, 2),
    Int32(7, 4),
    UInt32(8, 4),
    Int64(9, 8),
    UInt64(10, 8),
    Single(11, 4),
    Double(12, 8),
    Decimal(13, 16),
    String(14, -1),
    Guid(15, 16),
    Enum(16, 8),
    Boolean(17, 1),
    //DateTimeOld(18), not supported for now.
    Int8Array(19, -1),
    UInt8Array(20, -1),
    Int16Array(21, -1),
    UInt16Array(22, -1),
    Int32Array(23, -1),
    UInt32Array(24, -1),
    Int64Array(25, -1),
    UInt64Array(26, -1),
    SingleArray(27, -1),
    DoubleArray(28, -1),
    BooleanArray(29, -1),
    Dictionary(30, -1),
    DateTimeNew(31, 8),
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
        TreeMap::class.java             to GroBufTypeCode.Dictionary
)

internal val Class<*>.groBufTypeCode: GroBufTypeCode
    get() = groBufTypeCodeMap[this] ?: (when {
        isArray                         -> GroBufTypeCode.Array
        isEnum                          -> GroBufTypeCode.Enum
        superclass == Tuple::class.java -> GroBufTypeCode.Tuple
        else                            -> GroBufTypeCode.Object
    })