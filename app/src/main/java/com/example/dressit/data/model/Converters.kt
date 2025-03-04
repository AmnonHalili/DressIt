package com.example.dressit.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromGeoPoint(value: GeoPoint?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toGeoPoint(value: String?): GeoPoint? {
        if (value == null) return null
        return gson.fromJson(value, GeoPoint::class.java)
    }

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
} 