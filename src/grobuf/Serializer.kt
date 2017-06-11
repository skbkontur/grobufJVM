package grobuf

import org.objectweb.asm.*

import sun.misc.Unsafe
import java.io.FileOutputStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.ByteBuffer
import kotlin.reflect.KClass

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
    //Decimal(13, 16),
    String(14, -1),
    //Guid(15, 16),
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
    //Dictionary(30, -1),
    //DateTimeNew(31, 8),
    //Reference(32),
    //DateTimeOffset(33),
    //Tuple(34),
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
        Byte::class.java to GroBufTypeCode.Int8,
        Short::class.java to GroBufTypeCode.Int16,
        Int::class.java to GroBufTypeCode.Int32,
        Long::class.java to GroBufTypeCode.Int64,
        Char::class.java to GroBufTypeCode.UInt16,
        Boolean::class.java to GroBufTypeCode.Boolean,
        Float::class.java to GroBufTypeCode.Single,
        Double::class.java to GroBufTypeCode.Double,
        ByteArray::class.java to GroBufTypeCode.Int8Array,
        ShortArray::class.java to GroBufTypeCode.Int16Array,
        IntArray::class.java to GroBufTypeCode.Int32Array,
        LongArray::class.java to GroBufTypeCode.Int64Array,
        CharArray::class.java to GroBufTypeCode.UInt16Array,
        BooleanArray::class.java to GroBufTypeCode.BooleanArray,
        FloatArray::class.java to GroBufTypeCode.SingleArray,
        DoubleArray::class.java to GroBufTypeCode.DoubleArray,
        String::class.java to GroBufTypeCode.String
)

internal val Class<*>.groBufTypeCode: GroBufTypeCode
    get() = groBufTypeCodeMap[this] ?: (when {
        isArray -> GroBufTypeCode.Array
        isEnum -> GroBufTypeCode.Enum
        else -> GroBufTypeCode.Object
    })

internal val Class<*>.isReference get() = jvmPrimitiveType == null

internal class GroBufRandom(seed: Int) {
    private var m_inext = 0
    private var m_inextp = 0
    private val m_seedArray = IntArray(56)

    private fun Int.abs() = if (this >= 0) this else -this

    init {
        @Suppress("INTEGER_OVERFLOW")
        val num4 = if (seed == -0x80000000.toInt()) 0x7fffffff else seed.abs()
        var num2 = 0x9a4ec86 - num4
        m_seedArray[55] = num2
        var num3 = 1
        for (i in 1..54) {
            val index = (21 * i) % 55
            m_seedArray[index] = num3
            num3 = num2 - num3
            if (num3 < 0)
                num3 += 0x7fffffff
            num2 = m_seedArray[index]
        }
        for (j in 1..4) {
            for (k in 1..55) {
                m_seedArray[k] -= m_seedArray[1 + (k + 30) % 55]
                if (m_seedArray[k] < 0)
                    m_seedArray[k] += 0x7fffffff
            }
        }
        m_inext = 0
        m_inextp = 21
    }

    fun next(): Int {
        var inext = m_inext
        var inextp = m_inextp
        if (++inext >= 56)
            inext = 1
        if (++inextp >= 56)
            inextp = 1
        var num = m_seedArray[inext] - m_seedArray[inextp]
        if (num == 0x7fffffff)
            num--
        if (num < 0)
            num += 0x7fffffff
        m_seedArray[inext] = num
        m_inext = inext
        m_inextp = inextp
        return num
    }
}

internal class HashCalculator(seed: Int, val maxLength: Int) {
    private val randTable: Array<LongArray>

    init {
        val random = GroBufRandom(seed)
        fun next24BitRandom() = (random.next() and 0xFFFFFF).toLong()
        randTable = Array(maxLength * 2) {
            LongArray(256) {
                next24BitRandom() or (next24BitRandom() shl 24) or (next24BitRandom() shl 48)
            }
        }
    }

    fun calcHash(str: String): Long {
        if (str.length > maxLength)
            throw IllegalStateException("Names with length greater than $maxLength are not supported")
        var result = 0L
        for (i in str.indices) {
            result = result xor randTable[2 * i][str[i].toInt() and 0xFF]
            result = result xor randTable[2 * i + 1][(str[i].toInt() shr 8) and 0xFF]
        }
        return result
    }

    companion object {
        private val calculator = HashCalculator(314159265, 1000)

        fun calcHash(str: String) = calculator.calcHash(str)
    }
}

interface Serializer {
    fun <T> getSize(klass: Class<T>, obj: T): Int
    fun <T: Any> getSize(obj: T) = getSize(obj.javaClass, obj)
    fun <T> serialize(klass: Class<T>, obj: T): ByteArray
    fun <T: Any> serialize(obj: T) = serialize(obj.javaClass, obj)
    fun <T: Any> deserialize(klass: Class<T>, data: ByteArray): T
}

private object DynamicClassesLoader : ClassLoader() {
    fun loadClass(name: String, byteCode: ByteArray): Class<*> {
        return defineClass(name, byteCode, 0, byteCode.size)
    }
}

open class SerializerImpl: Serializer {

    private val collection = FragmentSerializerCollection()

    override fun <T> getSize(klass: Class<T>, obj: T): Int {
        @Suppress("UNCHECKED_CAST")
        return (collection.getFragmentSerializer(klass) as FragmentSerializer<T>).countSize(WriteContext(), obj)
    }

    override fun <T> serialize(klass: Class<T>, obj: T): ByteArray {
        @Suppress("UNCHECKED_CAST")
        val fragmentSerializer = collection.getFragmentSerializer(klass) as FragmentSerializer<T>
        val context = WriteContext()
        val size = fragmentSerializer.countSize(context, obj)
        context.result = ByteArray(size)
        fragmentSerializer.write(context, obj)
        return context.result
    }

    // TODO: do not return null.
    override fun <T : Any> deserialize(klass: Class<T>, data: ByteArray): T {
        @Suppress("UNCHECKED_CAST")
        val fragmentSerializer = collection.getFragmentSerializer(klass) as FragmentSerializer<T>
        return fragmentSerializer.read(ReadContext().also { it.data = data; it.index = 0 })
    }
}

class WriteContext {
    @JvmField var result: ByteArray = ByteArray(0)
    @JvmField var index = 0
}

class ReadContext {
    @JvmField var data: ByteArray = ByteArray(0)
    @JvmField var index = 0
}

private val unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").let {
    it.isAccessible = true
    it.get(null) as Unsafe
}

private val byteArrayDataOffset    = unsafe.arrayBaseOffset(ByteArray::class.java).toLong()
private val shortArrayDataOffset   = unsafe.arrayBaseOffset(ShortArray::class.java).toLong()
private val intArrayDataOffset     = unsafe.arrayBaseOffset(IntArray::class.java).toLong()
private val longArrayDataOffset    = unsafe.arrayBaseOffset(LongArray::class.java).toLong()
private val charArrayDataOffset    = unsafe.arrayBaseOffset(CharArray::class.java).toLong()
private val booleanArrayDataOffset = unsafe.arrayBaseOffset(BooleanArray::class.java).toLong()
private val floatArrayDataOffset   = unsafe.arrayBaseOffset(FloatArray::class.java).toLong()
private val doubleArrayDataOffset  = unsafe.arrayBaseOffset(DoubleArray::class.java).toLong()

