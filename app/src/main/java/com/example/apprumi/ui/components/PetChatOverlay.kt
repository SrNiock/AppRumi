package com.example.apprumi.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.apprumi.ui.screens.ArcadeDark
import com.example.apprumi.ui.screens.ArcadeTextStyle
import com.example.apprumi.ui.screens.ElectricCyan
import com.example.apprumi.ui.screens.GlassWhite
import com.example.apprumi.ui.screens.SoftPurple
import com.example.apprumi.viewmodel.ChatMessage
import com.example.apprumi.viewmodel.ChatViewModel

@Composable
fun PetChatOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    chatViewModel: ChatViewModel,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val history by chatViewModel.chatHistory.collectAsStateWithLifecycle()
    val isTyping by chatViewModel.isTyping.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    // --- LÓGICA DE AUTO-SCROLL ---
    // Se activa cada vez que cambia el tamaño del historial o el estado de escritura
    LaunchedEffect(history.size, isTyping) {
        if (history.isNotEmpty() || isTyping) {
            // Calculamos el índice del último elemento
            // Si está escribiendo, el último es el indicador (index = history.size)
            // Si no, es el último mensaje (index = history.size - 1)
            val lastIndex = if (isTyping) history.size else (history.size - 1).coerceAtLeast(0)

            listState.animateScrollToItem(lastIndex)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.7f))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount -> if (dragAmount > 30) onDismiss() }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
                Spacer(modifier = Modifier.height(160.dp))

                Text(
                    "SISTEMA RUMI",
                    style = ArcadeTextStyle.copy(
                        fontSize = 34.sp,
                        brush = Brush.linearGradient(listOf(ElectricCyan, GlassWhite))
                    )
                )

                LazyColumn(
                    state = listState, // El estado vinculado al scroll
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp)
                ) {
                    items(history) { msg ->
                        ChatBubbleLiquid(msg)
                    }
                    if (isTyping) {
                        item { TypingIndicator() }
                    }
                }

                // Input Box estilo "Liquid Glass"
                Surface(
                    modifier = Modifier.padding(bottom = 40.dp).fillMaxWidth(),
                    color = Color.White.copy(0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(ElectricCyan),
                            decorationBox = { inner ->
                                if (inputText.isEmpty()) Text("Comando...", color = Color.White.copy(0.3f))
                                inner()
                            }
                        )
                        IconButton(
                            onClick = {
                                if(inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Send, null, tint = ElectricCyan)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ChatBubbleLiquid(msg: ChatMessage) {
    // Usamos el mismo diseño que HabitItemDB
    Surface(
        color = if (msg.isUser) Color.White.copy(0.05f) else ElectricCyan.copy(0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (msg.isUser) Color.White.copy(0.1f) else ElectricCyan.copy(0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (msg.isUser) "OPERADOR" else "RUMI_OS",
                style = TextStyle(color = if(msg.isUser) Color.Gray else ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = msg.text,
                style = TextStyle(color = Color.White, fontSize = 15.sp, lineHeight = 20.sp)
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Row(
        modifier = Modifier.alpha(alpha).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "(>.< )  PROCESANDO...",
            color = ElectricCyan,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}