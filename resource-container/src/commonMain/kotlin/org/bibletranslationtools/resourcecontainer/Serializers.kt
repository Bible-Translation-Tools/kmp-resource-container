package org.bibletranslationtools.resourcecontainer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.modules.SerializersModule

val json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true

    serializersModule = SerializersModule {
        contextual(String::class, IntAsStringSerializer)
    }
}

val yaml = ObjectMapper(YAMLFactory())

object IntAsStringSerializer : JsonTransformingSerializer<String>(String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonPrimitive -> JsonPrimitive(element.content)
            else -> element
        }
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        return element
    }
}