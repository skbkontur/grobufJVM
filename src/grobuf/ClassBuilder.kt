package grobuf

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.FileOutputStream

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