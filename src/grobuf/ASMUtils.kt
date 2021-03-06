package grobuf

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Opcodes
import java.lang.reflect.Type
import kotlin.reflect.KClass


internal fun MethodVisitor.cast(from: JVMType, to: JVMType) {
    if (from is JVMType.Primitive) {
        if (to is JVMType.Primitive) {
            if (from == to) return
            error("Cast between two different primitives is not supported")
        }
        // Box value.
        val boxType = from.jvmPrimitive.boxType
                ?: throw IllegalStateException("Cast from void is not allowed")
        call(Opcodes.INVOKESTATIC, boxType, from.jvmPrimitive.boxMethodName,
                listOf(from), boxType)
    } else {
        if (to is JVMType.Primitive) {
            // Unbox value.
            val boxType = to.jvmPrimitive.boxType
                    ?: throw IllegalStateException("Cast to void is not allowed")
            if (from != boxType)
                visitTypeInsn(Opcodes.CHECKCAST, boxType.name)
            callVirtual(boxType, to.jvmPrimitive.unboxMethodName,
                    emptyList(), to)
        } else {
            if (to != Any::class.java)
                visitTypeInsn(Opcodes.CHECKCAST, to.name)
        }
    }
}

internal fun MethodVisitor.coerce(from: JVMPrimitive, to: JVMPrimitive) {
    if (from == to) return
    if (to != JVMPrimitive.BOOLEAN) {
        coercions[from.ordinal][to.ordinal].forEach { visitInsn(it) }
        return
    }
    @Suppress("NON_EXHAUSTIVE_WHEN")
    when (from) {
        JVMPrimitive.BYTE,
        JVMPrimitive.SHORT,
        JVMPrimitive.INT,
        JVMPrimitive.CHAR -> {
            val falseLabel = Label()
            visitLdcInsn(0)
            visitJumpInsn(IF_ICMPEQ, falseLabel)
            visitLdcInsn(1)
            val doneLabel = Label()
            visitJumpInsn(GOTO, doneLabel)
            visitLabel(falseLabel)
            visitLdcInsn(0)
            visitLabel(doneLabel)
        }

        JVMPrimitive.LONG -> {
            visitLdcInsn(0L)
            visitInsn(LCMP)
            val falseLabel = Label()
            visitJumpInsn(IFEQ, falseLabel)
            visitLdcInsn(1)
            val doneLabel = Label()
            visitJumpInsn(GOTO, doneLabel)
            visitLabel(falseLabel)
            visitLdcInsn(0)
            visitLabel(doneLabel)
        }

        JVMPrimitive.FLOAT -> {
            visitLdcInsn(0.0f)
            visitInsn(FCMPG)
            val falseLabel = Label()
            visitJumpInsn(IFEQ, falseLabel)
            visitLdcInsn(1)
            val doneLabel = Label()
            visitJumpInsn(GOTO, doneLabel)
            visitLabel(falseLabel)
            visitLdcInsn(0)
            visitLabel(doneLabel)
        }

        JVMPrimitive.DOUBLE -> {
            visitLdcInsn(0.0)
            visitInsn(DCMPG)
            val falseLabel = Label()
            visitJumpInsn(IFEQ, falseLabel)
            visitLdcInsn(1)
            val doneLabel = Label()
            visitJumpInsn(GOTO, doneLabel)
            visitLabel(falseLabel)
            visitLdcInsn(0)
            visitLabel(doneLabel)
        }
    }
}

private fun ops(vararg opcodes: Int) = opcodes

private val coercions = arrayOf(
        //                        BYTE           SHORT         INT      LONG    BOOL    CHAR            FLAT     DOUBLE
        /* BYTE    */ arrayOf(ops()        , ops(I2S)     , ops()   , ops(I2L), ops(), ops(I2C)     , ops(I2F), ops(I2D)),
        /* SHORT   */ arrayOf(ops(I2B)     , ops()        , ops()   , ops(I2L), ops(), ops(I2C)     , ops(I2F), ops(I2D)),
        /* INT     */ arrayOf(ops(I2B)     , ops(I2S)     , ops()   , ops(I2L), ops(), ops(I2C)     , ops(I2F), ops(I2D)),
        /* LONG    */ arrayOf(ops(L2I, I2B), ops(L2I, I2S), ops(L2I), ops()   , ops(), ops(L2I, I2C), ops(L2F), ops(L2D)),
        /* BOOLEAN */ arrayOf(ops()        , ops()        , ops()   , ops(I2L), ops(), ops()        , ops(I2F), ops(I2D)),
        /* CHAR    */ arrayOf(ops(I2B)     , ops(I2S)     , ops()   , ops(I2L), ops(), ops()        , ops(I2F), ops(I2D)),
        /* FLOAT   */ arrayOf(ops(F2I, I2B), ops(F2I, I2S), ops(F2I), ops(F2L), ops(), ops(F2I, I2C), ops()   , ops(F2D)),
        /* DOUBLE  */ arrayOf(ops(D2I, I2B), ops(D2I, I2S), ops(D2I), ops(D2L), ops(), ops(D2I, I2C), ops(D2F), ops())
)

