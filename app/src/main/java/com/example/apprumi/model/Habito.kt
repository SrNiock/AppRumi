package com.example.apprumi.model


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habitos")
data class Habito(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val motivo: String,
    val dificultad: Dificultad,
    val completado: Boolean = false,
    val tipoEstadistica: String = "GENERAL", // <--- Asegúrate de tener esto
    val diasRecurrencia: String = "1,2,3,4,5,6,7",
    val duracionMinutos: Int = 0,
    val playlistIds: String = "" // Guardaremos IDs: "101,102,105"
)