@Suppress("unused")
internal abstract class FragmentSerializer<T> {
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

internal abstract class ClassBuilder<out T>(val className: String,
                                            val superClassType: String) {

    private class Field(val name: String, val type: String, val value: Any?, val isLateinit: Boolean)

    private val fields = mutableListOf<Field>()
    protected val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)

    fun build(): T {
        classWriter.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, className, null, superClassType, null)

        buildBody()

        buildConstructor()
        buildInitializeMethod()
        buildCreateMethod()
        classWriter.visitEnd()

        val byteCode = classWriter.toByteArray()

        // TODO: debug output
        val fos = FileOutputStream("$className.class")
        fos.write(byteCode)
        fos.close()

        val builtClass = DynamicClassesLoader.loadClass(className, byteCode)
        val createMethod = builtClass.getMethod("create", Array<Any?>::class.java)
        val arguments = fields
                .filterNot { it.isLateinit }
                .map { it.value }
                .toTypedArray()
        @Suppress("UNCHECKED_CAST")
        return createMethod.invoke(null, arguments) as T
    }

    val argumentsOfInitialize get() = fields.filter { it.isLateinit }.map { it.type }

    protected abstract fun buildBody()

    protected fun defineField(name: String, type: String,
                              value: Any?, isLateinit: Boolean = false): Int {
        classWriter.visitField(Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL, name,
                type.toJVMSignature(), null, null)
                .visitEnd()
        fields += Field(name, type, value, isLateinit)
        return fields.size - 1
    }

    protected fun MethodVisitor.loadThis() {
        loadSlot<Any>(0)
    }

    protected fun MethodVisitor.loadField(fieldId: Int) {
        val field = fields[fieldId]
        loadThis()
        visitFieldInsn(Opcodes.GETFIELD, className, field.name, field.type.toJVMSignature())
    }

    protected fun MethodVisitor.genSwitch(keys: List<Long>, valueSlot: Int,
                                          defaultLabel: Label, caseBuilder: MethodVisitor.(Int) -> Unit) {
        if (keys.size <= 50)
            UnrolledBinarySearchBuilder.build(this, keys, valueSlot, defaultLabel, caseBuilder)
        else
            TODO()
    }

    private fun buildConstructor() {
        classWriter.defineMethod1<Array<Any?>, Void>(Opcodes.ACC_PUBLIC, "<init>").run {
            loadThis()                                           // stack: [this]
            ctorCall0(superClassType)                            // this::super(); stack: []
            initFields(this, fields.filterNot { it.isLateinit }) // init fields; stack: []
            ret<Void>()                                          // return; stack: []
            visitMaxs(3, 2)
            visitEnd()
        }
    }

    private fun buildInitializeMethod() {
        classWriter.defineMethod1<Array<Any?>, Void>(Opcodes.ACC_PUBLIC, "initialize").run {
            initFields(this, fields.filter { it.isLateinit })
            ret<Void>()
            visitMaxs(3, 2)
            visitEnd()
        }
    }

    private fun initFields(method: MethodVisitor, fields: List<Field>) = with(method) {
        fields.forEachIndexed { index, field ->
            loadThis()                                       // stack: [this]
            loadSlot<Array<Any?>>(1)                          // stack: [this, args]
            visitLdcInsn(index)                              // stack: [this, args, index]
            visitInsn(Opcodes.AALOAD)                        // stack: [this, args[index]]
            castObjectTo(field.type)                         // stack: [this, (fieldType)args[index]]
            visitFieldInsn(Opcodes.PUTFIELD, className,
                    field.name, field.type.toJVMSignature()) // this.field = (fieldType)args[index]; stack: []
        }
    }

    private fun buildCreateMethod() {
        classWriter.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "create",
                "([Ljava/lang/Object;)L$className;", null, null).run {
            visitTypeInsn(Opcodes.NEW, className) // stack: [new ClassName() => inst]
            visitInsn(Opcodes.DUP)                // stack: [inst, inst]
            loadSlot<Array<Any?>>(0)               // stack: [inst, inst, args]
            ctorCall1<Array<Any?>>(className)     // inst.<init>(args); stack: [inst]
            ret<Any>()                            // return inst; stack: []
            visitMaxs(3, 1)
            visitEnd()
        }
    }
}

internal sealed class BuildState {
    class Building(val className: String): BuildState()
    class Built(val inst: Any, val requiredBuilders: List<String>): BuildState()
    class Initialized(val inst: Any): BuildState()
}

internal class FragmentSerializerCollection {
    private val serializers = mutableMapOf<String, BuildState>(
            String::class.jvmType to BuildState.Initialized(StringSerializer())
    )
    private val serializerToTypeMap = mutableMapOf<String, String>()

    init {
        serializers.forEach { type, writer ->
            serializerToTypeMap += (writer as BuildState.Initialized).inst::class.jvmType to type
        }
    }

