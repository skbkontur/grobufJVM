package grobuf.serializers

import grobuf.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal abstract class FragmentSerializerBuilderBase(classLoader: DynamicClassesLoader,
                                                      protected val fragmentSerializerCollection: FragmentSerializerCollection,
                                                      protected val klass: Class<*>)
    : ClassBuilder<FragmentSerializer<*>>(
        classLoader    = classLoader,
        classType      = JVMType.FromString("${klass.jvmType.name.toJVMIdentifier()}_Serializer"),
        superClassType = FragmentSerializer::class.jvmType) {

    class Local(val type: JVMType, val slot: Int) {
        val size = if (type.occupiesTwoSlots) 2 else 1

        val nextSlot = slot + size

        private val JVMType.occupiesTwoSlots get() =
                this == JVMPrimitive.LONG.jvmType || this == JVMPrimitive.DOUBLE.jvmType
    }

    private val locals = mutableListOf<Local>()
    private lateinit var objLocal: Local
    private lateinit var contextLocal: Local
    private lateinit var typeCodeLocal: Local

    override fun buildBody() {
        buildCountMethod()
        buildWriteMethod()
        buildReadMethod()
    }

    protected fun declareLocal(type: JVMType) =
            Local(type, locals.lastOrNull()?.nextSlot ?: 0).also {
                locals += it
            }

    protected inline fun <reified T> declareLocal() = declareLocal(T::class.jvmType)

    protected fun declareLocal(klass: Class<*>) = declareLocal(klass.jvmType)

    protected fun MethodVisitor.loadLocal(local: Local) = loadSlot(local.type, local.slot)

    protected fun MethodVisitor.saveToLocal(local: Local) = saveToSlot(local.type, local.slot)

    protected fun MethodVisitor.loadObj() = loadLocal(objLocal)

    protected fun MethodVisitor.loadContext() = loadLocal(contextLocal)

    private val occupiedSlotsCount get() = locals.last().nextSlot

    //---------- Writing -------------------------------------------------------//

    protected fun MethodVisitor.loadResult() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, WriteContext::class.jvmType.name, "result", ByteArray::class.jvmType.signature)
    }

    protected inline fun <reified T> MethodVisitor.loadIndex() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, T::class.jvmType.name, "index", Int::class.jvmType.signature)
    }

    protected inline fun <reified T> MethodVisitor.increaseIndexBy(value: Int) {
        loadContext()
        loadIndex<T>()
        visitLdcInsn(value)
        visitInsn(Opcodes.IADD)
        visitFieldInsn(Opcodes.PUTFIELD, T::class.jvmType.name, "index", Int::class.jvmType.signature)
    }

    protected inline fun <reified T> MethodVisitor.increaseIndexBy(valueLoader: MethodVisitor.() -> Unit) {
        loadContext()
        loadIndex<T>()
        valueLoader()
        visitInsn(Opcodes.IADD)
        visitFieldInsn(Opcodes.PUTFIELD, T::class.jvmType.name, "index", Int::class.jvmType.signature)
    }

    private val JVMPrimitive.writeSafeMethodName get() = "write${javaName.capitalize()}Safe"
    private val JVMPrimitive.writeUnsafeMethodName get() = "write${javaName.capitalize()}Unsafe"

    protected fun MethodVisitor.writeSafe(klass: Class<*>, valueLoader: MethodVisitor.() -> Unit) {
        val jvmPrimitive = klass.jvmPrimitiveType ?: error("Expected a primitive but was $klass")
        loadThis()                                                                       // stack: [this]
        loadResult()                                                                     // stack: [this, result]
        loadIndex<WriteContext>()                                                        // stack: [this, result, index]
        valueLoader()                                                                    // stack: [this, result, index, value]
        callVirtual(classType, jvmPrimitive.writeSafeMethodName,
                listOf(ByteArray::class.java, Int::class.java, klass), Void::class.java) // this.write<type>Safe(result, index, value); stack: []
        increaseIndexBy<WriteContext>(jvmPrimitive.size)                                 // index += type.size; stack: []
    }

    protected inline fun <reified T> MethodVisitor.writeSafe(noinline valueLoader: MethodVisitor.() -> Unit) {
        val jvmPrimitive = T::class.jvmPrimitiveType ?: error("Expected a primitive but was ${T::class}")
        loadThis()                                                                         // stack: [this]
        loadResult()                                                                       // stack: [this, result]
        loadIndex<WriteContext>()                                                          // stack: [this, result, index]
        valueLoader()                                                                      // stack: [this, result, index, value]
        callVirtual3<ByteArray, Int, T, Void>(classType, jvmPrimitive.writeSafeMethodName) // this.write<type>Safe(result, index, value); stack: []
        increaseIndexBy<WriteContext>(jvmPrimitive.size)                                   // index += type.size; stack: []
    }

    protected fun MethodVisitor.writeLength(start: Local) {
        loadThis()                                         // stack: [this]
        loadResult()                                       // stack: [this, result]
        loadLocal(start)                                   // stack: [this, result, start]
        loadIndex<WriteContext>()                          // stack: [this, result, start, index]
        loadLocal(start)                                   // stack: [this, result, start, index, start]
        visitLdcInsn(4)                                    // stack: [this, result, start, index, start, 4]
        visitInsn(Opcodes.IADD)                            // stack: [this, result, start, index, start + 4]
        visitInsn(Opcodes.ISUB)                            // stack: [this, result, start, index - start - 4 => length]
        callVirtual3<ByteArray, Int, Int, Void>(classType,
                JVMPrimitive.INT.writeUnsafeMethodName)    // this.writeIntUnsafe(result, start, length); stack: []
    }

    private fun buildCountMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "countSize",
                listOf(WriteContext::class.java, klass), Int::class.java).run {
            declareLocal(classType) // this.
            contextLocal = declareLocal<WriteContext>()
            objLocal = declareLocal(klass)
            if (klass.isReference) {
                loadObj()                                      // stack: [obj]
                val notNullLabel = Label()
                visitJumpInsn(Opcodes.IFNONNULL, notNullLabel) // if (obj != null) goto notNull; stack: []
                visitLdcInsn(1)                                // stack: [1]
                ret<Int>()                                     // return 1; stack: []
                visitLabel(notNullLabel)
            }
            visitMaxs(countSizeNotNull(), occupiedSlotsCount)
            locals.clear()
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

    /**
     * Returns stack size.
     */
    protected abstract fun MethodVisitor.countSizeNotNull(): Int

    private fun buildWriteMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "write",
                        listOf(WriteContext::class.java, klass), Void::class.java).run {
            declareLocal(classType) // this.
            contextLocal = declareLocal<WriteContext>()
            objLocal = declareLocal(klass)
            if (klass.isReference) {
                loadObj()                                                    // stack: [obj]
                val notNullLabel = Label()
                visitJumpInsn(Opcodes.IFNONNULL, notNullLabel)               // if (obj != null) goto notNull; stack: []
                writeSafe<Byte> { visitLdcInsn(GroBufTypeCode.Empty.value) } // this.writeByteSafe(context.result, context.index, Empty); stack: []
                ret<Void>()                                                  // return; stack: []
                visitLabel(notNullLabel)
            }
            visitMaxs(writeNotNull(), occupiedSlotsCount)
            locals.clear()
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

    /**
     * Returns stack size.
     */
    protected abstract fun MethodVisitor.writeNotNull(): Int

    //---------- Reading -----------------------------------------------------------//

    protected fun MethodVisitor.loadData() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, ReadContext::class.jvmType.name, "data", ByteArray::class.jvmType.signature)
    }

    private val JVMPrimitive.readSafeMethodName get() = "read${javaName.capitalize()}Safe"

    protected fun MethodVisitor.readSafe(klass: Class<*>) {
        val jvmPrimitive = klass.jvmPrimitiveType ?: error("Expected a primitive but was $klass")
        loadThis()                                                     // stack: [this]
        loadData()                                                     // stack: [this, data]
        loadIndex<ReadContext>()                                       // stack: [this, data, index]
        callVirtual(classType, jvmPrimitive.readSafeMethodName,
                listOf(ByteArray::class.java, Int::class.java), klass) // stack: [this.read<type>Safe(result, index)]
        increaseIndexBy<ReadContext>(jvmPrimitive.size)                // index += type.size; stack: [this.read<type>Safe(result, index)]
    }

    protected inline fun <reified T> MethodVisitor.readSafe() {
        val jvmPrimitive = T::class.jvmPrimitiveType ?: error("Expected a primitive but was ${T::class}")
        loadThis()                                                                  // stack: [this]
        loadData()                                                                  // stack: [this, data]
        loadIndex<ReadContext>()                                                    // stack: [this, data, index]
        callVirtual2<ByteArray, Int, T>(classType, jvmPrimitive.readSafeMethodName) // stack: [this.read<type>Safe(result, index)]
        increaseIndexBy<ReadContext>(jvmPrimitive.size)                             // index += type.size; stack: [this.read<type>Safe(result, index)]
    }

    protected fun MethodVisitor.loadTypeCode() = loadLocal(typeCodeLocal)

    protected fun MethodVisitor.loadDefault(klass: Class<*>) {
        when (klass.jvmPrimitiveType) {
            JVMPrimitive.BYTE,
            JVMPrimitive.SHORT,
            JVMPrimitive.INT,
            JVMPrimitive.CHAR,
            JVMPrimitive.BOOLEAN -> visitLdcInsn(0)
            JVMPrimitive.LONG -> visitLdcInsn(0L)
            JVMPrimitive.FLOAT -> visitLdcInsn(0.0f)
            JVMPrimitive.DOUBLE -> visitLdcInsn(0.0)
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
        callVirtual2<Int, ReadContext, Void>(classType, "skipValue") // this.skipValue(typeCode, context)]
        loadDefault(klass)
        ret(klass)

        visitLabel(okLabel)
    }

    private fun buildReadMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "read",
                listOf(ReadContext::class.java), klass).run {
            declareLocal(classType) // this.
            contextLocal = declareLocal<ReadContext>()
            typeCodeLocal = declareLocal<Int>()

            readSafe<Byte>()                                    // stack: [this.readByteSafe(context.data, context.index) => typeCode]
            visitInsn(Opcodes.DUP)                              // stack: [typeCode, typeCode]
            saveToLocal(typeCodeLocal)
            val notEmptyLabel = Label()
            visitLdcInsn(GroBufTypeCode.Empty.value)            // stack: [typeCode, Empty]
            visitJumpInsn(Opcodes.IF_ICMPNE, notEmptyLabel)     // if (type != Empty) goto notEmpty; stack: []

            loadDefault(klass)
            ret(klass)                                          // return default(type); stack: []

            visitLabel(notEmptyLabel)
            loadThis()                                          // stack: [this]
            loadTypeCode()                                      // stack: [this, typeCode]
            callVirtual1<Int, Void>(classType, "checkTypeCode") // this.checkTypeCode(typeCode); stack: []

            visitMaxs(readNotNull(), occupiedSlotsCount)
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

    /**
     * Returns stack size.
     */
    protected abstract fun MethodVisitor.readNotNull(): Int

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
            callVirtual(classType, name, argumentTypes.map { it.klass }, returnType.klass)
            if (returnType.erased)
                castToObject(returnType.klass.jvmType)
            ret(returnType.actualType)
            visitMaxs(argumentTypes.size + 1, argumentTypes.size + 1)
            visitEnd()
        }
    }
}