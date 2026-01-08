package com.example.apprumi.data.local

import androidx.room.*
import com.example.apprumi.model.Habito
import com.example.apprumi.model.PetStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitoDao {

    // Asegúrate de que tu clase Habito tenga @Entity(tableName = "habitos")
    @Query("SELECT * FROM habitos ORDER BY completado ASC, id DESC")
    fun getAllHabitos(): Flow<List<Habito>>

    @Query("SELECT * FROM pet_status WHERE id = 1")
    suspend fun getPetStatusSync(): PetStatusEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habito: Habito)

    @Update
    suspend fun update(habito: Habito)

    @Delete
    suspend fun delete(habito: Habito)

    // CAMBIO: Usamos ID = 1 para que coincida con la Entity
    @Query("SELECT * FROM pet_status WHERE id = 1")
    fun getPetStatus(): Flow<PetStatusEntity?>

    // CAMBIO: Eliminamos 'savePetStatus' y dejamos solo 'upsertPetStatus'
    // @Upsert es más moderno y eficiente que @Insert(REPLACE)
    @Upsert
    suspend fun upsertPetStatus(status: PetStatusEntity)
}