    fun getFragmentSerializerType(klass: Class<*>): String {
        val canonicalName = klass.jvmType
        val buildState = serializers[canonicalName]
        return when (buildState) {
            null -> getFragmentSerializer(klass)::class.jvmType
            is BuildState.Building -> buildState.className
            is BuildState.Built -> buildState.inst::class.jvmType
            is BuildState.Initialized -> buildState.inst::class.jvmType
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getFragmentSerializer() = getFragmentSerializer(T::class.java) as FragmentSerializer<T>

    fun getFragmentSerializer(klass: Class<*>): FragmentSerializer<*> {
        val type = klass.jvmType
        val state = serializers[type]
        when (state) {
            null -> {
                val builder = getSerializerBuilder(klass)
                serializers[type] = BuildState.Building(builder.className)
                serializerToTypeMap[builder.className] = type
                val fragmentSerializer = builder.build()
                serializers[type] = BuildState.Built(fragmentSerializer, builder.argumentsOfInitialize.map { serializerToTypeMap[it]!! })
                initialize(type)
                return fragmentSerializer
            }
            is BuildState.Initialized -> return state.inst as FragmentSerializer<*>
            else -> throw IllegalStateException("Writer for $type is not initialized")
        }
    }

    private fun getSerializerBuilder(klass: Class<*>) = when {
        klass.jvmPrimitiveType != null ->
            PrimitivesSerializerBuilder(this, klass)

        klass.isArray ->
            if (klass.componentType.jvmPrimitiveType != null)
                PrimitivesArraySerializerBuilder(this, klass)
            else
                ArraySerializerBuilder(this, klass)

        else -> ClassSerializerBuilder(this, klass)
    }

    private fun initialize(type: String) {
        dfs(type, mutableSetOf())
    }

    private fun dfs(type: String, visited: MutableSet<String>) {
        visited.add(type)
        val state = serializers[type]
        when (state) {
            null -> throw IllegalStateException("Serializer for $type has not been created")
            is BuildState.Building -> return
            is BuildState.Initialized -> return

            is BuildState.Built -> {
                val requiredBuilders = state.requiredBuilders
                requiredBuilders.forEach {
                    if (!visited.contains(it))
                        dfs(it, visited)
                }
                val instances = requiredBuilders.map {
                    if (it == type)
                        state.inst // A loop.
                    else {
                        val requiredBuilderState = serializers[it]
                        when (requiredBuilderState) {
                            null -> throw IllegalStateException("Serializer for $it has not been created")
                            is BuildState.Building -> return // A cycle.
                            is BuildState.Built -> requiredBuilderState.inst
                            is BuildState.Initialized -> requiredBuilderState.inst
                        }
                    }
                }
                val initializeMethod = state.inst::class.java.getMethod("initialize", Array<Any?>::class.java)
                initializeMethod.invoke(state.inst, instances.toTypedArray())
                serializers[type] = BuildState.Initialized(state.inst)
            }
        }
    }
}

internal abstract class FragmentSerializerBuilderBase(protected val fragmentSerializerCollection: FragmentSerializerCollection,
                                                      protected val klass: Class<*>)
    : ClassBuilder<FragmentSerializer<*>>(
        className      = "${klass.jvmType.toJVMIdentifier()}_Serializer",
        superClassType = FragmentSerializer::class.jvmType) {

    override fun buildBody() {
        buildCountMethod()
        buildWriteMethod()
        buildReadMethod()
    }

    protected fun MethodVisitor.loadObj() {
        loadSlot(klass, 2)
    }

    protected fun MethodVisitor.loadContext() {
        loadSlot<Any>(1 + if (klass.occupiesTwoSlots) 1 else 0)
    }

    //---------- Writing -------------------------------------------------------//

    protected fun MethodVisitor.loadResult() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, WriteContext::class.jvmType, "result", ByteArray::class.jvmSignature)
    }

