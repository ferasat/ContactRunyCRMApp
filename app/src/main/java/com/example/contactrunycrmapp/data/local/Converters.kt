package com.example.contactrunycrmapp.data.local

import androidx.room.TypeConverter
import com.example.contactrunycrmapp.domain.model.SyncStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun listToString(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun fromStatus(value: String?): SyncStatus {
        return value?.let { SyncStatus.valueOf(it) } ?: SyncStatus.NEW
    }

    @TypeConverter
    fun statusToString(status: SyncStatus?): String {
        return status?.name ?: SyncStatus.NEW.name
    }
}
