package grobuf

import org.objectweb.asm.Opcodes
import kotlin.reflect.KClass

internal class Box(val type: JVMType, val unboxMethodName: String, val boxMethodName: String)

internal enum class JVMPrimitive(val shortName: String, val size: kotlin.Int, val typeAsArrayElement: kotlin.Int) {
    Byte("B", 1, Opcodes.T_BYTE),
    Short("S", 2, Opcodes.T_SHORT),
    Int("I", 4, Opcodes.T_INT),
    Long("J", 8, Opcodes.T_LONG),
    Boolean("Z", 1, Opcodes.T_BOOLEAN),
    Char("C", 2, Opcodes.T_CHAR),
    Float("F", 4, Opcodes.T_FLOAT),
    Double("D", 8, Opcodes.T_DOUBLE),
    Void("V", 0, 0)
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

        val box get() = primitiveToBoxed[jvmPrimitive]

        companion object {
            private val primitiveToBoxed = mapOf(
                    JVMPrimitive.Byte    to Box(java.lang.Byte::class.java.jvmType     , "byteValue", "valueOf"),
                    JVMPrimitive.Short   to Box(java.lang.Short::class.java.jvmType    , "shortValue", "valueOf"),
                    JVMPrimitive.Int     to Box(java.lang.Integer::class.java.jvmType  , "intValue", "valueOf"),
                    JVMPrimitive.Long    to Box(java.lang.Long::class.java.jvmType     , "longValue", "valueOf"),
                    JVMPrimitive.Boolean to Box(java.lang.Boolean::class.java.jvmType  , "booleanValue", "valueOf"),
                    JVMPrimitive.Char    to Box(java.lang.Character::class.java.jvmType, "charValue", "valueOf"),
                    JVMPrimitive.Float   to Box(java.lang.Float::class.java.jvmType    , "floatValue", "valueOf"),
                    JVMPrimitive.Double  to Box(java.lang.Double::class.java.jvmType   , "doubleValue", "valueOf")
            )
        }
    }
}

internal fun String.toJVMIdentifier() = replace('/', '_').replace('.', '_').replace(';', '_').replace("[", "Arr")

internal val Class<*>.jvmPrimitiveType get() = when (this) {
    Byte::class.java    -> JVMPrimitive.Byte
    Short::class.java   -> JVMPrimitive.Short
    Int::class.java     -> JVMPrimitive.Int
    Long::class.java    -> JVMPrimitive.Long
    Boolean::class.java -> JVMPrimitive.Boolean
    Char::class.java    -> JVMPrimitive.Char
    Float::class.java   -> JVMPrimitive.Float
    Double::class.java  -> JVMPrimitive.Double
    Void::class.java    -> JVMPrimitive.Void
    else -> null
}

internal val KClass<*>.jvmPrimitiveType get() = when (this) {
    Byte::class    -> JVMPrimitive.Byte
    Short::class   -> JVMPrimitive.Short
    Int::class     -> JVMPrimitive.Int
    Long::class    -> JVMPrimitive.Long
    Boolean::class -> JVMPrimitive.Boolean
    Char::class    -> JVMPrimitive.Char
    Float::class   -> JVMPrimitive.Float
    Double::class  -> JVMPrimitive.Double
    Void::class    -> JVMPrimitive.Void
    else -> null
}

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
