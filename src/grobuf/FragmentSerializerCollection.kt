package grobuf

import grobuf.serializers.*
import java.lang.reflect.Type
import java.util.*

internal class DynamicClassesLoader(parent: ClassLoader?) : ClassLoader(parent) {
    fun loadClass(name: String, byteCode: ByteArray): Class<*> {
        return defineClass(name, byteCode, 0, byteCode.size)
    }
}

private sealed class BuildState {
    class Building(val classType: JVMType): BuildState()
    class Built(val inst: FragmentSerializer<*>, val requiredSerializers: List<Type>): BuildState()
    class Initialized(val inst: FragmentSerializer<*>): BuildState()
}

internal class FragmentSerializerFactoryImpl(val collection: FragmentSerializerCollection,
                                             val baseSerializerBuilder: () -> FragmentSerializer<Any?>)
    : FragmentSerializerFactory {

    override val baseSerializer by lazy { baseSerializerBuilder() }

    @Suppress("UNCHECKED_CAST")
    override fun getSerializer(type: Type) =
            collection.getFragmentSerializer(type) as FragmentSerializer<Any?>

}

internal class FragmentSerializerCollection(val classLoader: DynamicClassesLoader,
                                            val dataMembersExtractor: DataMembersExtractor,
                                            val customSerializerCollection: CustomSerializerCollection?) {

    private val serializers = concurrentMapOf<Type, BuildState>(
            String::class.java to BuildState.Initialized(StringSerializer()),
            Date::class.java to BuildState.Initialized(DateSerializer()),
            UUID::class.java to BuildState.Initialized(GuidSerializer()),
            Decimal::class.java to BuildState.Built(DecimalSerializer(), DecimalSerializer.requiredTypes),
            Any::class.java to BuildState.Built(AnySerializer(), AnySerializer.requiredTypes)
    )
    private val serializerToTypeMap = mutableMapOf<JVMType, Type>()

    init {
        serializers.forEach { type, writer ->
            val inst = when (writer) {
                is BuildState.Building -> error("Prewritten serializers cannot be being built")
                is BuildState.Built -> writer.inst
                is BuildState.Initialized -> writer.inst
            }
            serializerToTypeMap += inst::class.jvmType to type
        }
    }

    fun getFragmentSerializerType(type: Type): JVMType {
        val buildState = serializers[type]
        return when (buildState) {
            null -> getFragmentSerializer(type)::class.jvmType
            is BuildState.Building -> buildState.classType
            is BuildState.Built -> buildState.inst::class.jvmType
            is BuildState.Initialized -> buildState.inst::class.jvmType
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getFragmentSerializer(vararg typeArguments: Type) =
            getFragmentSerializer(sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl.make(
                    T::class.java, typeArguments, null)) as FragmentSerializer<T>

    private val lockObject = Any()

    fun getFragmentSerializer(type: Type): FragmentSerializer<*> {
        val state = serializers[type]
        if (state is BuildState.Initialized)
            return state.inst
        synchronized(lockObject) {
            @Suppress("NAME_SHADOWING")
            val state = serializers[type]
            when (state) {
                null -> return buildSerializer(type)

                is BuildState.Built -> {
                    initialize(type)
                    return state.inst
                }

                is BuildState.Initialized -> return state.inst

                else -> error("Writer for $type is not initialized")
            }
        }
    }

    private fun buildSerializer(type: Type) = type.klass.let {
        when {
            it.jvmPrimitiveType != null ->
                buildSerializer(type, PrimitivesSerializerBuilder(classLoader, this, type))

            it.isArray ->
                if (it.componentType.jvmPrimitiveType != null)
                    buildSerializer(type, PrimitivesArraySerializerBuilder(classLoader, this, type))
                else
                    buildSerializer(type, ArraySerializerBuilder(classLoader, this, type))

            it.jvmType.isBox ->
                buildSerializer(type, BoxesSerializerBuilder(classLoader, this, type))

            it.isEnum ->
                buildSerializer(type, EnumSerializerBuilder(classLoader, this, type))

            it.superclass == Tuple::class.java ->
                buildSerializer(type, TupleSerializerBuilder(classLoader, this, type))

            it == ArrayList::class.java ->
                initSerializer(type, listOf(type.typeArgumentAt(0)), ListSerializer())

            it == HashSet::class.java ->
                initSerializer(type, listOf(type.typeArgumentAt(0)), HashSetSerializer())

            it == TreeSet::class.java ->
                initSerializer(type, listOf(type.typeArgumentAt(0)), TreeSetSerializer())

            it == HashMap::class.java ->
                initSerializer(type, listOf(type.typeArgumentAt(0), type.typeArgumentAt(1)), HashMapSerializer())

            it == LinkedHashMap::class.java ->
                initSerializer(type, listOf(type.typeArgumentAt(0), type.typeArgumentAt(1)), LinkedHashMapSerializer())

            it == TreeMap::class.java ->
                initSerializer(type, listOf(type.typeArgumentAt(0), type.typeArgumentAt(1)), TreeMapSerializer())

            else -> {
                @Suppress("UNCHECKED_CAST")
                val customSerializer = customSerializerCollection?.get(
                        type,
                        FragmentSerializerFactoryImpl(this) {
                            buildSerializer(type, ClassSerializerBuilder(classLoader, this, type)) as FragmentSerializer<Any?>
                        })
                if (customSerializer != null)
                    //initSerializer(type, emptyList(), CustomSerializer(customSerializer))
                    buildSerializer(type, CustomSerializerBuilder(classLoader, this, type, customSerializer))
                else
                    buildSerializer(type, ClassSerializerBuilder(classLoader, this, type))
            }
        }
    }

    private fun Type.typeArgumentAt(index: Int) = typeArguments.elementAtOrNull(index) ?: Any::class.java

    private fun buildSerializer(type: Type, builder: ClassBuilder<FragmentSerializer<*>>): FragmentSerializer<*> {
        serializers[type] = BuildState.Building(builder.classType)
        serializerToTypeMap[builder.classType] = type
        val serializer = builder.build()
        val requiredSerializers = builder.argumentsOfInitialize.map { serializerToTypeMap[it]!! }
        serializers[type] = BuildState.Built(serializer, requiredSerializers)
        initialize(type)
        return serializer
    }

    private fun initSerializer(type: Type, requiredSerializers: List<Type>, serializer: FragmentSerializer<*>)
            : FragmentSerializer<*> {
        serializerToTypeMap[serializer::class.jvmType] = type
        serializers[type] = BuildState.Built(serializer, requiredSerializers)
        initialize(type)
        return serializer
    }

    private fun initialize(type: Type) {
        dfs(type, mutableSetOf())
    }

    private fun dfs(type: Type, visited: MutableSet<Type>) {
        visited.add(type)
        val state = serializers[type]
        when (state) {
            null -> {
                getFragmentSerializerType(type)
                return
            }

            is BuildState.Building -> return
            is BuildState.Initialized -> return

            is BuildState.Built -> {
                val requiredSerializers = state.requiredSerializers
                requiredSerializers.forEach {
                    if (!visited.contains(it))
                        dfs(it, visited)
                }
                val instances = requiredSerializers.map {
                    if (it == type)
                        state.inst // A loop.
                    else {
                        val requiredSerializerState = serializers[it]
                        when (requiredSerializerState) {
                            null -> {
                                getFragmentSerializerType(it)
                                return
                            }

                            is BuildState.Building -> return // A cycle.
                            is BuildState.Built -> requiredSerializerState.inst
                            is BuildState.Initialized -> requiredSerializerState.inst
                        }
                    }
                }
                state.inst.initialize(instances.toTypedArray())
                serializers[type] = BuildState.Initialized(state.inst)
            }
        }
    }
}