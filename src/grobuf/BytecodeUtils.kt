package grobuf

import org.objectweb.asm.Opcodes
import kotlin.reflect.KClass

internal enum class JVMPrimitive(val shortName: String, val javaName: String, val javaBoxName: String?,
                                 val size: Int, val typeAsArrayElement: Int,
                                 val klass: Class<*>, val kClass: KClass<*>) {
    BYTE   ("B", "byte"   , "Byte"     , 1, Opcodes.T_BYTE   , Byte::class.java   , Byte::class   ),
    SHORT  ("S", "short"  , "Short"    , 2, Opcodes.T_SHORT  , Short::class.java  , Short::class  ),
    INT    ("I", "int"    , "Integer"  , 4, Opcodes.T_INT    , Int::class.java    , Int::class    ),
    LONG   ("J", "long"   , "Long"     , 8, Opcodes.T_LONG   , Long::class.java   , Long::class   ),
    BOOLEAN("Z", "boolean", "Boolean"  , 1, Opcodes.T_BOOLEAN, Boolean::class.java, Boolean::class),
    CHAR   ("C", "char"   , "Character", 2, Opcodes.T_CHAR   , Char::class.java   , Char::class   ),
    FLOAT  ("F", "float"  , "Float"    , 4, Opcodes.T_FLOAT  , Float::class.java  , Float::class  ),
    DOUBLE ("D", "double" , "Double"   , 8, Opcodes.T_DOUBLE , Double::class.java , Double::class ),
    VOID   ("V", "void"   , null       , 0, 0                , Void::class.java   , Void::class   );

    val boxType by lazy { JVMType.FromString("java/lang/$javaBoxName") }
    val unboxMethodName by lazy { if (javaBoxName == null) "" else "${javaName}Value" }
    val boxMethodName by lazy { if (javaBoxName == null) "" else "valueOf" }
}

internal sealed class JVMType {
    abstract val name: String
    abstract val signature: String

    override fun equals(other: Any?): Boolean {
        return other is JVMType && other.signature == signature
    }

    override fun hashCode(): Int {
        return signature.hashCode()
    }

    class FromString(override val name: String) : JVMType() {
        override val signature get() = "L$name;"
    }

    class FromClass(val klass: Class<*>) : JVMType() {
        override val name: String
            get() = if (klass.isArray)
                "[${klass.componentType!!.jvmType.signature}"
            else
                klass.canonicalName.replace('.', '/')

        override val signature: String
            get() = if (klass.isArray)
                name
            else
                "L$name;"
    }

    class Primitive(val jvmPrimitive: JVMPrimitive) : JVMType() {
        override val name get() = jvmPrimitive.shortName

        override val signature get() = name
    }
}

internal fun String.toJVMIdentifier() = replace('/', '_').replace('.', '_').replace(';', '_').replace("[", "Arr")

internal val Class<*>.jvmPrimitiveType get() =
        enumValues<JVMPrimitive>().firstOrNull { it.klass == this }

internal val KClass<*>.jvmPrimitiveType get() =
        enumValues<JVMPrimitive>().firstOrNull { it.kClass == this }

internal val Class<*>.jvmType get() =
        jvmPrimitiveType?.let { JVMType.Primitive(it) } ?: JVMType.FromClass(this)

internal val KClass<*>.jvmType get() =
        jvmPrimitiveType?.let { JVMType.Primitive(it) } ?: JVMType.FromClass(this.java)

internal fun computeMethodJVMSignature(argumentTypes: List<JVMType>, returnType: JVMType) =
        "(${argumentTypes.joinToString(separator = "") { it.signature }})${returnType.signature}"

internal fun computeMethodJVMSignature(argumentTypes: List<Class<*>>, returnType: Class<*>) =
        "(${argumentTypes.joinToString(separator = "") { it.jvmType.signature }})${returnType.jvmType.signature}"

internal fun computeMethodJVMSignature(argumentTypes: List<KClass<*>>, returnType: KClass<*>) =
        "(${argumentTypes.joinToString(separator = "") { it.jvmType.signature }})${returnType.jvmType.signature}"
internal val Class<*>.occupiesTwoSlots get() = this == Long::class.java || this == Double::class.java
