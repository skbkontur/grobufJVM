package grobuf

internal class Box(val name: String, val unboxMethodName: String, val boxMethodName: String)

internal val primitiveToBoxed = mapOf(
        "B" to Box("java/lang/Byte"     , "byteValue"   , "valueOf"),
        "S" to Box("java/lang/Short"    , "shortValue"  , "valueOf"),
        "I" to Box("java/lang/Integer"  , "intValue"    , "valueOf"),
        "J" to Box("java/lang/Long"     , "longValue"   , "valueOf"),
        "Z" to Box("java/lang/Boolean"  , "booleanValue", "valueOf"),
        "C" to Box("java/lang/Character", "charValue"   , "valueOf"),
        "F" to Box("java/lang/Float"    , "floatValue"  , "valueOf"),
        "D" to Box("java/lang/Double"   , "doubleValue" , "valueOf")
)

internal enum class JVMPrimitive(val shortName: String, val size: kotlin.Int) {
    Byte("B", 1),
    Short("S", 2),
    Int("I", 4),
    Long("J", 8),
    Boolean("Z", 1),
    Char("C", 2),
    Float("F", 4),
    Double("D", 8),
    Void("V", 0)
}

internal val primitiveSignatures = enumValues<JVMPrimitive>().map { it.shortName }.toSet()

internal fun String.toJVMIdentifier() = replace('/', '_').replace('.', '_')
internal fun String.toJVMType() = replace('.', '/')
internal fun String.toJVMSignature() = toJVMType().also {
    if (primitiveSignatures.contains(it))
        return it
    return "L$it;"
}

internal val Class<*>.jvmPrimitiveType get() = when (this) {
    Byte::class.java, java.lang.Byte::class.java -> JVMPrimitive.Byte
    Short::class.java, java.lang.Short::class.java -> JVMPrimitive.Short
    Int::class.java, java.lang.Integer::class.java -> JVMPrimitive.Int
    Long::class.java, java.lang.Long::class.java -> JVMPrimitive.Long
    Boolean::class.java, java.lang.Boolean::class.java -> JVMPrimitive.Boolean
    Char::class.java, java.lang.Character::class.java -> JVMPrimitive.Char
    Float::class.java, java.lang.Float::class.java -> JVMPrimitive.Float
    Double::class.java, java.lang.Double::class.java -> JVMPrimitive.Double
    Void::class.java -> JVMPrimitive.Void
    else -> null
}

internal val Class<*>.jvmType: String get() {
    return if (this.isArray)
        "[${this.componentType!!.jvmType}"
    else
        this.jvmPrimitiveType?.shortName ?: canonicalName.toJVMType()
}

internal val Class<*>.jvmSignature: String get() {
    return if (this.isArray)
        "[${this.componentType!!.jvmSignature}"
    else
        jvmType.toJVMSignature()
}

internal fun computeMethodJVMSignature(argumentTypes: List<Class<*>>, returnType: Class<*>) =
        "(${argumentTypes.joinToString(separator = "") { it.jvmSignature }})${returnType.jvmSignature}"
