package grobuf.serializers

import grobuf.GroBufTypeCode
import sun.misc.Unsafe

class WriteContext(@JvmField var result: ByteArray, @JvmField var index: Int) {
    constructor(): this(ByteArray(0), 0)
}

class ReadContext(@JvmField var data: ByteArray, @JvmField var index: Int) {
    constructor(): this(ByteArray(0), 0)
}

private val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe").let {
    it.isAccessible = true
    it.get(null) as Unsafe
}

private val byteArrayDataOffset    = theUnsafe.arrayBaseOffset(ByteArray::class.java).toLong()
private val shortArrayDataOffset   = theUnsafe.arrayBaseOffset(ShortArray::class.java).toLong()
private val intArrayDataOffset     = theUnsafe.arrayBaseOffset(IntArray::class.java).toLong()
private val longArrayDataOffset    = theUnsafe.arrayBaseOffset(LongArray::class.java).toLong()
private val charArrayDataOffset    = theUnsafe.arrayBaseOffset(CharArray::class.java).toLong()
private val booleanArrayDataOffset = theUnsafe.arrayBaseOffset(BooleanArray::class.java).toLong()
private val floatArrayDataOffset   = theUnsafe.arrayBaseOffset(FloatArray::class.java).toLong()
private val doubleArrayDataOffset  = theUnsafe.arrayBaseOffset(DoubleArray::class.java).toLong()

@Suppress("unused")
internal abstract class FragmentSerializer<T> {

    protected val unsafe = theUnsafe

    abstract fun countSize(context: WriteContext, obj: T): Int
    abstract fun write(context: WriteContext, obj: T)
    abstract fun read(context: ReadContext): T

    protected fun checkTypeCode(typeCode: Int) {
        if (GroBufTypeCode.lengths[typeCode] == 0)
            throw Error("Unknown type code: $typeCode") // TODO Create DataCorruptedException.
    }

    protected fun skipValue(typeCode: Int, context: ReadContext) {
        var length = GroBufTypeCode.lengths[typeCode]
        if (length < 0)
            length = readIntSafe(context.data, context.index) + 4
        context.index += length
    }

    protected fun throwBadDataLengthError(): Nothing = throw Error("Bad data length")

    //------------Write unsafe------------------------------------------------------------------//

