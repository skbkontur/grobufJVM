package grobuf

import org.objectweb.asm.*

import sun.misc.Unsafe
import java.io.FileOutputStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.ByteBuffer

interface Serializer {
    fun <T> getSize(obj: T): Int
    fun <T> serialize(obj: T): ByteArray
    fun <T: Any> deserialize(klass: Class<T>, data: ByteArray): T
}

open class SerializerImpl: Serializer {
    override fun <T> getSize(obj: T): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T> serialize(obj: T): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> deserialize(klass: Class<T>, data: ByteArray): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

internal class GroBufWriter {
    init {
        val buf = ByteBuffer.allocateDirect(1000)

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

internal abstract class FragmentSerializer<T> {
    private val unsafe: Unsafe
    private val byteArrayDataOffset: Int
    init {
        val theUnsafe: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
        theUnsafe.isAccessible = true
        unsafe = theUnsafe.get(null) as Unsafe
        byteArrayDataOffset = unsafe.arrayBaseOffset(ByteArray::class.java)
    }

    abstract fun countSize(obj: T, context: WriteContext): Int
    abstract fun write(obj: T, context: WriteContext)
    abstract fun read(prev: T, context: ReadContext): T

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

    protected fun ensureSize(array: ByteArray, offset: Int, size: Int) {
        if (offset + size > array.size)
            throw Error("Unexpected end of data") // TODO Create DataCorruptedException.
    }

    protected fun createInstance(klass: Class<*>): Any {
        return unsafe.allocateInstance(klass)
    }
}

private object DynamicClassesLoader : ClassLoader() {
    fun loadClass(name: String, byteCode: ByteArray): Class<*> {
        return defineClass(name, byteCode, 0, byteCode.size)
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
        loadArg<Any>(0)
    }

    protected fun MethodVisitor.loadField(fieldId: Int) {
        val field = fields[fieldId]
        loadThis()
        visitFieldInsn(Opcodes.GETFIELD, className, field.name, field.type.toJVMSignature())
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
            loadArg<Array<Any?>>(1)                          // stack: [this, args]
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
            loadArg<Array<Any?>>(0)               // stack: [inst, inst, args]
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
    private val serializers = mutableMapOf<String, BuildState>()
    private val serializerToTypeMap = mutableMapOf<String, String>()

    init {
        serializers.forEach { type, writer ->
            serializerToTypeMap += (writer as BuildState.Initialized).inst::class.java.jvmType to type
        }
    }

    fun getFragmentSerializerType(klass: Class<*>): String {
        val canonicalName = klass.jvmType
        val buildState = serializers[canonicalName]
        return when (buildState) {
            null -> getFragmentSerializer(klass)::class.java.jvmType
            is BuildState.Building -> buildState.className
            is BuildState.Built -> buildState.inst::class.java.jvmType
            is BuildState.Initialized -> buildState.inst::class.java.jvmType
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

    private fun getSerializerBuilder(klass: Class<*>): FragmentSerializerBuilderBase {
        if (klass.jvmPrimitiveType != null)
            return PrimitivesSerializerBuilder(this, klass)
        return ClassSerializerBuilder(this, klass)
    }

    private fun initialize(type: String) {
        dfs(type, mutableSetOf())
    }

    private fun dfs(type: String, visited: MutableSet<String>) {
        visited.add(type)
        val state = serializers[type]
        when (state) {
            null -> throw IllegalStateException("Writer for $type has not been created")
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
                            null -> throw IllegalStateException("Writer for $it has not been created")
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
        className      = "${klass.canonicalName.toJVMIdentifier()}_Serializer",
        superClassType = "grobuf/FragmentSerializer") {

    override fun buildBody() {
        buildCountMethod()
        buildWriteMethod()
        buildReadMethod()
    }

    protected fun MethodVisitor.loadObj() {
        loadArg(klass, 1)
    }

    protected fun MethodVisitor.loadContext() {
        loadArg<Any>(2)
    }

    protected fun MethodVisitor.loadResult() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, WriteContext::class.java.jvmType, "result", ByteArray::class.java.jvmSignature)
    }

    protected fun MethodVisitor.loadData() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, ReadContext::class.java.jvmType, "data", ByteArray::class.java.jvmSignature)
    }

    protected fun MethodVisitor.loadIndex(contextType: Class<*>) {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, contextType.jvmType, "index", Int::class.java.jvmSignature)
    }

    protected fun MethodVisitor.increaseIndexBy(contextType: Class<*>, value: Int) {
        loadContext()
        loadIndex(contextType)
        visitLdcInsn(value)
        visitInsn(Opcodes.IADD)
        visitFieldInsn(Opcodes.PUTFIELD, contextType.jvmType, "index", Int::class.java.jvmSignature)
    }

    private fun buildCountMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "countSize",
                listOf(klass, WriteContext::class.java), Int::class.java).run {
            countSizeNotNull()
            visitEnd()
        }
        if (klass != Any::class.java) {
            buildBridge("countSize",
                    listOf(
                            klass.erased(),
                            WriteContext::class.java.notErased()
                    ),
                    Int::class.java.notErased()
            )
        }
    }

