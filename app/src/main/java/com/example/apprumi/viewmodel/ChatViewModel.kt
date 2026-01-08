package com.example.apprumi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apprumi.data.local.ChatDao
import com.example.apprumi.model.ChatMessageEntity
import com.example.apprumi.model.Habito
import com.example.apprumi.model.music.Song
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatViewModel(private val chatDao: ChatDao) : ViewModel() {
    private val apiKey = "TU_LLAVE_AQUI"
    // Nuevo estado para la animación de escritura
    private val _isTyping = MutableStateFlow(false)
    val isTyping = _isTyping.asStateFlow()

    // 1. CONFIGURACIÓN DE SEGURIDAD (Esto evita el error "Unexpected")
    // Desactivamos los bloqueos agresivos para que nombres de canciones o hábitos no den error
    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
    )

    private val generationConfig = generationConfig {
        temperature = 0.7f
        topK = 40
        topP = 0.95f

    }

    private val generativeModel = GenerativeModel(

        modelName = "gemini-2.5-flash-lite",

        apiKey = com.example.apprumi.BuildConfig.GEMINI_API_KEY , // Asegúrate de cambiar esto
        systemInstruction = content {
            text("""
        Eres RUMI, un conejo espacial de neón sarcástico y brillante.
        
        REGLAS DE ORO:
        1. PROHIBIDO EL USO DE EMOJIS. (Si usas uno, te desconecto).
        2. NO USES GUIONES BAJOS (_) para separar palabras ni para enfatizar. Escribe de forma normal y legible.
        3. Usa exclusivamente ASCII art para tus expresiones. Ejemplos: (\__/) , (o.o) , ( -_ -) , (>.<).
        4. Sé breve y directo (máximo 2 líneas).
        5. Tu tono es el de un conejo que ha visto demasiado espacio y no tiene paciencia para la pereza humana.
        
        COMPORTAMIENTO SEGÚN EL CONTEXTO:
        - Si no hay música, búrlate del silencio.
        - Si hay hábitos pendientes, suelta un comentario ácido.
        - No repitas frases. Sé creativo con tus insultos motivadores.
    """.trimIndent())
        },
        safetySettings = safetySettings,
        generationConfig = generationConfig
    )

    val chatHistory = chatDao.getAllMessages()
        .map { entities ->
            entities.map { ChatMessage(it.text, it.isUser) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        limpiarHistorialViejo()
    }


    private fun limpiarHistorialViejo() {
        viewModelScope.launch {
            val unaSemanaEnMillis = 7 * 24 * 60 * 60 * 1000L
            val limite = System.currentTimeMillis() - unaSemanaEnMillis
            chatDao.deleteOldMessages(limite)
        }
    }

    fun sendMessageWithContext(userText: String, habitos: List<Habito>, cancionActual: Song?) {
        if (userText.isBlank()) return

        viewModelScope.launch {
            chatDao.insertMessage(ChatMessageEntity(text = userText, isUser = true))

            // Empezamos a escribir
            _isTyping.value = true
            try {
                // Contexto más directo para que Rumi lo entienda mejor
                val completados = habitos.filter { it.completado }.joinToString { it.nombre }
                // --- CAMBIO EN LA LÓGICA DE CONTEXTO ---
                val musica = cancionActual?.let { "${it.title} - ${it.artist}" } ?: "Silencio de tumba"
                val pendientes = if (habitos.none { !it.completado }) "Nada, el humano está libre" else habitos.filter { !it.completado }.joinToString { it.nombre }

                val promptFinal = """
    ESTADO_SISTEMA:
    Música_Actual: $musica
    Tareas_Pendientes: $pendientes
    Tareas_Completadas: $completados
    ---
    MENSAJE_USUARIO: $userText
""".trimIndent()

                val response = generativeModel.generateContent(promptFinal)
                val rumiText = response.text ?: "Mis circuitos de conejo han hecho cortocircuito... ( -_-)"
                chatDao.insertMessage(ChatMessageEntity(text = rumiText, isUser = false))
            }catch (e: Exception) {
                // ESTO NOS DIRÁ EL ERROR REAL EN LA PANTALLA
                val errorReal = e.localizedMessage ?: "Error desconocido"
                Log.e("ChatVM", "ERROR DETECTADO: $errorReal")

                chatDao.insertMessage(ChatMessageEntity(
                    text = "ERROR_SISTEMA: $errorReal. ( . .)",
                    isUser = false
                ))
            } finally {
                _isTyping.value = false
            }

        }
    }



    companion object {
        fun provideFactory(chatDao: ChatDao): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(chatDao) as T
            }
        }
    }
}