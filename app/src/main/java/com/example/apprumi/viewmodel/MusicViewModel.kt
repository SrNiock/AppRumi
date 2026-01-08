package com.example.apprumi.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apprumi.data.repository.MusicRepository
import com.example.apprumi.model.music.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()
    private val repository = MusicRepository(application)
    private var mediaPlayer: MediaPlayer? = null
    private var visualizer: Visualizer? = null
    private var progressJob: Job? = null

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _bassLevel = MutableStateFlow(0f)
    val bassLevel = _bassLevel.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    // --- NUEVOS ESTADOS PARA EL HUD ---
    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()
    // --- NUEVO: Estado para la cola actual ---
    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue = _currentQueue.asStateFlow()
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()

    fun loadLibrary() {
        viewModelScope.launch {
            _songs.value = repository.fetchLocalSongs()
            // Por defecto, la cola es toda la biblioteca
            if (_currentQueue.value.isEmpty()) _currentQueue.value = _songs.value
        }
    }

    fun playSong(song: Song, playlist: List<Song>? = null) {
        // Si pasamos una playlist (misión), actualizamos la cola.
        // Si no, y la cola está vacía, usamos la biblioteca.
        if (playlist != null) {
            _currentQueue.value = playlist
        } else if (_currentQueue.value.isEmpty()) {
            _currentQueue.value = _songs.value
        }

        mediaPlayer?.stop()
        mediaPlayer?.release()
        visualizer?.release()
        progressJob?.cancel()

        _currentSong.value = song

        mediaPlayer = MediaPlayer().apply {
            setDataSource(getApplication(), song.uri)
            prepareAsync()
            setOnPreparedListener {
                it.start()
                _isPlaying.value = true
                setupVisualizer(it.audioSessionId)
                startProgressTracker()
            }
            setOnCompletionListener { skipNext() }
        }
    }


    fun playMissionPlaylist(idsString: String) {
        if (idsString.isBlank()) return
        val ids = idsString.split(",").mapNotNull { it.toLongOrNull() }
        val playlist = _songs.value.filter { ids.contains(it.id) }

        if (playlist.isNotEmpty()) {
            // Reproducimos la primera e indicamos que ésta es la nueva cola
            playSong(playlist[0], playlist)
        }
    }
    // Dentro de tu MusicViewModel
    fun seekTo(progressFraction: Float) {
        mediaPlayer?.let { player ->
            val seekPosition = (progressFraction * player.duration).toInt()
            player.seekTo(seekPosition)
            // Actualizamos el flujo de progreso manualmente para respuesta instantánea
            _progress.value = progressFraction
        }
    }
    fun playQueue(idsString: String) {
        if (idsString.isBlank()) return
        val ids = idsString.split(",").mapNotNull { it.toLongOrNull() }
        val playlist = _songs.value.filter { ids.contains(it.id) }

        if (playlist.isNotEmpty()) {
            // Lógica para reproducir la primera y encolar el resto
            playSong(playlist[0])
            // Podrías añadir un sistema de cola aquí
        }
    }
    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            } else {
                it.start()
                _isPlaying.value = true
            }
        }
    }

    // --- NUEVAS FUNCIONES PARA EL HUD ---

    fun skipNext() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) return

        if (_isShuffle.value) {
            val randomIndex = Random.nextInt(queue.size)
            playSong(queue[randomIndex])
        } else {
            val index = queue.indexOf(_currentSong.value)
            if (index != -1 && index < queue.size - 1) {
                playSong(queue[index + 1])
            } else {
                playSong(queue[0]) // Volver al inicio de la cola
            }
        }
    }

    fun skipPrevious() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) return

        val index = queue.indexOf(_currentSong.value)
        if (index > 0) {
            playSong(queue[index - 1])
        } else {
            playSong(queue.last()) // Ir a la última de la cola
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleFavorite() {
        _isFavorite.value = !_isFavorite.value
        // NOTA: Aquí deberías llamar a tu repositorio para guardar
        // este estado en la base de datos de la canción actual.
    }

    private fun setupVisualizer(id: Int) {
        try {
            visualizer = Visualizer(id).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, w: ByteArray?, s: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, s: Int) {
                        if (fft != null && _isPlaying.value) {
                            val magnitude = Math.hypot(fft[2].toDouble(), fft[3].toDouble()).toFloat()
                            _bassLevel.value = (_bassLevel.value * 0.8f) + ((magnitude / 65f).coerceIn(0f, 1f) * 0.2f)
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startProgressTracker() {
        progressJob = viewModelScope.launch {
            while (true) {
                mediaPlayer?.let {
                    if (it.isPlaying && it.duration > 0) {
                        _progress.value = it.currentPosition.toFloat() / it.duration.toFloat()
                    }
                }
                delay(500)
            }
        }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        visualizer?.release()
        progressJob?.cancel()
        super.onCleared()
    }
}