    protected abstract fun MethodVisitor.countSizeNotNull()

    private fun buildWriteMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "write",
                        listOf(klass, WriteContext::class.java), Void::class.java).run {
            writeNotNull()
            visitEnd()
        }
        if (klass != Any::class.java) {
            buildBridge("write",
                    listOf(
                            klass.erased(),
                            WriteContext::class.java.notErased()
                    ),
                    Void::class.java.notErased()
            )
        }
    }

    protected abstract fun MethodVisitor.writeNotNull()

    private fun buildReadMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "read",
                listOf(klass, ReadContext::class.java), klass).run {
            readNotNull()
            visitEnd()
        }
        if (klass != Any::class.java) {
            buildBridge("read",
                    listOf(
                            klass.erased(),
                            ReadContext::class.java.notErased()
                    ),
                    klass.erased()
            )
        }
    }

    protected abstract fun MethodVisitor.readNotNull()

    private class PossiblyErasedType(val klass: Class<*>, val erased: Boolean)

    private fun Class<*>.erased() = PossiblyErasedType(this, true)

    private fun Class<*>.notErased() = PossiblyErasedType(this, false)

    private val PossiblyErasedType.actualType get() = if (erased) Any::class.java else klass

    private fun buildBridge(name: String, argumentTypes: List<PossiblyErasedType>, returnType: PossiblyErasedType) {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, name,
                argumentTypes.map { it.actualType }, returnType.actualType).run {
            loadThis()
            argumentTypes.forEachIndexed { index, argumentType ->
                loadArg(argumentType.actualType, index + 1)
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
        visitLdcInsn(jvmPrimitiveType.size)
        ret<Int>()
        visitMaxs(1, 3)
    }

    override fun MethodVisitor.writeNotNull() {
        loadThis()                                                                       // stack: [this]
        loadResult()                                                                     // stack: [this, result]
        loadIndex(WriteContext::class.java)                                              // stack: [this, result, index]
        loadObj()                                                                        // stack: [this, result, index, obj]
        callVirtual(className, "write${jvmPrimitiveType.name}Safe",
                listOf(ByteArray::class.java, Int::class.java, klass), Void::class.java) // this.write<type>Safe(result, index, obj); stack: []
        increaseIndexBy(WriteContext::class.java, jvmPrimitiveType.size)                 // index += type.size; stack: []
        ret<Void>()                                                                      // return; stack: []
        visitMaxs(4, 3)
    }

    override fun MethodVisitor.readNotNull() {
        loadThis()                                                      // stack: [this]
        loadData()                                                      // stack: [this, data]
        loadIndex(ReadContext::class.java)                              // stack: [this, data, index]
        callVirtual(className, "read${jvmPrimitiveType.name}Safe",
                listOf(ByteArray::class.java, Int::class.java), klass)  // stack: [this.read<type>Safe(result, index)]
        increaseIndexBy(ReadContext::class.java, jvmPrimitiveType.size) // index += type.size; stack: [this.read<type>Safe(result, index)]
        ret(klass)                                                      // return this.read<type>Safe(result, index); stack: []
        visitMaxs(3, 3)
    }
}

