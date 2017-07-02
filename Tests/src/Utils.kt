import grobuf.Decimal
import sun.misc.Unsafe
import java.lang.reflect.Modifier
import java.util.*

private val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe").let {
    it.isAccessible = true
    it.get(null) as Unsafe
}

inline fun <reified T> generateRandomTrash(random: Random,
                                           fillRate: Int, stringsLength: Int, arraysSize: Int) =
        generateRandomTrash(T::class.java, random, fillRate, stringsLength, arraysSize)

fun <T> generateRandomTrash(klass: Class<T>, random: Random,
                            fillRate: Int, stringsLength: Int, arraysSize: Int): T {
    val result = theUnsafe.allocateInstance(klass)
    fillWithRandomTrash(result, random, fillRate, stringsLength, arraysSize)
    return result as T
}

fun deepPrint(obj: Any?): String {
    val result = StringBuilder()
    DeepPrinter(result).print(obj)
    return result.toString()
}

private class DeepPrinter(val output: StringBuilder) {

    var indent = 0

    private fun indented(block: () -> Unit) {
        indent += 2
        block()
        indent -= 2
    }

    fun out(s: String) {
        for (i in 0 until indent)
            output.append(' ')
        output.appendln(s)
    }

    fun print(obj: Any?) {
        if (obj == null) {
            out("null")
            return
        }
        val klass = obj::class.java
        if (klass.isALeaf) {
            when (klass) {
                ByteArray::class.java -> out((obj as ByteArray).contentToString())
                ShortArray::class.java -> out((obj as ShortArray).contentToString())
                IntArray::class.java -> out((obj as IntArray).contentToString())
                LongArray::class.java -> out((obj as LongArray).contentToString())
                BooleanArray::class.java -> out((obj as BooleanArray).contentToString())
                CharArray::class.java -> out((obj as CharArray).contentToString())
                FloatArray::class.java -> out((obj as FloatArray).contentToString())
                DoubleArray::class.java -> out((obj as DoubleArray).contentToString())
                else -> out(obj.toString())
            }
        } else if (!klass.isArray) {
            val fields = klass.fields
                    .filter { Modifier.isPublic(it.modifiers) }
                    .filterNot { Modifier.isStatic(it.modifiers) }
                    .filterNot { Modifier.isFinal(it.modifiers) }
                    .sortedBy { it.name }
            for (field in fields) {
                out("<field ${field.name}>")
                indented {
                    print(field.get(obj))
                }
            }
        } else {
            obj as Array<Any?>
            obj.forEachIndexed { index, item ->
                out("<item #$index>")
                indented {
                    print(item)
                }
            }
        }
    }
}

private fun fillWithRandomTrash(obj: Any, random: Random, fillRate: Int, stringsLength: Int, arraysSize: Int) {
    val fields = obj::class.java.fields
            .filter { Modifier.isPublic(it.modifiers) }
            .filterNot { Modifier.isStatic(it.modifiers) }
            .filterNot { Modifier.isFinal(it.modifiers) }
    for (field in fields) {
        if (!field.type.isPrimitive && random.nextInt(101) > fillRate)
            continue
        val type = field.type
        val fieldValue = if (type.isALeaf) {
            getRandomValue(type, random, stringsLength, arraysSize)
        } else if (!type.isArray) {
            theUnsafe.allocateInstance(type).also {
                fillWithRandomTrash(it, random, fillRate, stringsLength, arraysSize)
            }
        } else {
            val elementType = type.componentType!!
            val size = random.nextInt(arraysSize) + arraysSize
            (java.lang.reflect.Array.newInstance(elementType, size) as Array<Any?>).also {
                for (index in 0 until size) {
                    it[index] = if (random.nextInt(101) > fillRate)
                        null
                    else {
                        if (elementType.isALeaf)
                            getRandomValue(elementType, random, stringsLength, arraysSize)
                        else {
                            theUnsafe.allocateInstance(elementType).also {
                                fillWithRandomTrash(it, random, fillRate, stringsLength, arraysSize)
                            }
                        }
                    }
                }
            }
        }
        field.set(obj, fieldValue)
    }
}

private val leafTypes = setOf(
        Byte::class.java,
        Short::class.java,
        Int::class.java,
        Long::class.java,
        Boolean::class.java,
        Char::class.java,
        Float::class.java,
        Double::class.java,
        java.lang.Byte::class.java,
        java.lang.Short::class.java,
        java.lang.Integer::class.java,
        java.lang.Long::class.java,
        java.lang.Boolean::class.java,
        java.lang.Character::class.java,
        java.lang.Float::class.java,
        java.lang.Double::class.java,
        ByteArray::class.java,
        ShortArray::class.java,
        IntArray::class.java,
        LongArray::class.java,
        BooleanArray::class.java,
        CharArray::class.java,
        FloatArray::class.java,
        DoubleArray::class.java,
        String::class.java,
        Date::class.java,
        UUID::class.java,
        Decimal::class.java
)

private val Class<*>.isALeaf: Boolean get() = leafTypes.contains(this)

private fun Random.nextByte() = nextInt().toByte()
private fun Random.nextShort() = nextInt().toShort()
private fun Random.nextChar() = nextInt().toChar()

private fun getRandomValue(klass: Class<*>, random: Random,
                           stringsLength: Int, arraysSize: Int): Any = when (klass) {
    Byte::class.java,
    java.lang.Byte::class.java -> random.nextByte()

    Short::class.java,
    java.lang.Short::class.java -> random.nextShort()

    Int::class.java,
    java.lang.Integer::class.java -> random.nextInt()

    Long::class.java,
    java.lang.Long::class.java -> random.nextLong()

    Boolean::class.java,
    java.lang.Boolean::class.java -> random.nextBoolean()

    Char::class.java,
    java.lang.Character::class.java -> random.nextChar()

    Float::class.java,
    java.lang.Float::class.java -> random.nextFloat()

    Double::class.java,
    java.lang.Double::class.java -> random.nextDouble()

    ByteArray::class.java -> ByteArray(arraysSize + random.nextInt(arraysSize), { random.nextByte() })

    ShortArray::class.java -> ShortArray(arraysSize + random.nextInt(arraysSize), { random.nextShort() })

    IntArray::class.java -> IntArray(arraysSize + random.nextInt(arraysSize), { random.nextInt() })

    LongArray::class.java -> LongArray(arraysSize + random.nextInt(arraysSize), { random.nextLong() })

    BooleanArray::class.java -> BooleanArray(arraysSize + random.nextInt(arraysSize), { random.nextBoolean() })

    CharArray::class.java -> CharArray(arraysSize + random.nextInt(arraysSize), { random.nextChar() })

    FloatArray::class.java -> FloatArray(arraysSize + random.nextInt(arraysSize), { random.nextFloat() })

    DoubleArray::class.java -> DoubleArray(arraysSize + random.nextInt(arraysSize), { random.nextDouble() })

    String::class.java -> String(CharArray(stringsLength + random.nextInt(stringsLength * 2), { 'a' + random.nextInt(26) }))

    Date::class.java -> Date(random.nextInt().toLong())

    UUID::class.java -> UUID(random.nextLong(), random.nextLong())

    Decimal::class.java -> Decimal.valueOf(random.nextDouble())

    else -> error("Invalid type: $klass")
}