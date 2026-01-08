package com.example.apprumi.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Modelo para la Base de Datos
@Entity(tableName = "pet_status")
data class PetStatusEntity(
    @PrimaryKey val id: Int = 1,
    val animo: Float = 1f,
    val salud: Float = 1f,
    val aseo: Float = 1f,
    val ultimaAccion: Long = System.currentTimeMillis() // <--- NUEVO CAMPO
)