internal class ClassSerializerBuilder(fragmentSerializerCollection: FragmentSerializerCollection, klass: Class<*>)
    : FragmentSerializerBuilderBase(fragmentSerializerCollection, klass) {

    override fun MethodVisitor.countSizeNotNull() {
        visitLdcInsn(0)                                                         // stack: [0 => size]
        klass.forEachAppropriateField {
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(it.type)
            loadField(getSerializerField(fieldSerializerType))                  // stack: [size, fieldSerializer]
            loadObj()                                                           // stack: [size, fieldSerializer, obj]
            visitFieldInsn(Opcodes.GETFIELD, klass.jvmType,
                    it.name, it.type.jvmSignature)                              // stack: [size, fieldSerializer, obj.field]
            loadContext()                                                       // stack: [size, fieldSerializer, obj.field, context]
            callVirtual(fieldSerializerType, "countSize",
                    listOf(it.type, WriteContext::class.java), Int::class.java) // stack: [size, fieldSerializer.countSize(obj.field, context)]
            visitInsn(Opcodes.IADD)                                             // stack: [size + fieldSerializer.countSize(obj.field, context)]
        }
        ret<Int>()                                                              // return size; stack: []
        visitMaxs(4, 3)
    }

    override fun MethodVisitor.writeNotNull() {
        klass.forEachAppropriateField {
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(it.type)
            loadField(getSerializerField(fieldSerializerType))                   // stack: [fieldSerializer]
            loadObj()                                                            // stack: [fieldSerializer, obj]
            visitFieldInsn(Opcodes.GETFIELD, klass.jvmType,
                    it.name, it.type.jvmSignature)                               // stack: [fieldSerializer, obj.field]
            loadContext()                                                        // stack: [fieldSerializer, obj.field, context]
            callVirtual(fieldSerializerType, "write",
                    listOf(it.type, WriteContext::class.java), Void::class.java) // stack: [fieldSerializer.write(obj.field, context)]
        }
        ret<Void>()
        visitMaxs(3, 3)
    }

    override fun MethodVisitor.readNotNull() {
        klass.forEachAppropriateField {
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(it.type)
            loadObj()                                                        // stack: [obj]
            loadField(getSerializerField(fieldSerializerType))               // stack: [obj, fieldSerializer]
            loadObj()                                                        // stack: [obj, fieldSerializer, obj]
            visitFieldInsn(Opcodes.GETFIELD, klass.jvmType,
                    it.name, it.type.jvmSignature)                           // stack: [obj, fieldSerializer, obj.field]
            if (it.type.jvmPrimitiveType == null) {
                visitInsn(Opcodes.DUP)                                       // stack: [obj, fieldSerializer, obj.field, obj.field]
                val notNullLabel = Label()
                visitJumpInsn(Opcodes.IFNONNULL, notNullLabel)               // if (obj.field != null) goto notNull; stack: [obj, fieldSerializer, obj.field]
                visitInsn(Opcodes.POP)                                       // stack: [obj, fieldSerializer]
                val parameterlessConstructor = it.type.constructors.firstOrNull { it.parameterCount == 0 }
                if (parameterlessConstructor != null) {
                    visitTypeInsn(Opcodes.NEW, it.type.jvmType)              // stack: [obj, fieldSerializer, new fieldType() => inst]
                    visitInsn(Opcodes.DUP)                                   // stack: [obj, fieldSerializer, inst, inst]
                    ctorCall0(it.type.jvmType)                               // inst.<init>(); stack: [obj, fieldSerializer, inst]
                } else {
                    loadThis()                                               // stack: [obj, fieldSerializer, this]
                    loadField(getFieldTypeField(it.type))                    // stack: [obj, fieldSerializer, this, this.fieldType]
                    callVirtual1<Class<*>, Any>(className, "createInstance") // stack: [obj, fieldSerializer, this.createInstance(this.fieldType)]
                    castObjectTo(it.type.jvmType)                            // stack: [obj, fieldSerializer, (fieldType)this.createInstance(this.fieldType)]
                }
                visitLabel(notNullLabel)
                //visitFrame(Opcodes.F_NEW, 3, arrayOf(className, klass.jvmType, ReadContext::class.java.jvmType), 3, arrayOf(klass.jvmType, fieldSerializerType, it.type.jvmType))
            }
            loadContext()                                                    // stack: [obj, fieldSerializer, obj.field, context]
            callVirtual(fieldSerializerType, "read",
                    listOf(it.type, ReadContext::class.java), it.type)       // stack: [obj, fieldSerializer.read(obj.field, context)]
            visitFieldInsn(Opcodes.PUTFIELD, klass.jvmType,
                    it.name, it.type.jvmSignature)                           // obj.field = fieldSerializer.read(obj.field, context); stack: []
        }
        loadObj()                                                            // stack: [obj]
        ret(klass)                                                           // return obj; stack: []
        visitMaxs(4, 3)
    }

    private fun Class<*>.forEachAppropriateField(block: (java.lang.reflect.Field) -> Unit) {
        // TODO: DataMembersExtractor
        fields.filter { Modifier.isPublic(it.modifiers) }
              .filterNot { Modifier.isStatic(it.modifiers) }
              .forEach(block)
    }

    private fun getSerializerField(type: String) = serializerFields.getOrPut(type) {
        defineField("${type.toJVMIdentifier()}_serializer", type.toJVMType(), null, true)
    }

    private fun getFieldTypeField(type: Class<*>) = fieldTypesFields.getOrPut(type) {
        defineField("${type.jvmType.toJVMIdentifier()}_fieldType", type::class.java.jvmType, type)
    }

    private val serializerFields = mutableMapOf<String, Int>()
    private val fieldTypesFields = mutableMapOf<Class<*>, Int>()
}

