package grobuf

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.reflect.KClass


internal fun MethodVisitor.castObjectTo(type: JVMType) {
    if (type !is JVMType.Primitive) {
        visitTypeInsn(Opcodes.CHECKCAST, type.name)
    } else {
        val boxType = type.jvmPrimitive.boxType
                ?: throw IllegalStateException("Cast to void is not allowed")
        visitTypeInsn(Opcodes.CHECKCAST, boxType.name)
        // Unbox value.
        callVirtual(boxType, type.jvmPrimitive.unboxMethodName,
                emptyList(), type)
    }
}

internal fun MethodVisitor.castToObject(type: JVMType) {
    if (type is JVMType.Primitive) {
        val boxType = type.jvmPrimitive.boxType
                ?: throw IllegalStateException("Cast from void is not allowed")
        // Box value.
        call(Opcodes.INVOKESTATIC, boxType, type.jvmPrimitive.boxMethodName,
                listOf(type), boxType)
    }
}

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

internal fun MethodVisitor.ret(klass: Class<*>) = ret(klass.jvmPrimitiveType)

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

internal fun MethodVisitor.loadSlot(klass: Class<*>, index: Int) = loadSlot(klass.jvmPrimitiveType, index)

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

internal fun MethodVisitor.saveToSlot(klass: Class<*>, index: Int) = saveToSlot(klass.jvmPrimitiveType, index)

internal inline fun<reified T> MethodVisitor.saveToSlot(index: Int) = saveToSlot(T::class.jvmPrimitiveType, index)

internal fun ClassVisitor.defineMethod(access: Int, name: String, argumentTypes: List<JVMType>, returnType: JVMType) =
        visitMethod(access, name, computeMethodJVMSignature(argumentTypes, returnType), null, null)

internal fun ClassVisitor.defineMethod(access: Int, name: String, argumentTypes: List<Class<*>>, returnType: Class<*>) =
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

internal fun MethodVisitor.call(opcode: Int, owner: JVMType, name: String, argumentTypes: List<Class<*>>, returnType: Class<*>) =
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

internal fun MethodVisitor.ctorCall(owner: JVMType, argumentTypes: List<Class<*>>) =
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

internal fun MethodVisitor.callVirtual(owner: JVMType, name: String, argumentTypes: List<Class<*>>, returnType: Class<*>) =
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
