package grobuf

import org.objectweb.asm.Opcodes
import sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.*
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

    val jvmType by lazy { JVMType.Primitive(this) }
    val boxType by lazy { if (javaBoxName == null) null else JVMType.FromString("java/lang/$javaBoxName") }
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

internal val JVMType.isBox get() =
        enumValues<JVMPrimitive>().any { it.boxType == this }

internal val Class<*>.jvmType get() =
        jvmPrimitiveType?.let { JVMType.Primitive(it) } ?: JVMType.FromClass(this)

internal val KClass<*>.jvmType get() =
        jvmPrimitiveType?.let { JVMType.Primitive(it) } ?: JVMType.FromClass(this.java)

internal val Type.jvmType get() = klass.jvmType

internal val Class<*>.isReference get() = jvmPrimitiveType == null

internal val Type.isReference get() = klass.isReference

internal fun computeMethodJVMSignature(argumentTypes: List<JVMType>, returnType: JVMType) =
        "(${argumentTypes.joinToString(separator = "") { it.signature }})${returnType.signature}"

internal fun computeMethodJVMSignature(argumentTypes: List<Type>, returnType: Type) =
        "(${argumentTypes.joinToString(separator = "") { it.jvmType.signature }})${returnType.jvmType.signature}"

internal fun computeMethodJVMSignature(argumentTypes: List<KClass<*>>, returnType: KClass<*>) =
        "(${argumentTypes.joinToString(separator = "") { it.jvmType.signature }})${returnType.jvmType.signature}"

internal val Type.klass: Class<*> get() = when (this) {
    is Class<*> -> this
    is GenericArrayType -> Array<Any?>::class.java
    is ParameterizedType -> this.rawType as Class<*>
    else -> error("Unexpected type: $this")
}

internal val Type.typeArguments get() = when (this) {
    is Class<*> -> emptyList<Type>()
    is GenericArrayType -> listOf(this.genericComponentType)
    is ParameterizedType -> this.actualTypeArguments.toList()
    else -> error("Unexpected type: $this")
}

internal fun Type.substitute(typeArguments: List<Type>): Type = when (this) {
    is Class<*> -> this

    is GenericArrayType -> {
        val substitutedComponentType = this.genericComponentType.substitute(typeArguments)
        if (substitutedComponentType == this.genericComponentType)
            this
        else
            GenericArrayTypeImpl.make(substitutedComponentType)
    }

    is ParameterizedType -> {
        val substitutedTypeArguments = this.actualTypeArguments.map { it.substitute(typeArguments) }
        if (substitutedTypeArguments.withIndex().all { it.value == this.actualTypeArguments[it.index] })
            this
        else
            ParameterizedTypeImpl.make(this.rawType as Class<*>, substitutedTypeArguments.toTypedArray(), this.ownerType)
    }

    is TypeVariable<*> -> {
        val index = this.genericDeclaration.typeParameters.indexOf(this)
        typeArguments.elementAtOrNull(index)
                ?: error("Unexpected type $this")
    }

    is WildcardType -> {
        // Erasure.
        if (this.upperBounds.isEmpty())
            Any::class.java
        else
            this.upperBounds.single().substitute(typeArguments)
    }

    else -> error("Unexpected type $this")
}