fun zzz1(x: Byte) : Any { return x }
fun zzz2(x: Short) : Any { return x }
fun zzz3(x: Char) : Any { return x }
fun zzz4(x: Int) : Any { return x }
fun zzz5(x: Boolean) : Any { return x }
fun zzz6(x: Long) : Any { return x }
fun zzz7(x: Float) : Any { return x }
fun zzz8(x: Double) : Any { return x }

class A(/*@JvmField val b: B?, */@JvmField var x: Int, @JvmField var y: Int)

class B(@JvmField var a: A, @JvmField var x: Int)

class B2(@JvmField var a: A?, @JvmField var x: Int)

fun countSize(x: Any?, context: WriteContext) {

}

fun main(args: Array<String>) {
    val fragmentSerializerCollection = FragmentSerializerCollection()
    val serializer = fragmentSerializerCollection.getFragmentSerializer<B>()
    val context = WriteContext()
    val obj = B(A(42, 117), -1)
    val size = serializer.countSize(obj, context)
    println(size)
    val arr = ByteArray(size)
    context.index = 0
    context.result = arr
    serializer.write(obj, context)
    println(arr.contentToString())
    val serializer2 = fragmentSerializerCollection.getFragmentSerializer<B2>()
    val b2 = B2(null, 1000)
    val readB2 = serializer2.read(b2, ReadContext().also { it.data = arr; it.index = 0 })
    println(readB2.x)
    println(readB2.a!!.x)
    println(readB2.a!!.y)
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