    protected inline fun <reified T> MethodVisitor.loadIndex() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, T::class.jvmType, "index", Int::class.jvmSignature)
    }

    protected inline fun <reified T> MethodVisitor.increaseIndexBy(value: Int) {
        loadContext()
        loadIndex<T>()
        visitLdcInsn(value)
        visitInsn(Opcodes.IADD)
        visitFieldInsn(Opcodes.PUTFIELD, T::class.jvmType, "index", Int::class.jvmSignature)
    }

    protected inline fun <reified T> MethodVisitor.increaseIndexBy(valueLoader: MethodVisitor.() -> Unit) {
        loadContext()
        loadIndex<T>()
        valueLoader()
        visitInsn(Opcodes.IADD)
        visitFieldInsn(Opcodes.PUTFIELD, T::class.jvmType, "index", Int::class.jvmSignature)
    }

    protected fun MethodVisitor.writeSafe(klass: Class<*>, valueLoader: MethodVisitor.() -> Unit) {
        val jvmPrimitive = klass.jvmPrimitiveType ?: error("Expected a primitive but was $klass")
        loadThis()                                                                       // stack: [this]
        loadResult()                                                                     // stack: [this, result]
        loadIndex<WriteContext>()                                                        // stack: [this, result, index]
        valueLoader()                                                                    // stack: [this, result, index, value]
        callVirtual(className, "write${jvmPrimitive.name}Safe",
                listOf(ByteArray::class.java, Int::class.java, klass), Void::class.java) // this.write<type>Safe(result, index, value); stack: []
        increaseIndexBy<WriteContext>(jvmPrimitive.size)                                 // index += type.size; stack: []
    }

    protected inline fun <reified T> MethodVisitor.writeSafe(noinline valueLoader: MethodVisitor.() -> Unit) {
        val jvmPrimitive = T::class.jvmPrimitiveType ?: error("Expected a primitive but was ${T::class}")
        loadThis()                                                                        // stack: [this]
        loadResult()                                                                      // stack: [this, result]
        loadIndex<WriteContext>()                                                         // stack: [this, result, index]
        valueLoader()                                                                     // stack: [this, result, index, value]
        callVirtual3<ByteArray, Int, T, Void>(className, "write${jvmPrimitive.name}Safe") // this.write<type>Safe(result, index, value); stack: []
        increaseIndexBy<WriteContext>(jvmPrimitive.size)                                  // index += type.size; stack: []
    }

    protected fun MethodVisitor.writeLength(startSlot: Int) {
        loadThis()                                                               // stack: [this]
        loadResult()                                                             // stack: [this, result]
        loadSlot<Int>(startSlot)                                                 // stack: [this, result, start]
        loadIndex<WriteContext>()                                                // stack: [this, result, start, index]
        loadSlot<Int>(startSlot)                                                 // stack: [this, result, start, index, start]
        visitLdcInsn(4)                                                          // stack: [this, result, start, index, start, 4]
        visitInsn(Opcodes.IADD)                                                  // stack: [this, result, start, index, start + 4]
        visitInsn(Opcodes.ISUB)                                                  // stack: [this, result, start, index - start - 4 => length]
        callVirtual3<ByteArray, Int, Int, Void>(className, "writeIntUnsafe")     // this.writeIntUnsafe(result, start, length); stack: []
    }

    private fun buildCountMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "countSize",
                listOf(WriteContext::class.java, klass), Int::class.java).run {
            if (klass.isReference) {
                loadObj()                                      // stack: [obj]
                val notNullLabel = Label()
                visitJumpInsn(Opcodes.IFNONNULL, notNullLabel) // if (obj != null) goto notNull; stack: []
                visitLdcInsn(1)                                // stack: [1]
                ret<Int>()                                     // return 1; stack: []
                visitLabel(notNullLabel)
            }
            countSizeNotNull()
            visitEnd()
        }
        if (klass != Any::class.java) {
            buildBridge(
                    name          = "countSize",
                    argumentTypes = listOf(
                            WriteContext::class.java.notErased(),
                            klass.erased()
                    ),
                    returnType    = Int::class.java.notErased()
            )
        }
    }

    protected abstract fun MethodVisitor.countSizeNotNull()

    private fun buildWriteMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "write",
                        listOf(WriteContext::class.java, klass), Void::class.java).run {
            if (klass.isReference) {
                loadObj()                                                    // stack: [obj]
                val notNullLabel = Label()
                visitJumpInsn(Opcodes.IFNONNULL, notNullLabel)               // if (obj != null) goto notNull; stack: []
                writeSafe<Byte> { visitLdcInsn(GroBufTypeCode.Empty.value) } // this.writeByteSafe(context.result, context.index, Empty); stack: []
                ret<Void>()                                                  // return; stack: []
                visitLabel(notNullLabel)
            }
            writeNotNull()
            visitEnd()
        }
        if (klass != Any::class.java) {
            buildBridge(
                    name          = "write",
                    argumentTypes = listOf(
                            WriteContext::class.java.notErased(),
                            klass.erased()
                    ),
                    returnType    = Void::class.java.notErased()
            )
        }
    }

    protected abstract fun MethodVisitor.writeNotNull()

    //---------- Reading -----------------------------------------------------------//

    protected fun MethodVisitor.loadData() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, ReadContext::class.jvmType, "data", ByteArray::class.jvmSignature)
    }

    protected fun MethodVisitor.readSafe(klass: Class<*>) {
        val jvmPrimitive = klass.jvmPrimitiveType ?: error("Expected a primitive but was $klass")
        loadThis()                                                     // stack: [this]
        loadData()                                                     // stack: [this, data]
        loadIndex<ReadContext>()                                       // stack: [this, data, index]
        callVirtual(className, "read${jvmPrimitive.name}Safe",
                listOf(ByteArray::class.java, Int::class.java), klass) // stack: [this.read<type>Safe(result, index)]
        increaseIndexBy<ReadContext>(jvmPrimitive.size)                // index += type.size; stack: [this.read<type>Safe(result, index)]
    }

    protected inline fun <reified T> MethodVisitor.readSafe() {
        val jvmPrimitive = T::class.jvmPrimitiveType ?: error("Expected a primitive but was ${T::class}")
        loadThis()                                                                 // stack: [this]
        loadData()                                                                 // stack: [this, data]
        loadIndex<ReadContext>()                                                   // stack: [this, data, index]
        callVirtual2<ByteArray, Int, T>(className, "read${jvmPrimitive.name}Safe") // stack: [this.read<type>Safe(result, index)]
        increaseIndexBy<ReadContext>(jvmPrimitive.size)                            // index += type.size; stack: [this.read<type>Safe(result, index)]
    }

    protected fun MethodVisitor.getTypeCodeSlot() =
            2 + if (klass.occupiesTwoSlots) 1 else 0

    protected fun MethodVisitor.loadTypeCode() {
        loadSlot<Int>(getTypeCodeSlot())
    }

    protected fun MethodVisitor.loadDefault(klass: Class<*>) {
        when (klass.jvmPrimitiveType) {
            JVMPrimitive.Byte,
            JVMPrimitive.Short,
            JVMPrimitive.Int,
            JVMPrimitive.Char,
            JVMPrimitive.Boolean -> visitLdcInsn(0)
            JVMPrimitive.Long -> visitLdcInsn(0L)
            JVMPrimitive.Float -> visitLdcInsn(0.0f)
            JVMPrimitive.Double -> visitLdcInsn(0.0)
            else -> visitInsn(Opcodes.ACONST_NULL)
        }
    }

    protected fun MethodVisitor.assertTypeCode(expectedTypeCode: GroBufTypeCode) {
        loadTypeCode()                                               // stack: [typeCode]
        visitLdcInsn(expectedTypeCode.value)                         // stack: [typeCode, expectedTypeCode]
        val okLabel = Label()
        visitJumpInsn(Opcodes.IF_ICMPEQ, okLabel)                    // if (typeCode == expectedTypeCode) goto ok; stack: []
        loadThis()                                                   // stack: [this]
        loadTypeCode()                                               // stack: [this, typeCode]
        loadContext()                                                // stack: [this, typeCode, context]
        callVirtual2<Int, ReadContext, Void>(className, "skipValue") // this.skipValue(typeCode, context)]
        loadDefault(klass)
        ret(klass)

        visitLabel(okLabel)
    }

    private fun buildReadMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "read",
                listOf(ReadContext::class.java), klass).run {
            readSafe<Byte>()                                    // stack: [this.readByteSafe(context.data, context.index) => typeCode]
            visitInsn(Opcodes.DUP)                              // stack: [typeCode, typeCode]
            saveToSlot<Int>(getTypeCodeSlot())                  // slot_3 = typeCode; stack: [typeCode]
            val notEmptyLabel = Label()
            visitLdcInsn(GroBufTypeCode.Empty.value)            // stack: [typeCode, Empty]
            visitJumpInsn(Opcodes.IF_ICMPNE, notEmptyLabel)     // if (type != Empty) goto notEmpty; stack: []

            loadDefault(klass)
            ret(klass)                                          // return default(type); stack: []

            visitLabel(notEmptyLabel)
            loadThis()                                          // stack: [this]
            loadTypeCode()                                      // stack: [this, typeCode]
            callVirtual1<Int, Void>(className, "checkTypeCode") // this.checkTypeCode(typeCode); stack: []

            readNotNull()
            visitEnd()
        }
        if (klass != Any::class.java) {
            buildBridge(
                    name          = "read",
                    argumentTypes = listOf(
                            ReadContext::class.java.notErased()
                    ),
                    returnType    = klass.erased()
            )
        }
    }

    protected abstract fun MethodVisitor.readNotNull()

    //---------- Bridges building ---------------------------------------------------------//

    private class PossiblyErasedType(val klass: Class<*>, val erased: Boolean)

    private fun Class<*>.erased() = PossiblyErasedType(this, true)

    private fun Class<*>.notErased() = PossiblyErasedType(this, false)

    private val PossiblyErasedType.actualType get() = if (erased) Any::class.java else klass

    private fun buildBridge(name: String, argumentTypes: List<PossiblyErasedType>, returnType: PossiblyErasedType) {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, name,
                argumentTypes.map { it.actualType }, returnType.actualType).run {
            loadThis()
            argumentTypes.forEachIndexed { index, argumentType ->
                loadSlot(argumentType.actualType, index + 1)
                if (argumentType.erased)
                    castObjectTo(argumentType.klass.jvmType)
            }
            callVirtual(className, name, argumentTypes.map { it.klass }, returnType.klass)
            if (returnType.erased)
                castToObject(returnType.klass.jvmType)
            ret(returnType.actualType)
            visitMaxs(argumentTypes.size + 1, argumentTypes.size + 1)
            visitEnd()
        }
    }
}

internal class PrimitivesSerializerBuilder(fragmentSerializerCollection: FragmentSerializerCollection, klass: Class<*>)
    : FragmentSerializerBuilderBase(fragmentSerializerCollection, klass) {

    private val jvmPrimitiveType = klass.jvmPrimitiveType!!

    override fun MethodVisitor.countSizeNotNull() {
        visitLdcInsn(1 /* typeCode */ + jvmPrimitiveType.size)
        ret<Int>()
        visitMaxs(1, 3)
    }

    override fun MethodVisitor.writeNotNull() {
        writeSafe<Byte> { visitLdcInsn(klass.groBufTypeCode.value) } // this.writeByteSafe(result, index, typeCode]; stack: []
        writeSafe(klass) { loadObj() }                               // this.write<type>Safe(result, index, obj); stack: []
        ret<Void>()                                                  // return; stack: []
        visitMaxs(4, 3)
    }

    override fun MethodVisitor.readNotNull() {
        // TODO: Coercion between primitives.
        readSafe(klass) // stack: [this.read<type>Safe(result, index)]
        ret(klass)      // return this.read<type>Safe(result, index); stack: []
        visitMaxs(3, 3)
    }
}

