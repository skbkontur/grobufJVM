package grobuf

import grobuf.serializers.*
import java.lang.reflect.Type

internal class DynamicClassesLoader : ClassLoader() {
    fun loadClass(name: String, byteCode: ByteArray): Class<*> {
        return defineClass(name, byteCode, 0, byteCode.size)
    }
}

private sealed class BuildState {
    class Building(val classType: JVMType): BuildState()
    class Built(val inst: Any, val requiredBuilders: List<Type>): BuildState()
    class Initialized(val inst: Any): BuildState()
}

internal class FragmentSerializerCollection(val classLoader: DynamicClassesLoader) {
    private val serializers = concurrentMapOf<Type, BuildState>(
            String::class.java to BuildState.Initialized(StringSerializer())
    )
    private val serializerToTypeMap = mutableMapOf<JVMType, Type>()

    init {
        serializers.forEach { type, writer ->
            serializerToTypeMap += (writer as BuildState.Initialized).inst::class.jvmType to type
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
            return state.inst as FragmentSerializer<*>
        synchronized(lockObject) {
            @Suppress("NAME_SHADOWING")
            val state = serializers[type]
            when (state) {
                null -> {
                    val builder = getSerializerBuilder(type)
                    serializers[type] = BuildState.Building(builder.classType)
                    serializerToTypeMap[builder.classType] = type
                    val fragmentSerializer = builder.build()
                    serializers[type] = BuildState.Built(fragmentSerializer,
                            builder.argumentsOfInitialize.map { serializerToTypeMap[it]!! }
                    )
                    initialize(type)
                    return fragmentSerializer
                }
                is BuildState.Initialized -> return state.inst as FragmentSerializer<*>
                else -> throw IllegalStateException("Writer for $type is not initialized")
            }
        }
    }

    private fun getSerializerBuilder(type: Type) = type.klass.let {
        when {
            it.jvmPrimitiveType != null ->
                PrimitivesSerializerBuilder(classLoader, this, type)

            it.isArray ->
                if (it.componentType.jvmPrimitiveType != null)
                    PrimitivesArraySerializerBuilder(classLoader, this, type)
                else
                    ArraySerializerBuilder(classLoader, this, type)

            it.jvmType.isBox ->
                BoxesSerializerBuilder(classLoader, this, type)

            it.isEnum ->
                EnumSerializerBuilder(classLoader, this, type)

            it.superclass == Tuple::class.java ->
                TupleSerializerBuilder(classLoader, this, type)

            else -> ClassSerializerBuilder(classLoader, this, type)
        }
    }

    private fun initialize(type: Type) {
        dfs(type, mutableSetOf())
    }

    private fun dfs(type: Type, visited: MutableSet<Type>) {
        visited.add(type)
        val state = serializers[type]
        when (state) {
            null -> throw IllegalStateException("Serializer for $type has not been created")
            is BuildState.Building -> return
            is BuildState.Initialized -> return

            is BuildState.Built -> {
                val requiredBuilders = state.requiredBuilders
                requiredBuilders.forEach {
                    if (!visited.contains(it))
                        dfs(it, visited)
                }
                val instances = requiredBuilders.map {
                    if (it == type)
                        state.inst // A loop.
                    else {
                        val requiredBuilderState = serializers[it]
                        when (requiredBuilderState) {
                            null -> throw IllegalStateException("Serializer for $it has not been created")
                            is BuildState.Building -> return // A cycle.
                            is BuildState.Built -> requiredBuilderState.inst
                            is BuildState.Initialized -> requiredBuilderState.inst
                        }
                    }
                }
                val initializeMethod = state.inst::class.java.getMethod("initialize", Array<Any?>::class.java)
                initializeMethod.invoke(state.inst, instances.toTypedArray())
                serializers[type] = BuildState.Initialized(state.inst)
            }
        }
    }
}