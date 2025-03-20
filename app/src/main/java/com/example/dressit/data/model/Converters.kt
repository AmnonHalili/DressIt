package com.example.dressit.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromCommentList(value: List<Comment>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCommentList(value: String?): List<Comment>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Comment>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
} 