internal class PrimitivesArraySerializerBuilder(fragmentSerializerCollection: FragmentSerializerCollection, klass: Class<*>)
    : FragmentSerializerBuilderBase(fragmentSerializerCollection, klass) {

    private val elementJvmPrimitiveType = klass.componentType.jvmPrimitiveType!!

    override fun MethodVisitor.countSizeNotNull() {
        visitLdcInsn(1 /* typeCode */ + 4 /* length */) // stack: [5]
        loadObj()                                       // stack: [5, obj]
        visitInsn(Opcodes.ARRAYLENGTH)                  // stack: [5, obj.length]
        visitLdcInsn(elementJvmPrimitiveType.size)      // stack: [5, obj.length, elementSize]
        visitInsn(Opcodes.IMUL)                         // stack: [5, obj.length * elementSize]
        visitInsn(Opcodes.IADD)                         // stack: [5 + obj.length * elementSize]
        ret<Int>()                                      // return 5 + obj.length * elementSize; stack: []
        visitMaxs(3, 3)
    }

    override fun MethodVisitor.writeNotNull() {
        writeSafe<Byte> { visitLdcInsn(klass.groBufTypeCode.value) }                     // this.writeByteSafe(result, index, typeCode]; stack: []

        loadObj()                                                                        // stack: [obj]
        visitInsn(Opcodes.ARRAYLENGTH)                                                   // stack: [obj.length]
        visitLdcInsn(elementJvmPrimitiveType.size)                                       // stack: [obj.length, elementSize]
        visitInsn(Opcodes.IMUL)                                                          // stack: [obj.length * elementSize]
        val lengthSlot = 3
        saveToSlot<Int>(lengthSlot)                                                      // length = obj.length * elementSize
        writeSafe<Int> { loadSlot<Int>(lengthSlot) }                                     // this.writeIntSafe(result, index, length]; stack: []

        loadThis()                                                                       // stack: [this]
        loadResult()                                                                     // stack: [this, result]
        loadIndex<WriteContext>()                                                        // stack: [this, result, index]
        loadObj()                                                                        // stack: [this, result, index, obj]
        callVirtual(className, "write${elementJvmPrimitiveType}ArraySafe",
                listOf(ByteArray::class.java, Int::class.java, klass), Void::class.java) // this.writeArray(result, index, obj); stack: []
        increaseIndexBy<WriteContext> { loadSlot<Int>(lengthSlot) }                      // index += length; stack: []

        ret<Void>()
        visitMaxs(4, 4)
    }

    override fun MethodVisitor.readNotNull() {
        assertTypeCode(klass.groBufTypeCode)
        readSafe<Int>()                                                                  // stack: [*(int*)data[index] => length]
        visitInsn(Opcodes.DUP)                                                           // stack: [length, length]
        val lengthSlot = 3
        saveToSlot<Int>(lengthSlot)                                                      // stack: [length]
        visitLdcInsn(elementJvmPrimitiveType.size)                                       // stack: [length, elementSize]
        visitInsn(Opcodes.IDIV)                                                          // stack: [length / elementSize => arrayLength]
        visitIntInsn(Opcodes.NEWARRAY, elementJvmPrimitiveType.typeAsArrayElement)       // stack: [new <elementType>Array[arrayLength] => result]
        val resultSlot = 4
        saveToSlot<Any>(resultSlot)                                                      // stack: []

        loadThis()                                                                       // stack: [this]
        loadSlot<Any>(resultSlot)                                                        // stack: [this, result]
        loadIndex<ReadContext>()                                                         // stack: [this, result, index]
        loadData()                                                                       // stack: [this, result, index, data]
        callVirtual(className, "read${elementJvmPrimitiveType}ArraySafe",
                listOf(klass, Int::class.java, ByteArray::class.java), Void::class.java) // this.readArray(result, index, obj); stack: []
        increaseIndexBy<ReadContext> { loadSlot<Int>(lengthSlot) }                       // index += length; stack: []

        loadSlot<Any>(resultSlot)
        ret(klass)
        visitMaxs(4, 5)
    }
}

