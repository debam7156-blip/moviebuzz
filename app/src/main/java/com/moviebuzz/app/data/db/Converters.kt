package com.moviebuzz.app.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    private val stringList = ListSerializer(String.serializer())

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        value?.let { json.encodeToString(stringList, it) } ?: "[]"

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.let { runCatching { json.decodeFromString(stringList, it) }.getOrNull() } ?: emptyList()
}
