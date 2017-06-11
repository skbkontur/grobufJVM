package grobuf

import grobuf.serializers.*

private sealed class BuildState {
    class Building(val className: String): BuildState()
    class Built(val inst: Any, val requiredBuilders: List<String>): BuildState()
    class Initialized(val inst: Any): BuildState()
}

internal class FragmentSerializerCollection {
    private val serializers = mutableMapOf<String, BuildState>(
            String::class.jvmType to BuildState.Initialized(StringSerializer())
    )
    private val serializerToTypeMap = mutableMapOf<String, String>()

    init {
        serializers.forEach { type, writer ->
            serializerToTypeMap += (writer as BuildState.Initialized).inst::class.jvmType to type
        }
    }

    fun getFragmentSerializerType(klass: Class<*>): String {
        val canonicalName = klass.jvmType
        val buildState = serializers[canonicalName]
        return when (buildState) {
            null -> getFragmentSerializer(klass)::class.jvmType
            is BuildState.Building -> buildState.className
            is BuildState.Built -> buildState.inst::class.jvmType
            is BuildState.Initialized -> buildState.inst::class.jvmType
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getFragmentSerializer() = getFragmentSerializer(T::class.java) as FragmentSerializer<T>

    fun getFragmentSerializer(klass: Class<*>): FragmentSerializer<*> {
        val type = klass.jvmType
        val state = serializers[type]
        when (state) {
            null -> {
                val builder = getSerializerBuilder(klass)
                serializers[type] = BuildState.Building(builder.className)
                serializerToTypeMap[builder.className] = type
                val fragmentSerializer = builder.build()
                serializers[type] = BuildState.Built(fragmentSerializer, builder.argumentsOfInitialize.map { serializerToTypeMap[it]!! })
                initialize(type)
                return fragmentSerializer
            }
            is BuildState.Initialized -> return state.inst as FragmentSerializer<*>
            else -> throw IllegalStateException("Writer for $type is not initialized")
        }
    }

    private fun getSerializerBuilder(klass: Class<*>) = when {
        klass.jvmPrimitiveType != null ->
            PrimitivesSerializerBuilder(this, klass)

        klass.isArray ->
            if (klass.componentType.jvmPrimitiveType != null)
                PrimitivesArraySerializerBuilder(this, klass)
            else
                ArraySerializerBuilder(this, klass)

        else -> ClassSerializerBuilder(this, klass)
    }

    private fun initialize(type: String) {
        dfs(type, mutableSetOf())
    }

    private fun dfs(type: String, visited: MutableSet<String>) {
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