private fun MethodVisitor.ret(jvmPrimitive: JVMPrimitive?) {
    val opcode = when (jvmPrimitive) {
        JVMPrimitive.BYTE,
        JVMPrimitive.SHORT,
        JVMPrimitive.CHAR,
        JVMPrimitive.INT,
        JVMPrimitive.BOOLEAN -> Opcodes.IRETURN
        JVMPrimitive.LONG -> Opcodes.LRETURN
        JVMPrimitive.FLOAT -> Opcodes.FRETURN
        JVMPrimitive.DOUBLE -> Opcodes.DRETURN
        JVMPrimitive.VOID -> Opcodes.RETURN
        else -> Opcodes.ARETURN
    }
    visitInsn(opcode)
}

internal fun MethodVisitor.ret(type: Type) = ret(type.klass.jvmPrimitiveType)

internal inline fun<reified T> MethodVisitor.ret() = ret(T::class.jvmPrimitiveType)

private fun MethodVisitor.loadSlot(jvmPrimitive: JVMPrimitive?, index: Int) {
    val opcode = when (jvmPrimitive) {
        JVMPrimitive.BYTE,
        JVMPrimitive.SHORT,
        JVMPrimitive.CHAR,
        JVMPrimitive.INT,
        JVMPrimitive.BOOLEAN -> Opcodes.ILOAD
        JVMPrimitive.LONG -> Opcodes.LLOAD
        JVMPrimitive.FLOAT -> Opcodes.FLOAD
        JVMPrimitive.DOUBLE -> Opcodes.DLOAD
        else -> Opcodes.ALOAD
    }
    visitVarInsn(opcode, index)
}

internal fun MethodVisitor.loadSlot(type: JVMType, index: Int) =
        loadSlot((type as? JVMType.Primitive)?.jvmPrimitive, index)

internal fun MethodVisitor.loadSlot(type: Type, index: Int) = loadSlot(type.klass.jvmPrimitiveType, index)

internal inline fun<reified T> MethodVisitor.loadSlot(index: Int) = loadSlot(T::class.jvmPrimitiveType, index)

private fun MethodVisitor.saveToSlot(jvmPrimitive: JVMPrimitive?, index: Int) {
    val opcode = when (jvmPrimitive) {
        JVMPrimitive.BYTE,
        JVMPrimitive.SHORT,
        JVMPrimitive.CHAR,
        JVMPrimitive.INT,
        JVMPrimitive.BOOLEAN -> Opcodes.ISTORE
        JVMPrimitive.LONG -> Opcodes.LSTORE
        JVMPrimitive.FLOAT -> Opcodes.FSTORE
        JVMPrimitive.DOUBLE -> Opcodes.DSTORE
        else -> Opcodes.ASTORE
    }
    visitVarInsn(opcode, index)
}

internal fun MethodVisitor.saveToSlot(type: JVMType, index: Int) =
        saveToSlot((type as? JVMType.Primitive)?.jvmPrimitive, index)

internal fun MethodVisitor.saveToSlot(type: Type, index: Int) = saveToSlot(type.klass.jvmPrimitiveType, index)

internal inline fun<reified T> MethodVisitor.saveToSlot(index: Int) = saveToSlot(T::class.jvmPrimitiveType, index)

internal fun ClassVisitor.defineMethod(access: Int, name: String, argumentTypes: List<JVMType>, returnType: JVMType) =
        visitMethod(access, name, computeMethodJVMSignature(argumentTypes, returnType), null, null)

internal fun ClassVisitor.defineMethod(access: Int, name: String, argumentTypes: List<Type>, returnType: Type) =
        visitMethod(access, name, computeMethodJVMSignature(argumentTypes, returnType), null, null)

private fun ClassVisitor.defineMethodK(access: Int, name: String, argumentTypes: List<KClass<*>>, returnType: KClass<*>) =
        visitMethod(access, name, computeMethodJVMSignature(argumentTypes, returnType), null, null)

internal inline fun<reified R> ClassVisitor.defineMethod0(access: Int, name: String) =
        defineMethodK(access, name, emptyList(), R::class)

internal inline fun<reified T1, reified R> ClassVisitor.defineMethod1(access: Int, name: String) =
        defineMethodK(access, name, listOf(T1::class), R::class)

