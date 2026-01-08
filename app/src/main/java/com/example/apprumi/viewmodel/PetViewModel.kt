package com.example.apprumi.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apprumi.data.local.HabitoDao
import com.example.apprumi.model.Dificultad
import com.example.apprumi.model.Habito
import com.example.apprumi.model.PetStatusEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PetViewModel(private val dao: HabitoDao) : ViewModel() {

    // 1. Cargamos la lista de hábitos desde la BD
    val habitos: StateFlow<List<Habito>> = dao.getAllHabitos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 2. Definimos 'status' combinando los datos base con la lógica dinámica
    // ESTA ES LA VARIABLE QUE TE DABA ERROR
    val status: StateFlow<PetStatusEntity> = combine(
        dao.getPetStatus().map { it ?: PetStatusEntity() },
        habitos
    ) { pet, listaHabitos ->
        val total = listaHabitos.size
        val completados = listaHabitos.count { it.completado }

        // Salud dinámica: Porcentaje de misiones completadas
        val nuevaSalud = if (total > 0) completados.toFloat() / total.toFloat() else 1f
        // Aseo dinámico: Mismo concepto
        val nuevoAseo = if (total > 0) completados.toFloat() / total.toFloat() else 1f

        pet.copy(
            salud = nuevaSalud,
            aseo = nuevoAseo,
            animo = pet.animo // El ánimo es persistente en la BD
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PetStatusEntity()
    )

    init {
        viewModelScope.launch {
            // 1. Verificamos si ya existe la mascota en la BD
            val pet = dao.getPetStatusSync()
            if (pet == null) {
                // Si no existe, la creamos por primera vez
                dao.upsertPetStatus(PetStatusEntity(
                    id = 1,
                    animo = 1f,
                    salud = 1f,
                    aseo = 1f,
                    ultimaAccion = System.currentTimeMillis()
                ))
            } else {
                // Si ya existe, comprobamos si merece castigo
                verificarCastigoPorInactividad()
            }
        }
    }

    private fun verificarCastigoPorInactividad() {
        viewModelScope.launch {
            // Esperamos un poco para que la BD esté lista
            delay(500)
            val petActual = dao.getPetStatusSync() ?: return@launch
            val ahora = System.currentTimeMillis()
            val unDiaEnMillis = 24 * 60 * 60 * 1000L

            val tiempoTranscurrido = ahora - petActual.ultimaAccion

            if (tiempoTranscurrido > unDiaEnMillis) {
                val diasPerdidos = (tiempoTranscurrido / unDiaEnMillis).toInt()
                val penalizacion = 0.1f * diasPerdidos
                val nuevoAnimo = (petActual.animo - penalizacion).coerceAtLeast(0f)

                dao.upsertPetStatus(petActual.copy(
                    animo = nuevoAnimo,
                    ultimaAccion = ahora // Reseteamos el contador para no penalizar dos veces hoy
                ))
            }
        }
    }

    fun completarHabito(habito: Habito) {
        viewModelScope.launch {
            val nuevoEstado = !habito.completado
            dao.update(habito.copy(completado = nuevoEstado))

            if (nuevoEstado) {
                // Accedemos a .value de 'status' que ya está definido arriba
                val petActual = status.value
                val nuevoAnimo = (petActual.animo + 0.05f).coerceAtMost(1f)

                // Actualizamos ánimo y reseteamos fecha de última acción
                dao.upsertPetStatus(petActual.copy(
                    animo = nuevoAnimo,
                    ultimaAccion = System.currentTimeMillis()
                ))
            }
        }
    }


    // --- ESTADOS DE MISIÓN ---
    var activeMission by mutableStateOf<Habito?>(null)
        private set

    var secondsRemaining by mutableIntStateOf(0)
        private set

    var isMissionRunning by mutableStateOf(false)
        private set

    // NUEVO: Estado de pausa


    // NUEVO: Referencia al trabajo del temporizador para evitar duplicados


// En PetViewModel.kt


// En PetViewModel.kt


    private var timerJob: kotlinx.coroutines.Job? = null

    // --- DENTRO DE PETVIEWMODEL ---

    // 1. Declarar la variable de control (Asegúrate de que solo esté escrita UNA VEZ)
    private var currentMissionJob: kotlinx.coroutines.Job? = null

    // 2. Estado de pausa
    var isPaused by mutableStateOf(false)
        private set

    fun startMission(habito: Habito) {
        // Cancelamos cualquier trabajo previo usando la nueva variable
        currentMissionJob?.cancel()

        activeMission = habito
        secondsRemaining = habito.duracionMinutos * 60
        isMissionRunning = true
        isPaused = false

        // Asignamos el nuevo Job
        currentMissionJob = viewModelScope.launch {
            while (secondsRemaining > 0 && isMissionRunning) {
                delay(1000)
                if (!isPaused) {
                    secondsRemaining--
                }
            }
            if (secondsRemaining <= 0 && isMissionRunning) {
                finishMission()
            }
        }
    }

    fun togglePause() {
        isPaused = !isPaused
    }

    fun stopMission() {
        isMissionRunning = false
        isPaused = false
        activeMission = null
        // Cancelamos el Job específico
        currentMissionJob?.cancel()
    }

    // Actualiza también finishMission si la usas
    private fun finishMission() {
        isMissionRunning = false
        activeMission?.let { completarHabito(it) }
        activeMission = null
        currentMissionJob?.cancel()
    }
    fun agregarHabito(
        nombre: String,
        motivo: String,
        dificultad: Dificultad,
        duracion: Int,
        playlist: String
    ) {
        viewModelScope.launch {
            val nuevoHabito = Habito(
                nombre = nombre,
                motivo = motivo,
                dificultad = dificultad,
                completado = false,
                tipoEstadistica = "GENERAL", // Según tu modelo
                diasRecurrencia = "1,2,3,4,5,6,7", // Según tu modelo
                duracionMinutos = duracion,
                playlistIds = playlist
            )
            // Usamos la función 'insert' que definiste en tu DAO
            dao.insert(nuevoHabito)
        }
    }

    fun borrarHabito(habito: Habito) {
        viewModelScope.launch { dao.delete(habito) }
    }

    companion object {
        fun provideFactory(dao: HabitoDao): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PetViewModel(dao) as T
        }
    }
}