    protected fun writeByteUnsafe(array: ByteArray, offset: Int, value: Byte) {
        unsafe.putByte(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeShortUnsafe(array: ByteArray, offset: Int, value: Short) {
        unsafe.putShort(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeIntUnsafe(array: ByteArray, offset: Int, value: Int) {
        unsafe.putInt(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeLongUnsafe(array: ByteArray, offset: Int, value: Long) {
        unsafe.putLong(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeBooleanUnsafe(array: ByteArray, offset: Int, value: Boolean) {
        unsafe.putBoolean(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeCharUnsafe(array: ByteArray, offset: Int, value: Char) {
        unsafe.putChar(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeFloatUnsafe(array: ByteArray, offset: Int, value: Float) {
        unsafe.putFloat(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeDoubleUnsafe(array: ByteArray, offset: Int, value: Double) {
        unsafe.putDouble(array, byteArrayDataOffset + offset, value)
    }

    //------------Write safe------------------------------------------------------------------//

    protected fun writeByteSafe(array: ByteArray, offset: Int, value: Byte) {
        ensureSize(array, offset, 1)
        unsafe.putByte(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeShortSafe(array: ByteArray, offset: Int, value: Short) {
        ensureSize(array, offset, 2)
        unsafe.putShort(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeIntSafe(array: ByteArray, offset: Int, value: Int) {
        ensureSize(array, offset, 4)
        unsafe.putInt(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeLongSafe(array: ByteArray, offset: Int, value: Long) {
        ensureSize(array, offset, 8)
        unsafe.putLong(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeBooleanSafe(array: ByteArray, offset: Int, value: Boolean) {
        ensureSize(array, offset, 1)
        unsafe.putBoolean(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeCharSafe(array: ByteArray, offset: Int, value: Char) {
        ensureSize(array, offset, 2)
        unsafe.putChar(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeFloatSafe(array: ByteArray, offset: Int, value: Float) {
        ensureSize(array, offset, 4)
        unsafe.putFloat(array, byteArrayDataOffset + offset, value)
    }

    protected fun writeDoubleSafe(array: ByteArray, offset: Int, value: Double) {
        ensureSize(array, offset, 8)
        unsafe.putDouble(array, byteArrayDataOffset + offset, value)
    }

    //------------Read unsafe------------------------------------------------------------------//

    protected fun readByteUnsafe(array: ByteArray, offset: Int): Byte {
        return unsafe.getByte(array, byteArrayDataOffset + offset)
    }

    protected fun readShortUnsafe(array: ByteArray, offset: Int): Short {
        return unsafe.getShort(array, byteArrayDataOffset + offset)
    }

    protected fun readIntUnsafe(array: ByteArray, offset: Int): Int {
        return unsafe.getInt(array, byteArrayDataOffset + offset)
    }

    protected fun readLongUnsafe(array: ByteArray, offset: Int): Long {
        return unsafe.getLong(array, byteArrayDataOffset + offset)
    }

    protected fun readBooleanUnsafe(array: ByteArray, offset: Int): Boolean {
        return unsafe.getBoolean(array, byteArrayDataOffset + offset)
    }

    protected fun readCharUnsafe(array: ByteArray, offset: Int): Char {
        return unsafe.getChar(array, byteArrayDataOffset + offset)
    }

    protected fun readFloatUnsafe(array: ByteArray, offset: Int): Float {
        return unsafe.getFloat(array, byteArrayDataOffset + offset)
    }

    protected fun readDoubleUnsafe(array: ByteArray, offset: Int): Double {
        return unsafe.getDouble(array, byteArrayDataOffset + offset)
    }

    //------------Read safe------------------------------------------------------------------//

    protected fun readByteSafe(array: ByteArray, offset: Int): Byte {
        ensureSize(array, offset, 1)
        return unsafe.getByte(array, byteArrayDataOffset + offset)
    }

    protected fun readShortSafe(array: ByteArray, offset: Int): Short {
        ensureSize(array, offset, 2)
        return unsafe.getShort(array, byteArrayDataOffset + offset)
    }

    protected fun readIntSafe(array: ByteArray, offset: Int): Int {
        ensureSize(array, offset, 4)
        return unsafe.getInt(array, byteArrayDataOffset + offset)
    }

    protected fun readLongSafe(array: ByteArray, offset: Int): Long {
        ensureSize(array, offset, 8)
        return unsafe.getLong(array, byteArrayDataOffset + offset)
    }

    protected fun readBooleanSafe(array: ByteArray, offset: Int): Boolean {
        ensureSize(array, offset, 1)
        return unsafe.getBoolean(array, byteArrayDataOffset + offset)
    }

    protected fun readCharSafe(array: ByteArray, offset: Int): Char {
        ensureSize(array, offset, 2)
        return unsafe.getChar(array, byteArrayDataOffset + offset)
    }

    protected fun readFloatSafe(array: ByteArray, offset: Int): Float {
        ensureSize(array, offset, 4)
        return unsafe.getFloat(array, byteArrayDataOffset + offset)
    }

    protected fun readDoubleSafe(array: ByteArray, offset: Int): Double {
        ensureSize(array, offset, 8)
        return unsafe.getDouble(array, byteArrayDataOffset + offset)
    }

    //------------Write arrays safe------------------------------------------------------------------//

    protected fun writeByteArraySafe(dest: ByteArray, offset: Int, source: ByteArray) {
        val size = source.size
        ensureSize(dest, offset, size)
        unsafe.copyMemory(source, byteArrayDataOffset, dest, byteArrayDataOffset + offset, size.toLong())
    }

    protected fun writeShortArraySafe(dest: ByteArray, offset: Int, source: ShortArray) {
        val size = source.size * 2
        ensureSize(dest, offset, size)
        unsafe.copyMemory(source, shortArrayDataOffset, dest, byteArrayDataOffset + offset, size.toLong())
    }

    protected fun writeIntArraySafe(dest: ByteArray, offset: Int, source: IntArray) {
        val size = source.size * 4
        ensureSize(dest, offset, size)
        unsafe.copyMemory(source, intArrayDataOffset, dest, byteArrayDataOffset + offset, size.toLong())
    }

    protected fun writeLongArraySafe(dest: ByteArray, offset: Int, source: LongArray) {
        val size = source.size * 8
        ensureSize(dest, offset, size)
        unsafe.copyMemory(source, longArrayDataOffset, dest, byteArrayDataOffset + offset, size.toLong())
    }

    protected fun writeCharArraySafe(dest: ByteArray, offset: Int, source: CharArray) {
        val size = source.size * 2
        ensureSize(dest, offset, size)
        unsafe.copyMemory(source, charArrayDataOffset, dest, byteArrayDataOffset + offset, size.toLong())
    }

    protected fun writeBooleanArraySafe(dest: ByteArray, offset: Int, source: BooleanArray) {
        val size = source.size
        ensureSize(dest, offset, size)
        unsafe.copyMemory(source, booleanArrayDataOffset, dest, byteArrayDataOffset + offset, size.toLong())
    }

    protected fun writeFloatArraySafe(dest: ByteArray, offset: Int, source: FloatArray) {
        val size = source.size * 4
        ensureSize(dest, offset, size)
        unsafe.copyMemory(source, floatArrayDataOffset, dest, byteArrayDataOffset + offset, size.toLong())
    }

    protected fun writeDoubleArraySafe(dest: ByteArray, offset: Int, source: DoubleArray) {
        val size = source.size * 8
        ensureSize(dest, offset, size)
        unsafe.copyMemory(source, doubleArrayDataOffset, dest, byteArrayDataOffset + offset, size.toLong())
    }

    //------------Read arrays safe------------------------------------------------------------------//

    protected fun readByteArraySafe(dest: ByteArray, offset: Int, source: ByteArray) {
        val size = dest.size
        ensureSize(source, offset, size)
        unsafe.copyMemory(source, byteArrayDataOffset + offset, dest, byteArrayDataOffset, size.toLong())
    }

    protected fun readShortArraySafe(dest: ShortArray, offset: Int, source: ByteArray) {
        val size = dest.size * 2
        ensureSize(source, offset, size)
        unsafe.copyMemory(source, byteArrayDataOffset + offset, dest, shortArrayDataOffset, size.toLong())
    }

    protected fun readIntArraySafe(dest: IntArray, offset: Int, source: ByteArray) {
        val size = dest.size * 4
        ensureSize(source, offset, size)
        unsafe.copyMemory(source, byteArrayDataOffset + offset, dest, intArrayDataOffset, size.toLong())
    }

    protected fun readLongArraySafe(dest: LongArray, offset: Int, source: ByteArray) {
        val size = dest.size * 8
        ensureSize(source, offset, size)
        unsafe.copyMemory(source, byteArrayDataOffset + offset, dest, longArrayDataOffset, size.toLong())
    }

    protected fun readCharArraySafe(dest: CharArray, offset: Int, source: ByteArray) {
        val size = dest.size * 2
        ensureSize(source, offset, size)
        unsafe.copyMemory(source, byteArrayDataOffset + offset, dest, charArrayDataOffset, size.toLong())
    }

    protected fun readBooleanArraySafe(dest: BooleanArray, offset: Int, source: ByteArray) {
        val size = dest.size
        ensureSize(source, offset, size)
        unsafe.copyMemory(source, byteArrayDataOffset + offset, dest, booleanArrayDataOffset, size.toLong())
    }

    protected fun readFloatArraySafe(dest: FloatArray, offset: Int, source: ByteArray) {
        val size = dest.size * 4
        ensureSize(source, offset, size)
        unsafe.copyMemory(source, byteArrayDataOffset + offset, dest, floatArrayDataOffset, size.toLong())
    }

    protected fun readDoubleArraySafe(dest: DoubleArray, offset: Int, source: ByteArray) {
        val size = dest.size * 8
        ensureSize(source, offset, size)
        unsafe.copyMemory(source, byteArrayDataOffset + offset, dest, doubleArrayDataOffset, size.toLong())
    }

    protected fun ensureSize(array: ByteArray, offset: Int, size: Int) {
        if (offset + size > array.size)
            throw Error("Unexpected end of data") // TODO Create DataCorruptedException.
    }

    protected fun createInstance(klass: Class<*>): Any {
        return unsafe.allocateInstance(klass)
    }
}