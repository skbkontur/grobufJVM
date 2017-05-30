package grobuf

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


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

internal fun MethodVisitor.ret(klass: Class<*>) {
    val opcode = when (klass.jvmPrimitiveType) {
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

internal inline fun<reified T> MethodVisitor.ret() = ret(T::class.java)

internal fun MethodVisitor.loadArg(klass: Class<*>, index: Int) {
    val opcode = when (klass.jvmPrimitiveType) {
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

internal inline fun<reified T> MethodVisitor.loadArg(index: Int) = loadArg(T::class.java, index)

internal fun ClassVisitor.defineMethod(access: Int, name: String, argumentTypes: List<Class<*>>, returnType: Class<*>) =
        visitMethod(access, name, computeMethodJVMSignature(argumentTypes, returnType), null, null)

internal inline fun<reified R> ClassVisitor.defineMethod0(access: Int, name: String) =
        defineMethod(access, name, emptyList(), R::class.java)

internal inline fun<reified T1, reified R> ClassVisitor.defineMethod1(access: Int, name: String) =
        defineMethod(access, name, listOf(T1::class.java), R::class.java)

internal inline fun<reified T1, reified T2, reified R> ClassVisitor.defineMethod2(access: Int, name: String) =
        defineMethod(access, name, listOf(T1::class.java, T2::class.java), R::class.java)

internal inline fun<reified T1, reified T2, reified T3, reified R> ClassVisitor.defineMethod3(access: Int, name: String) =
        defineMethod(access, name, listOf(T1::class.java, T2::class.java, T3::class.java), R::class.java)

internal fun MethodVisitor.call(opcode: Int, owner: String, name: String, argumentTypes: List<Class<*>>, returnType: Class<*>) =
        visitMethodInsn(opcode, owner, name, computeMethodJVMSignature(argumentTypes, returnType), false)

internal inline fun<reified R> MethodVisitor.call0(opcode: Int, owner: String, name: String) =
        call(opcode, owner, name, emptyList(), R::class.java)

internal inline fun<reified T1, reified R> MethodVisitor.call1(opcode: Int, owner: String, name: String) =
        call(opcode, owner, name, listOf(T1::class.java), R::class.java)

internal inline fun<reified T1, reified T2, reified R> MethodVisitor.call2(opcode: Int, owner: String, name: String) =
        call(opcode, owner, name, listOf(T1::class.java, T2::class.java), R::class.java)

internal inline fun<reified T1, reified T2, reified T3, reified R> MethodVisitor.call3(opcode: Int, owner: String, name: String) =
        call(opcode, owner, name, listOf(T1::class.java, T2::class.java, T3::class.java), R::class.java)

internal fun MethodVisitor.ctorCall(owner: String, argumentTypes: List<Class<*>>) =
        call(Opcodes.INVOKESPECIAL, owner, "<init>", argumentTypes, Void::class.java)

internal fun MethodVisitor.ctorCall0(owner: String) = ctorCall(owner, emptyList())

internal inline fun<reified T1> MethodVisitor.ctorCall1(owner: String) = ctorCall(owner, listOf(T1::class.java))

internal inline fun<reified T1, reified T2> MethodVisitor.ctorCall2(owner: String) =
        ctorCall(owner, listOf(T1::class.java, T2::class.java))

internal inline fun<reified T1, reified T2, reified T3> MethodVisitor.ctorCall3(owner: String) =
        ctorCall(owner, listOf(T1::class.java, T2::class.java, T3::class.java))

internal fun MethodVisitor.callVirtual(owner: String, name: String, argumentTypes: List<Class<*>>, returnType: Class<*>) =
        call(Opcodes.INVOKEVIRTUAL, owner, name, argumentTypes, returnType)

internal inline fun<reified R> MethodVisitor.callVirtual0(owner: String, name: String) =
        callVirtual(owner, name, emptyList(), R::class.java)

internal inline fun<reified T1, reified R> MethodVisitor.callVirtual1(owner: String, name: String) =
        callVirtual(owner, name, listOf(T1::class.java), R::class.java)

internal inline fun<reified T1, reified T2, reified R> MethodVisitor.callVirtual2(owner: String, name: String) =
        callVirtual(owner, name, listOf(T1::class.java, T2::class.java), R::class.java)

internal inline fun<reified T1, reified T2, reified T3, reified R> MethodVisitor.callVirtual3(owner: String, name: String) =
        callVirtual(owner, name, listOf(T1::class.java, T2::class.java, T3::class.java), R::class.java)
