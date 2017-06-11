package grobuf

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.reflect.KClass


internal fun MethodVisitor.castObjectTo(type: String) {
    val box = primitiveToBoxed[type]
    visitTypeInsn(Opcodes.CHECKCAST, box?.name ?: type)
    if (box != null) { // Unbox value.
        visitMethodInsn(Opcodes.INVOKEVIRTUAL, box.name, box.unboxMethodName,
                "()$type", false)
    }
}

internal fun MethodVisitor.castToObject(type: String) {
    val box = primitiveToBoxed[type]
    if (box != null) { // Box value.
        visitMethodInsn(Opcodes.INVOKESTATIC, box.name, box.boxMethodName,
                "($type)${box.name.toJVMSignature()}", false)
    }
}

private fun MethodVisitor.ret(jvmPrimitive: JVMPrimitive?) {
    val opcode = when (jvmPrimitive) {
        JVMPrimitive.Byte,
        JVMPrimitive.Short,
        JVMPrimitive.Char,
        JVMPrimitive.Int,
        JVMPrimitive.Boolean -> Opcodes.IRETURN
        JVMPrimitive.Long-> Opcodes.LRETURN
        JVMPrimitive.Float -> Opcodes.FRETURN
        JVMPrimitive.Double -> Opcodes.DRETURN
        JVMPrimitive.Void -> Opcodes.RETURN
        else -> Opcodes.ARETURN
    }
    visitInsn(opcode)
}

internal fun MethodVisitor.ret(klass: Class<*>) = ret(klass.jvmPrimitiveType)

internal inline fun<reified T> MethodVisitor.ret() = ret(T::class.jvmPrimitiveType)

private fun MethodVisitor.loadSlot(jvmPrimitive: JVMPrimitive?, index: Int) {
    val opcode = when (jvmPrimitive) {
        JVMPrimitive.Byte,
        JVMPrimitive.Short,
        JVMPrimitive.Char,
        JVMPrimitive.Int,
        JVMPrimitive.Boolean -> Opcodes.ILOAD
        JVMPrimitive.Long -> Opcodes.LLOAD
        JVMPrimitive.Float -> Opcodes.FLOAD
        JVMPrimitive.Double -> Opcodes.DLOAD
        else -> Opcodes.ALOAD
    }
    visitVarInsn(opcode, index)
}

internal fun MethodVisitor.loadSlot(klass: Class<*>, index: Int) = loadSlot(klass.jvmPrimitiveType, index)

internal inline fun<reified T> MethodVisitor.loadSlot(index: Int) = loadSlot(T::class.jvmPrimitiveType, index)

private fun MethodVisitor.saveToSlot(jvmPrimitive: JVMPrimitive?, index: Int) {
    val opcode = when (jvmPrimitive) {
        JVMPrimitive.Byte,
        JVMPrimitive.Short,
        JVMPrimitive.Char,
        JVMPrimitive.Int,
        JVMPrimitive.Boolean -> Opcodes.ISTORE
        JVMPrimitive.Long -> Opcodes.LSTORE
        JVMPrimitive.Float -> Opcodes.FSTORE
        JVMPrimitive.Double -> Opcodes.DSTORE
        else -> Opcodes.ASTORE
    }
    visitVarInsn(opcode, index)
}

internal fun MethodVisitor.saveToSlot(klass: Class<*>, index: Int) = saveToSlot(klass.jvmPrimitiveType, index)

internal inline fun<reified T> MethodVisitor.saveToSlot(index: Int) = saveToSlot(T::class.jvmPrimitiveType, index)

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

internal fun MethodVisitor.call(opcode: Int, owner: String, name: String, argumentTypes: List<Class<*>>, returnType: Class<*>) =
        visitMethodInsn(opcode, owner, name, computeMethodJVMSignature(argumentTypes, returnType), false)

private fun MethodVisitor.callK(opcode: Int, owner: String, name: String, argumentTypes: List<KClass<*>>, returnType: KClass<*>) =
        visitMethodInsn(opcode, owner, name, computeMethodJVMSignature(argumentTypes, returnType), false)

internal inline fun<reified R> MethodVisitor.call0(opcode: Int, owner: String, name: String) =
        callK(opcode, owner, name, emptyList(), R::class)

internal inline fun<reified T1, reified R> MethodVisitor.call1(opcode: Int, owner: String, name: String) =
        callK(opcode, owner, name, listOf(T1::class), R::class)

internal inline fun<reified T1, reified T2, reified R> MethodVisitor.call2(opcode: Int, owner: String, name: String) =
        callK(opcode, owner, name, listOf(T1::class, T2::class), R::class)

internal inline fun<reified T1, reified T2, reified T3, reified R> MethodVisitor.call3(opcode: Int, owner: String, name: String) =
        callK(opcode, owner, name, listOf(T1::class, T2::class, T3::class), R::class)

internal fun MethodVisitor.ctorCall(owner: String, argumentTypes: List<Class<*>>) =
        call(Opcodes.INVOKESPECIAL, owner, "<init>", argumentTypes, Void::class.java)

private fun MethodVisitor.ctorCallK(owner: String, argumentTypes: List<KClass<*>>) =
        callK(Opcodes.INVOKESPECIAL, owner, "<init>", argumentTypes, Void::class)

internal fun MethodVisitor.ctorCall0(owner: String) = ctorCall(owner, emptyList())

internal inline fun<reified T1> MethodVisitor.ctorCall1(owner: String) = ctorCallK(owner, listOf(T1::class))

internal inline fun<reified T1, reified T2> MethodVisitor.ctorCall2(owner: String) =
        ctorCallK(owner, listOf(T1::class, T2::class))

internal inline fun<reified T1, reified T2, reified T3> MethodVisitor.ctorCall3(owner: String) =
        ctorCallK(owner, listOf(T1::class, T2::class, T3::class))

internal fun MethodVisitor.callVirtual(owner: String, name: String, argumentTypes: List<Class<*>>, returnType: Class<*>) =
        call(Opcodes.INVOKEVIRTUAL, owner, name, argumentTypes, returnType)

private fun MethodVisitor.callVirtualK(owner: String, name: String, argumentTypes: List<KClass<*>>, returnType: KClass<*>) =
        callK(Opcodes.INVOKEVIRTUAL, owner, name, argumentTypes, returnType)

internal inline fun<reified R> MethodVisitor.callVirtual0(owner: String, name: String) =
        callVirtualK(owner, name, emptyList(), R::class)

internal inline fun<reified T1, reified R> MethodVisitor.callVirtual1(owner: String, name: String) =
        callVirtualK(owner, name, listOf(T1::class), R::class)

internal inline fun<reified T1, reified T2, reified R> MethodVisitor.callVirtual2(owner: String, name: String) =
        callVirtualK(owner, name, listOf(T1::class, T2::class), R::class)

internal inline fun<reified T1, reified T2, reified T3, reified R> MethodVisitor.callVirtual3(owner: String, name: String) =
        callVirtualK(owner, name, listOf(T1::class, T2::class, T3::class), R::class)