internal class ArraySerializerBuilder(fragmentSerializerCollection: FragmentSerializerCollection, klass: Class<*>)
    : FragmentSerializerBuilderBase(fragmentSerializerCollection, klass) {

    private val elementType = klass.componentType!!
    private val elementSerializerType = fragmentSerializerCollection.getFragmentSerializerType(elementType)
    private val elementSerializerField = defineField("elementSerializer", elementSerializerType, null, true)

    override fun MethodVisitor.countSizeNotNull() {
        visitLdcInsn(1 /* typeCode */ + 4 /* data length */ + 4 /* array length */) // stack: [9 => size]
        loadObj()                                                                   // stack: [size, obj]
        visitInsn(Opcodes.ARRAYLENGTH)                                              // stack: [size, obj.length]
        visitInsn(Opcodes.DUP)                                                      // stack: [size, obj.length, obj.length]
        val lengthSlot = 3
        saveToSlot<Int>(lengthSlot)                                                 // length = obj.length; stack: [size, length]
        val doneLabel = Label()
        visitJumpInsn(Opcodes.IFEQ, doneLabel)                                      // if (length == 0) goto done; stack: [size]

        val indexSlot = 4
        visitLdcInsn(0)                                                             // stack: [size, 0]
        saveToSlot<Int>(indexSlot)                                                  // index = 0; stack: [size]
        val loopStartLabel = Label()
        visitLabel(loopStartLabel)
        loadField(elementSerializerField)                                           // stack: [size, elementSerializer]
        loadContext()                                                               // stack: [size, elementSerializer, context]
        loadObj()                                                                   // stack: [size, elementSerializer, context, obj]
        loadSlot<Int>(indexSlot)                                                    // stack: [size, elementSerializer, context, obj, index]
        visitInsn(Opcodes.AALOAD)                                                   // stack: [size, elementSerializer, context, obj[index]]
        callVirtual(elementSerializerType, "countSize",
                listOf(WriteContext::class.java, elementType), Int::class.java)     // stack: [size, elementSerializer.countSize(context, obj[index])]
        visitInsn(Opcodes.IADD)                                                     // stack: [size + elementSerializer.countSize(context, obj[index]) => size]

        loadSlot<Int>(indexSlot)                                                    // stack: [size, index]
        visitLdcInsn(1)                                                             // stack: [size, index, 1]
        visitInsn(Opcodes.IADD)                                                     // stack: [size, index + 1]
        visitInsn(Opcodes.DUP)                                                      // stack: [size, index + 1, index + 1]
        saveToSlot<Int>(indexSlot)                                                  // index = index + 1; stack: [size, index]
        loadSlot<Int>(lengthSlot)                                                   // stack: [size, index, length]
        visitJumpInsn(Opcodes.IF_ICMPLT, loopStartLabel)                            // if (index < length) goto loopStart; stack: [size]

        visitLabel(doneLabel)
        ret<Int>()                                                                  // return size; stack: []
        visitMaxs(5, 5)
    }

    override fun MethodVisitor.writeNotNull() {
        writeSafe<Byte> { visitLdcInsn(GroBufTypeCode.Array.value) }

        val startSlot = 3
        loadIndex<WriteContext>()                                                   // stack: [context.index]
        saveToSlot<Int>(startSlot)                                                  // start = context.index; stack: []
        increaseIndexBy<WriteContext>(4)                                            // context.index += 4; stack: []

        loadObj()                                                                   // stack: [obj]
        visitInsn(Opcodes.ARRAYLENGTH)                                              // stack: [obj.length]
        visitInsn(Opcodes.DUP)                                                      // stack: [obj.length, obj.length]
        val lengthSlot = 4
        saveToSlot<Int>(lengthSlot)                                                 // length = obj.length; stack: [length]
        writeSafe<Int> { loadSlot<Int>(lengthSlot) }                                // writeIntSafe(length); stack: [length]
        val doneLabel = Label()
        visitJumpInsn(Opcodes.IFEQ, doneLabel)                                      // if (length == 0) goto done; stack: []

        val indexSlot = 5
        visitLdcInsn(0)                                                             // stack: [0]
        saveToSlot<Int>(indexSlot)                                                  // index = 0; stack: []
        val loopStartLabel = Label()
        visitLabel(loopStartLabel)
        loadField(elementSerializerField)                                           // stack: [elementSerializer]
        loadContext()                                                               // stack: [elementSerializer, context]
        loadObj()                                                                   // stack: [elementSerializer, context, obj]
        loadSlot<Int>(indexSlot)                                                    // stack: [elementSerializer, context, obj, index]
        visitInsn(Opcodes.AALOAD)                                                   // stack: [elementSerializer, context, obj[index]]
        callVirtual(elementSerializerType, "write",
                listOf(WriteContext::class.java, elementType), Void::class.java)    // elementSerializer.write(context, obj[index]); stack: []

        loadSlot<Int>(indexSlot)                                                    // stack: [index]
        visitLdcInsn(1)                                                             // stack: [index, 1]
        visitInsn(Opcodes.IADD)                                                     // stack: [index + 1]
        visitInsn(Opcodes.DUP)                                                      // stack: [index + 1, index + 1]
        saveToSlot<Int>(indexSlot)                                                  // index = index + 1; stack: [index]
        loadSlot<Int>(lengthSlot)                                                   // stack: [index, length]
        visitJumpInsn(Opcodes.IF_ICMPLT, loopStartLabel)                            // if (index < length) goto loopStart; stack: []

        visitLabel(doneLabel)
        writeLength(startSlot)
        ret<Void>()
        visitMaxs(6, 6)
    }

    override fun MethodVisitor.readNotNull() {
        assertTypeCode(GroBufTypeCode.Array)

        readSafe<Int>()                                         // stack: [data length]
        loadIndex<ReadContext>()                                // stack: [data length, context.index]
        val endSlot = 3
        visitInsn(Opcodes.IADD)                                 // stack: [data length + context.index]
        saveToSlot<Int>(endSlot)                                // end = context.index + data length; stack: []
        readSafe<Int>()                                         // stack: [length]
        val lengthSlot = 4
        visitInsn(Opcodes.DUP)                                  // stack: [length, length]
        saveToSlot<Int>(lengthSlot)                             // stack: [length]
        visitTypeInsn(Opcodes.ANEWARRAY, elementType.jvmType)   // stack: [new elementType[length] => result]
        loadSlot<Int>(lengthSlot)
        val doneLabel = Label()
        visitJumpInsn(Opcodes.IFEQ, doneLabel)                  // if (length == 0) goto done; stack: [result]

        val indexSlot = 5
        visitLdcInsn(0)                                         // stack: [result, 0]
        saveToSlot<Int>(indexSlot)                              // index = 0; stack: [result]
        val loopStartLabel = Label()
        visitLabel(loopStartLabel)
        visitInsn(Opcodes.DUP)                                  // stack: [result, result]
        loadSlot<Int>(indexSlot)                                // stack: [result, result, index]
        loadField(elementSerializerField)                       // stack: [result, result, index, elementSerializer]
        loadContext()                                           // stack: [result, result, index, elementSerializer, context]
        callVirtual(elementSerializerType, "read",
                listOf(ReadContext::class.java), elementType)   // stack: [result, result, index, elementSerializer.read(context)]
        visitInsn(Opcodes.AASTORE)                              // result[index] = elementSerializer.read(context); stack: [result]

        loadSlot<Int>(indexSlot)                                // stack: [result, index]
        visitLdcInsn(1)                                         // stack: [result, index, 1]
        visitInsn(Opcodes.IADD)                                 // stack: [result, index + 1]
        visitInsn(Opcodes.DUP)                                  // stack: [result, index + 1, index + 1]
        saveToSlot<Int>(indexSlot)                              // index = index + 1; stack: [result, index]
        loadSlot<Int>(lengthSlot)                               // stack: [result, index, length]
        visitJumpInsn(Opcodes.IF_ICMPLT, loopStartLabel)        // if (index < length) goto loopStart; stack: [result]

        visitLabel(doneLabel)
        loadSlot<Int>(endSlot)                                  // stack: [result, end]
        loadIndex<ReadContext>()                                // stack: [result, end, context.index]
        val badDataLabel = Label()
        visitJumpInsn(Opcodes.IF_ICMPNE, badDataLabel)          // if (end != context.index) goto badData; stack: [result]
        ret(klass)

        visitLabel(badDataLabel)
        loadThis()
        callVirtual0<Void>(className, "throwBadDataLengthError")
        ret(klass)

        visitMaxs(5, 6)
    }
}

