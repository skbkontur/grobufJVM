package grobuf

import grobuf.serializers.*
import java.lang.reflect.Type
import java.util.*

internal class DynamicClassesLoader : ClassLoader() {
    fun loadClass(name: String, byteCode: ByteArray): Class<*> {
        return defineClass(name, byteCode, 0, byteCode.size)
    }
}

private sealed class BuildState {
    class Building(val classType: JVMType): BuildState()
    class Built(val inst: FragmentSerializer<*>, val requiredSerializers: List<Type>): BuildState()
    class Initialized(val inst: FragmentSerializer<*>): BuildState()
}

internal class FragmentSerializerCollection(val classLoader: DynamicClassesLoader) {
    private val serializers = concurrentMapOf<Type, BuildState>(
            String::class.java to BuildState.Initialized(StringSerializer()),
            Date::class.java to BuildState.Initialized(DateSerializer()),
            UUID::class.java to BuildState.Initialized(GuidSerializer()),
            Decimal::class.java to BuildState.Built(DecimalSerializer(), DecimalSerializer.requiredTypes)
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
                    state.requiredSerializers.forEach { getFragmentSerializerType(it) }
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

            it == HashMap::class.java ->
                initSerializer(type, getArgumentsForMap(type), HashMapSerializer())

            it == LinkedHashMap::class.java ->
                initSerializer(type, getArgumentsForMap(type), LinkedHashMapSerializer())

            it == TreeMap::class.java ->
                initSerializer(type, getArgumentsForMap(type), TreeMapSerializer())

            else -> buildSerializer(type, ClassSerializerBuilder(classLoader, this, type))
        }
    }

    private fun getArgumentsForMap(type: Type) = type.typeArguments.let {
        listOf(
                it.elementAtOrNull(0) ?: Any::class.java,
                it.elementAtOrNull(1) ?: Any::class.java
        )
    }

    private fun buildSerializer(type: Type, builder: ClassBuilder<FragmentSerializer<*>>): FragmentSerializer<*> {
        serializers[type] = BuildState.Building(builder.classType)
        serializerToTypeMap[builder.classType] = type
        val serializer = builder.build()
        serializers[type] = BuildState.Built(serializer,
                builder.argumentsOfInitialize.map { serializerToTypeMap[it]!! }
        )
        initialize(type)
        return serializer
    }

    private fun initSerializer(type: Type, requiredSerializers: List<Type>, serializer: FragmentSerializer<*>)
            : FragmentSerializer<*> {
        serializerToTypeMap[serializer::class.jvmType] = type
        serializers[type] = BuildState.Built(serializer, requiredSerializers)
        requiredSerializers.forEach{ getFragmentSerializerType(it) }
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
            null -> error("Serializer for $type has not been created")
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
                            null -> error("Serializer for $it has not been created")
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