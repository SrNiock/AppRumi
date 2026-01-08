package com.example.apprumi.ui.components

import android.content.Context
import android.os.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import com.example.apprumi.R
import com.example.apprumi.ui.screens.ElectricCyan
import com.example.apprumi.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PetIdleAnimation(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current

    // 1. VIBRADOR REFORZADO
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerVibration(type: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Usamos efectos predefinidos que son más difíciles de ignorar por el sistema
                val effect = if (type == "heavy") {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (type == "heavy") 80 else 40)
            }
        } catch (e: Exception) {
            // Si falla el efecto predefinido, fallback a vibración clásica
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    // --- ESTADOS ---
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val bass by musicViewModel.bassLevel.collectAsState()

    val idleRabbit = R.raw.bunny2
    val draggingRabbit = R.raw.sbunny
    val dancingRabbits = listOf(R.raw.medrab, R.raw.rabbit)

    var isDragging by remember { mutableStateOf(false) }
    var currentDancingIndex by remember { mutableIntStateOf(0) }
    var currentRabbitRes by remember { mutableIntStateOf(idleRabbit) }

    // Animación de Alpha con un valor inicial de 1f
    val bunnyAlpha = remember { Animatable(1f) }

    val targetRes = when {
        isDragging -> draggingRabbit
        isPlaying && currentSong != null -> dancingRabbits[currentDancingIndex]
        else -> idleRabbit
    }

    // --- LÓGICA DE TRANSICIÓN (REPARADA) ---
    LaunchedEffect(targetRes) {
        if (targetRes != currentRabbitRes) {
            // Si el cambio es por arrastre, la vibración es más ligera
            triggerVibration(if (isDragging) "light" else "heavy")

            // Efecto visual rápido
            bunnyAlpha.animateTo(0f, animationSpec = tween(150))
            currentRabbitRes = targetRes
            // Forzamos el regreso del alpha inmediatamente después de cambiar el recurso
            bunnyAlpha.animateTo(1f, animationSpec = tween(200))
        }
    }

    LaunchedEffect(currentSong) {
        if (isPlaying) {
            currentDancingIndex = (dancingRabbits.indices).random()
        }
    }

    Box(
        modifier = modifier
            .size(450.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ -> change.consume() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val rabbitComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(currentRabbitRes))
        val rabbitProgress by animateLottieCompositionAsState(
            composition = rabbitComposition,
            iterations = LottieConstants.IterateForever,
            isPlaying = true
        )

        LottieAnimation(
            composition = rabbitComposition,
            progress = { rabbitProgress },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Si el alpha es bajo (transición), encogemos un poco
                    val scaleFactor = 0.8f + (bunnyAlpha.value * 0.4f)
                    val pulse = bass * 0.15f
                    scaleX = scaleFactor + pulse
                    scaleY = scaleFactor + pulse
                    alpha = bunnyAlpha.value
                }
        )

        SpaceParticleBurst(
            triggerKey = currentRabbitRes,
            bass = bass
        )
    }
}

@Composable
fun SpaceParticleBurst(triggerKey: Any, bass: Float) {
    val progress = remember { Animatable(0f) }

    val burstStars = remember(triggerKey) {
        List(65) {
            val sizeTier = Random.nextFloat().pow(2.5f)
            object {
                val angle = Random.nextFloat() * 2 * PI.toFloat()
                val speedFactor = Random.nextFloat() * 0.7f + 0.3f
                val baseRadius = 0.5f + sizeTier * 6f
                val baseAlpha = 0.2f + (sizeTier * 0.7f)
            }
        }
    }

    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
        )
    }

    if (progress.value > 0f && progress.value < 1f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxExplosionDist = (size.width * 0.55f) * (1f + bass * 0.4f)

            burstStars.forEach { star ->
                val dist = progress.value * maxExplosionDist * star.speedFactor
                val x = centerX + cos(star.angle) * dist
                val y = centerY + sin(star.angle) * dist

                val alpha = (star.baseAlpha * (1f - progress.value)).coerceIn(0f, 1f)
                val radiusPx = (star.baseRadius * (1f - progress.value * 0.3f)).dp.toPx()
                val starColor = lerp(Color.White, ElectricCyan, progress.value)

                if (star.baseRadius > 3.0f) {
                    val glowRadius = radiusPx * 4f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(starColor.copy(alpha = 0.4f * alpha), Color.Transparent),
                            center = Offset(x, y),
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = Offset(x, y)
                    )
                }

                drawCircle(
                    color = starColor,
                    radius = radiusPx,
                    center = Offset(x, y),
                    alpha = alpha
                )
            }
        }
    }
}