internal class ClassSerializerBuilder(fragmentSerializerCollection: FragmentSerializerCollection, klass: Class<*>)
    : FragmentSerializerBuilderBase(fragmentSerializerCollection, klass) {

    private val klassField by lazy { defineField("klass", klass::class.jvmType, klass) }

    override fun MethodVisitor.countSizeNotNull() {
        visitLdcInsn(1 /* typeCode */ + 4 /* length */)                         // stack: [5 => size]
        klass.appropriateFields.forEach {
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(it.type)
            val fieldVisitedLabel = Label()
            val fieldSlot = 3
            if (it.type.isReference) {
                loadObj()                                                       // stack: [size, obj]
                visitFieldInsn(Opcodes.GETFIELD, klass.jvmType,
                        it.name, it.type.jvmSignature)                          // stack: [size, obj.field]
                visitInsn(Opcodes.DUP)                                          // stack: [size, obj.field, obj.field]
                saveToSlot<Any>(fieldSlot)                                      // field = obj.field; stack: [size, obj.field]
                visitJumpInsn(Opcodes.IFNULL, fieldVisitedLabel)                // if (obj.field == null) goto fieldVisited; stack: [size]
            }
            loadField(getSerializerField(fieldSerializerType))                  // stack: [size, fieldSerializer]
            loadContext()                                                       // stack: [size, fieldSerializer, context]
            if (it.type.isReference)
                loadSlot<Any>(fieldSlot)                                        // stack: [size, fieldSerializer, context, obj.field]
            else {
                loadObj()                                                       // stack: [size, fieldSerializer, context, obj]
                visitFieldInsn(Opcodes.GETFIELD, klass.jvmType,
                        it.name, it.type.jvmSignature)                          // stack: [size, fieldSerializer, context, obj.field]
            }
            callVirtual(fieldSerializerType, "countSize",
                    listOf(WriteContext::class.java, it.type), Int::class.java) // stack: [size, fieldSerializer.countSize(context, obj.field)]
            visitInsn(Opcodes.IADD)                                             // stack: [size + fieldSerializer.countSize(context, obj.field) => size]
            visitLdcInsn(8)                                                     // stack: [size, 8]
            visitInsn(Opcodes.IADD)                                             // stack: [size + 8 => size]
            visitLabel(fieldVisitedLabel)
        }
        ret<Int>()                                                              // return size; stack: []
        visitMaxs(4, 4)
    }

    override fun MethodVisitor.writeNotNull() {
        writeSafe<Byte> { visitLdcInsn(GroBufTypeCode.Object.value) }            // this.writeByteSafe(result, index, Object]; stack: []
        loadIndex<WriteContext>()                                                // stack: [index]
        val startSlot = 3
        saveToSlot<Int>(startSlot)                                               // start = index; stack: []
        increaseIndexBy<WriteContext>(4)                                         // index += 4; stack: []
        klass.appropriateFields.forEach {
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(it.type)
            val fieldVisitedLabel = Label()
            val fieldSlot = 4
            if (it.type.isReference) {
                loadObj()                                                        // stack: [obj]
                visitFieldInsn(Opcodes.GETFIELD, klass.jvmType,
                        it.name, it.type.jvmSignature)                           // stack: [obj.field]
                visitInsn(Opcodes.DUP)                                           // stack: [obj.field, obj.field]
                saveToSlot<Any>(fieldSlot)                                       // field = obj.field; stack: [obj.field]
                visitJumpInsn(Opcodes.IFNULL, fieldVisitedLabel)                 // if (obj.field == null) goto fieldVisited; stack: []
            }
            writeSafe<Long> { visitLdcInsn(HashCalculator.calcHash(it.name)) }   // writeLongSafe(calcHash(fieldName)); stack: []
            loadField(getSerializerField(fieldSerializerType))                   // stack: [fieldSerializer]
            loadContext()                                                        // stack: [fieldSerializer, context]
            if (it.type.isReference)
                loadSlot<Any>(fieldSlot)                                         // stack: [fieldSerializer, context, obj.field]
            else {
                loadObj()                                                        // stack: [fieldSerializer, context, obj]
                visitFieldInsn(Opcodes.GETFIELD, klass.jvmType,
                        it.name, it.type.jvmSignature)                           // stack: [fieldSerializer, context, obj.field]
            }
            callVirtual(fieldSerializerType, "write",
                    listOf(WriteContext::class.java, it.type), Void::class.java) // fieldSerializer.write(context, obj.field); stack: []
            visitLabel(fieldVisitedLabel)
        }

        writeLength(startSlot)

        ret<Void>()
        visitMaxs(6, 5)
    }

    override fun MethodVisitor.readNotNull() {
        val fields = klass.appropriateFields
                .map { it to HashCalculator.calcHash(it.name) }
                .sortedBy { it.second }
        assertTypeCode(GroBufTypeCode.Object)

        val parameterlessConstructor = klass.constructors.firstOrNull { it.parameterCount == 0 }
        if (parameterlessConstructor != null) {
            visitTypeInsn(Opcodes.NEW, klass.jvmType)                // stack: [new klass() => result]
            visitInsn(Opcodes.DUP)                                   // stack: [result, result]
            ctorCall0(klass.jvmType)                                 // inst.<init>(); stack: [result]
        } else {
            loadThis()                                               // stack: [this]
            loadField(klassField)                                    // stack: [this, klass]
            callVirtual1<Class<*>, Any>(className, "createInstance") // stack: [this.createInstance(klass)]
            castObjectTo(klass.jvmType)                              // stack: [(klass)this.createInstance(klass) => result]
        }

        readSafe<Int>()                                              // stack: [result, length]
        visitInsn(Opcodes.DUP)                                       // stack: [result, length, length]
        val emptyLabel = Label()
        visitJumpInsn(Opcodes.IFEQ, emptyLabel)                      // if (length == 0) goto empty; stack: [result, length]
        loadIndex<ReadContext>()                                     // stack: [result, length, index]
        visitInsn(Opcodes.IADD)                                      // stack: [result, length + index]
        val endSlot = 1 + getTypeCodeSlot()
        saveToSlot<Int>(endSlot)                                     // end = length + index; stack: [result]

        val loopStartLabel = Label()
        val loopEndLabel = Label()
        visitLabel(loopStartLabel)
        readSafe<Long>()                                             // stack: [*(long*)data[index]]
        val hashCodeSlot = 2 + getTypeCodeSlot()
        saveToSlot<Long>(hashCodeSlot)                               // hashCode = *(long*)data[index]; stack: []
        val defaultLabel = Label()
        genSwitch(fields.map { it.second }, hashCodeSlot, defaultLabel) {
            val field = fields[it].first
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(field.type)
            visitInsn(Opcodes.DUP)                                   // stack: [result, result]
            loadField(getSerializerField(fieldSerializerType))       // stack: [result, result, fieldSerializer]
            loadContext()                                            // stack: [result, result, fieldSerializer, context]
            callVirtual(fieldSerializerType, "read",
                    listOf(ReadContext::class.java), field.type)     // stack: [result, result, fieldSerializer.read(context)]
            visitFieldInsn(Opcodes.PUTFIELD, klass.jvmType,
                    field.name, field.type.jvmSignature)             // result.field = fieldSerializer.read(context); stack: [result]
            visitJumpInsn(Opcodes.GOTO, loopEndLabel)
        }

        visitLabel(defaultLabel)
        loadThis()                                                   // stack: [result, this]
        readSafe<Byte>()                                             // stack: [result, this, *(byte*)data[index] => fieldTypeCode]
        loadContext()                                                // stack: [result, this, fieldTypeCode, context]
        callVirtual2<Int, ReadContext, Void>(className, "skipValue") // this.skipValue(fieldTypeCode, context); stack: [result]

        visitLabel(loopEndLabel)
        loadIndex<ReadContext>()                                     // stack: [result, index]
        loadSlot<Int>(endSlot)                                       // stack: [result, index, end]
        visitJumpInsn(Opcodes.IF_ICMPLT, loopStartLabel)             // if (index < end) goto loopStart; stack: [result]

        loadSlot<Int>(endSlot)                                       // stack: [result, end]
        loadIndex<ReadContext>()                                     // stack: [result, end, context.index]
        val badDataLabel = Label()
        visitJumpInsn(Opcodes.IF_ICMPNE, badDataLabel)               // if (end != context.index) goto badData; stack: [result]
        ret(klass)

        visitLabel(badDataLabel)
        loadThis()
        callVirtual0<Void>(className, "throwBadDataLengthError")
        ret(klass)

        visitLabel(emptyLabel)                                       // stack: [result, 0]
        visitInsn(Opcodes.POP)                                       // stack: [result]
        ret(klass)                                                   // return result; stack: []
        visitMaxs(4, 3 + getTypeCodeSlot())
    }

    private val Class<*>.appropriateFields: List<java.lang.reflect.Field>
        get() = // TODO: DataMembersExtractor
            fields.filter { Modifier.isPublic(it.modifiers) }
                  .filterNot { Modifier.isStatic(it.modifiers) }
                  .filterNot { Modifier.isFinal(it.modifiers) }

    private fun getSerializerField(type: String) = serializerFields.getOrPut(type) {
        defineField("${type.toJVMIdentifier()}_serializer", type.toJVMType(), null, true)
    }

    private val serializerFields = mutableMapOf<String, Int>()
}