internal inline fun<reified T1, reified T2, reified R> ClassVisitor.defineMethod2(access: Int, name: String) =
        defineMethodK(access, name, listOf(T1::class, T2::class), R::class)

internal inline fun<reified T1, reified T2, reified T3, reified R> ClassVisitor.defineMethod3(access: Int, name: String) =
        defineMethodK(access, name, listOf(T1::class, T2::class, T3::class), R::class)

internal fun MethodVisitor.call(opcode: Int, owner: JVMType, name: String, argumentTypes: List<JVMType>, returnType: JVMType) =
        visitMethodInsn(opcode, owner.name, name, computeMethodJVMSignature(argumentTypes, returnType), false)

internal fun MethodVisitor.call(opcode: Int, owner: JVMType, name: String, argumentTypes: List<Type>, returnType: Type) =
        visitMethodInsn(opcode, owner.name, name, computeMethodJVMSignature(argumentTypes, returnType), false)

private fun MethodVisitor.callK(opcode: Int, owner: JVMType, name: String, argumentTypes: List<KClass<*>>, returnType: KClass<*>) =
        visitMethodInsn(opcode, owner.name, name, computeMethodJVMSignature(argumentTypes, returnType), false)

internal inline fun<reified R> MethodVisitor.call0(opcode: Int, owner: JVMType, name: String) =
        callK(opcode, owner, name, emptyList(), R::class)

internal inline fun<reified T1, reified R> MethodVisitor.call1(opcode: Int, owner: JVMType, name: String) =
        callK(opcode, owner, name, listOf(T1::class), R::class)

internal inline fun<reified T1, reified T2, reified R> MethodVisitor.call2(opcode: Int, owner: JVMType, name: String) =
        callK(opcode, owner, name, listOf(T1::class, T2::class), R::class)

internal inline fun<reified T1, reified T2, reified T3, reified R> MethodVisitor.call3(opcode: Int, owner: JVMType, name: String) =
        callK(opcode, owner, name, listOf(T1::class, T2::class, T3::class), R::class)

internal fun MethodVisitor.ctorCall(owner: JVMType, argumentTypes: List<Type>) =
        call(Opcodes.INVOKESPECIAL, owner, "<init>", argumentTypes, Void::class.java)

private fun MethodVisitor.ctorCallK(owner: JVMType, argumentTypes: List<KClass<*>>) =
        callK(Opcodes.INVOKESPECIAL, owner, "<init>", argumentTypes, Void::class)

internal fun MethodVisitor.ctorCall0(owner: JVMType) = ctorCall(owner, emptyList())

internal inline fun<reified T1> MethodVisitor.ctorCall1(owner: JVMType) = ctorCallK(owner, listOf(T1::class))

internal inline fun<reified T1, reified T2> MethodVisitor.ctorCall2(owner: JVMType) =
        ctorCallK(owner, listOf(T1::class, T2::class))

internal inline fun<reified T1, reified T2, reified T3> MethodVisitor.ctorCall3(owner: JVMType) =
        ctorCallK(owner, listOf(T1::class, T2::class, T3::class))

internal fun MethodVisitor.callVirtual(owner: JVMType, name: String, argumentTypes: List<JVMType>, returnType: JVMType) =
        call(Opcodes.INVOKEVIRTUAL, owner, name, argumentTypes, returnType)

internal fun MethodVisitor.callVirtual(owner: JVMType, name: String, argumentTypes: List<Type>, returnType: Type) =
        call(Opcodes.INVOKEVIRTUAL, owner, name, argumentTypes, returnType)

private fun MethodVisitor.callVirtualK(owner: JVMType, name: String, argumentTypes: List<KClass<*>>, returnType: KClass<*>) =
        callK(Opcodes.INVOKEVIRTUAL, owner, name, argumentTypes, returnType)

internal inline fun<reified R> MethodVisitor.callVirtual0(owner: JVMType, name: String) =
        callVirtualK(owner, name, emptyList(), R::class)

internal inline fun<reified T1, reified R> MethodVisitor.callVirtual1(owner: JVMType, name: String) =
        callVirtualK(owner, name, listOf(T1::class), R::class)

internal inline fun<reified T1, reified T2, reified R> MethodVisitor.callVirtual2(owner: JVMType, name: String) =
        callVirtualK(owner, name, listOf(T1::class, T2::class), R::class)

internal inline fun<reified T1, reified T2, reified T3, reified R> MethodVisitor.callVirtual3(owner: JVMType, name: String) =
        callVirtualK(owner, name, listOf(T1::class, T2::class, T3::class), R::class)
