package com.example.apprumi.data.local

import androidx.room.TypeConverter
import com.example.apprumi.model.Dificultad

class Converters {
    @TypeConverter
    fun fromDificultad(value: Dificultad) = value.name

    @TypeConverter
    fun toDificultad(value: String) = Dificultad.valueOf(value)
}