fun zzz1(x: Byte) : Any { return x }
fun zzz2(x: Short) : Any { return x }
fun zzz3(x: Char) : Any { return x }
fun zzz4(x: Int) : Any { return x }
fun zzz5(x: Boolean) : Any { return x }
fun zzz6(x: Long) : Any { return x }
fun zzz7(x: Float) : Any { return x }
fun zzz8(x: Double) : Any { return x }

data class A(/*@JvmField val b: B?, */@JvmField var x: Int, @JvmField var y: Int, @JvmField var s: String?)

class B(@JvmField var a: A, @JvmField var x: Int)

class B2(@JvmField var a: A?, @JvmField var x: Int)

fun countSize(x: Any?, context: WriteContext) {

}

class Qzz(@JvmField val x: Int, @JvmField val y: java.lang.Integer)

fun zzz(klass: KClass<*>) {
    println(klass)
}

inline fun <reified T> zzz() {
    zzz(T::class)
}

fun main(args: Array<String>) {
    Qzz::class.java.fields.forEach {
        val clazz = it.type
        println(clazz)
        val kClass = clazz.kotlin
        println(kClass)
        println(kClass.java)
    }
    zzz(Int::class)
    zzz<Int>()
    val fragmentSerializerCollection = FragmentSerializerCollection()
    val serializer = fragmentSerializerCollection.getFragmentSerializer<B>()
    val context = WriteContext()
    val obj = B(A(42, 117, "zzz"), -1)
    val size = serializer.countSize(context, obj)
    println(size)
    val arr = ByteArray(size)
    context.index = 0
    context.result = arr
    serializer.write(context, obj)
    println(arr.contentToString())
    val serializer2 = fragmentSerializerCollection.getFragmentSerializer<B2>()
    val readB2 = serializer2.read(ReadContext().also { it.data = arr; it.index = 0 })
    println(readB2.x)
    println(readB2.a!!.x)
    println(readB2.a!!.y)
    println(readB2.a!!.s)
    val serializer3 = fragmentSerializerCollection.getFragmentSerializer<IntArray>()
    val size3 = serializer3.countSize(context, intArrayOf(1, 2, 3))
    val arr3 = ByteArray(size3)
    context.index = 0
    context.result = arr3
    serializer3.write(context, intArrayOf(1, 2, 3))
    println(arr3.contentToString())
    val readArr = serializer3.read(ReadContext().also { it.data = arr3; it.index = 0 })
    println(readArr.contentToString())

    val serializer4 = fragmentSerializerCollection.getFragmentSerializer<Array<A>>()
    val size4 = serializer4.countSize(context, arrayOf(A(42, 117, "zzz"), A(-1, 314, "qxx")))
    val arr4 = ByteArray(size4)
    context.index = 0
    context.result = arr4
    serializer4.write(context, arrayOf(A(42, 117, "zzz"), A(-1, 314, "qxx")))
    println(arr4.contentToString())
    val readArr4 = serializer4.read(ReadContext().also { it.data = arr4; it.index = 0 })
    println(readArr4.contentToString())

    //test3()
}

class Z(val x: Int)

fun test(arr: Array<Any?>) {
    val x = arr[0] as Int

    println(x)
}

fun test1() {
    val size = 1024 * 1024 * 10
    val buf = ByteBuffer.allocateDirect(size)
    for (j in 0..10000) {
        val time = System.nanoTime()

        for (i in 0..size - 1 step 4)
            buf.putInt(i, i)
        val elapsed = (System.nanoTime() - time).toDouble() / 1000
        //println(buf[7])
        println(elapsed)
    }
}

fun test2() {
    val theUnsafe: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
    theUnsafe.isAccessible = true
    val unsafe = theUnsafe.get(null) as Unsafe
    val size = 1024 * 1024 * 10
    val startIndex = unsafe.allocateMemory(size.toLong())
    for (i in 0..size - 1 step 4)
        unsafe.putInt(startIndex + i, i)
//    val arr = ByteArray(size)
//    val zzz = unsafe.arrayBaseOffset(arr::class.java)
//    unsafe.copyMemory(null, startIndex, arr, zzz.toLong(), size.toLong())
//    for (i in 0..10)
//        println(arr[i])
    for (j in 0..10000) {
        val time = System.nanoTime()

        for (i in 0..size - 1 step 4)
            unsafe.putInt(startIndex + i, i)
        val elapsed = (System.nanoTime() - time).toDouble() / 1000
        //println(buf[7])
        println(elapsed)
    }
}

internal class StringSerializer: FragmentSerializer<String?>() {

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
}

fun test3() {
//    val arr = ByteArray(size)
//    val zzz = unsafe.arrayBaseOffset(arr::class.java)
//    unsafe.copyMemory(null, startIndex, arr, zzz.toLong(), size.toLong())
//    for (i in 0..10)
//        println(arr[i])
    val str = "as;ldkfjalsdkjf;laskjdfsldfjkals;dkjfla;ksjdflasjdf;lkjasdf;ljasdf;lkjasd;lfjkasdf"
    val serializer = StringSerializer()
    val writeContext = WriteContext()
    val size = serializer.countSize(writeContext, str)
    writeContext.result = kotlin.ByteArray(size)
    serializer.write(writeContext, str)
    val readContext = ReadContext().also { it.data = writeContext.result }
    println(serializer.read(readContext))
    for (j in 0..10000) {
        val time = System.nanoTime()
        for (i in 0..10000) {
            readContext.index = 0
            serializer.read(readContext)
        }
        val elapsed = (System.nanoTime() - time).toDouble() / 1000
        //println(buf[7])
        println(elapsed)
    }
}