package com.example.apprumi.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apprumi.R
import com.example.apprumi.model.*
import com.example.apprumi.model.music.Song
import com.example.apprumi.ui.components.PetChatOverlay
import com.example.apprumi.ui.components.PetIdleAnimation
import com.example.apprumi.ui.components.music.MusicLibraryOverlay
import com.example.apprumi.viewmodel.ChatViewModel
import com.example.apprumi.viewmodel.MusicViewModel
import com.example.apprumi.viewmodel.PetViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
// --- COLORES ---
val DeepSpace = Color(0xFF0B0D17)
val SoftPurple = Color(0xFF1E1B4B)
val MutedCoral = Color(0xFFFB7185)
val ElectricCyan = Color(0xFF00E5FF)
val GlassWhite = Color(0xFFF8FAFC)
val ArcadeDark = Color(0xFF0F172A)

val ArcadeTextStyle = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontFamily = FontFamily.SansSerif,
    letterSpacing = 1.sp,
    shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), offset = Offset(2f, 2f), blurRadius = 4f)
)

private data class RadialStar(
    val angle: Float,
    val speedFactor: Float,
    val baseRadiusDp: Float,
    val baseAlpha: Float,
    val timeOffset: Float
)
// Este es el "molde" que le dice a Kotlin qué datos tiene el estado de tu mascota
data class PetStatus(
    val animo: Float = 1f,
    val salud: Float = 1f,
    val aseo: Float = 1f
)

 @Composable
fun VirtualPetScreen() {
    val context = LocalContext.current

    // --- VIEWMODELS ---
    val musicViewModel: MusicViewModel = viewModel()
    val petViewModel: PetViewModel = viewModel(factory = PetViewModel.provideFactory(AppDatabase.getDatabase(context).habitoDao()))
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.provideFactory(AppDatabase.getDatabase(context).chatDao()))

    // --- ESTADOS ---
    val petStatus by petViewModel.status.collectAsStateWithLifecycle()
    val listaHabitos by petViewModel.habitos.collectAsStateWithLifecycle()
    val isPlaying by musicViewModel.isPlaying.collectAsStateWithLifecycle()
    val bassLevel by musicViewModel.bassLevel.collectAsStateWithLifecycle<Float>()
    val currentProgress by musicViewModel.progress.collectAsStateWithLifecycle<Float>()
    val librarySongs by musicViewModel.songs.collectAsStateWithLifecycle<List<Song>>()
    val currentSong by musicViewModel.currentSong.collectAsStateWithLifecycle()
    val isShuffle by musicViewModel.isShuffle.collectAsStateWithLifecycle(initialValue = false)
    val isFavorite by musicViewModel.isFavorite.collectAsStateWithLifecycle(initialValue = false)

    // Estados de Control de UI
    var showMusicControls by remember { mutableStateOf(false) }
    var isHabitsVisible by remember { mutableStateOf(false) }
    var isChatVisible by remember { mutableStateOf(false) }
    var isLibraryVisible by remember { mutableStateOf(false) }
    var isManagementMode by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val listStateMisiones = rememberLazyListState()
     val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
         arrayOf(
             android.Manifest.permission.READ_MEDIA_AUDIO,
             android.Manifest.permission.RECORD_AUDIO
         )
     } else {
         arrayOf(
             android.Manifest.permission.READ_EXTERNAL_STORAGE,
             android.Manifest.permission.RECORD_AUDIO
         )
     }

     val launcher = rememberLauncherForActivityResult(
         ActivityResultContracts.RequestMultiplePermissions()
     ) { permissions ->
         // Si se aceptan ahora, cargamos la librería
         if (permissions.values.all { it }) {
             musicViewModel.loadLibrary()
         }
     }

     LaunchedEffect(Unit) {
         // FILTRO: Solo pedimos los que NO están concedidos
         val missingPermissions = permissionsToRequest.filter { permission ->
             androidx.core.content.ContextCompat.checkSelfPermission(
                 context, permission
             ) != android.content.pm.PackageManager.PERMISSION_GRANTED
         }

         if (missingPermissions.isNotEmpty()) {
             launcher.launch(missingPermissions.toTypedArray())
         } else {
             // Si ya los tiene todos, cargamos la música directamente
             musicViewModel.loadLibrary()
         }
     }
    // --- EFECTO DE DESENFOQUE ---
    val blurRadius by animateDpAsState(
        targetValue = if (showAddDialog) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "blur"
    )

    // --- LÓGICA DE NAVEGACIÓN ---
    val navigateTo: (String) -> Unit = { destination ->
        showMusicControls = false
        isHabitsVisible = false
        isChatVisible = false
        isLibraryVisible = false
        isManagementMode = false // <--- Añade esto para limpiar estados al navegar
        when (destination) {
            "HABITS" -> isHabitsVisible = true
            "CHAT" -> isChatVisible = true
            "LIBRARY" -> isLibraryVisible = true
        }
    }
     val dynamicHudSpacer by animateDpAsState(
         targetValue = if (showMusicControls) 350.dp else 180.dp, // Sube a 340 si está expandido
         animationSpec = spring(stiffness = Spring.StiffnessLow),
         label = "hudExpansion"
     )
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) musicViewModel.loadLibrary() }

    LaunchedEffect(showMusicControls) {
        if (showMusicControls) {
            delay(100000)
            showMusicControls = false
        }
    }
     LaunchedEffect(petViewModel.isPaused) {
         if (petViewModel.isMissionRunning) {
             if (petViewModel.isPaused && isPlaying) {
                 musicViewModel.togglePlayPause() // Pausa la música si pausamos la misión
             } else if (!petViewModel.isPaused && !isPlaying) {
                 musicViewModel.togglePlayPause() // Reanuda la música si reanudamos la misión
             }
         }
     }
     val checkPermissionAndStart: (Habito) -> Unit = { habito ->
         val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             android.Manifest.permission.READ_MEDIA_AUDIO
         } else {
             android.Manifest.permission.READ_EXTERNAL_STORAGE
         }

         val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
             context, permission
         ) == android.content.pm.PackageManager.PERMISSION_GRANTED

         if (isGranted) {
             // Si tiene permiso, iniciamos música y tiempo
             musicViewModel.playMissionPlaylist(habito.playlistIds)
             petViewModel.startMission(habito)
             isHabitsVisible = false
         } else {
             // Si no tiene, lanzamos el selector de permisos antes de dejarle empezar
             permissionLauncher.launch(permission)
         }
     }


     val isAnyMenuOpen = showAddDialog || isHabitsVisible || isChatVisible || isLibraryVisible || showMusicControls

     BackHandler(enabled = isAnyMenuOpen) {
         when {
             // 1. Prioridad máxima: Cerrar diálogos emergentes
             showAddDialog -> {
                 showAddDialog = false
             }
             // 2. Si estamos editando hábitos, primero salir del modo gestión
             isManagementMode -> {
                 isManagementMode = false
             }
             // 3. Cerrar los menús principales (Overlays)
             isLibraryVisible -> isLibraryVisible = false
             isChatVisible -> isChatVisible = false
             isHabitsVisible -> isHabitsVisible = false

             // 4. Si nada de lo anterior está abierto pero el HUD está expandido, lo colapsamos
             showMusicControls -> {
                 showMusicControls = false
             }
         }
     }




    Box(modifier = Modifier.fillMaxSize()) {

        // --- CAPA 1: MUNDO (ESTA CAPA SE DESENFOCA) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // RenderEffect para Android 12+, .blur para versiones anteriores
                    if (blurRadius > 0.dp) {
                        renderEffect = BlurEffect(blurRadius.toPx(), blurRadius.toPx())
                    }
                }
                .blur(blurRadius)
        ) {
            DynamicSpaceBackground(energy = bassLevel)
            ParticleBackground(bass = bassLevel, energy = bassLevel)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. ESPACIO PARA EL HUD (Status Bar + HUD)
                Spacer(modifier = Modifier.height(dynamicHudSpacer))
                // 2. CONTADOR DE MISIÓN
                AnimatedVisibility(
                    visible = petViewModel.isMissionRunning,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    MissionTimerDisplay(
                        missionName = petViewModel.activeMission?.nombre ?: "Misión",
                        seconds = petViewModel.secondsRemaining,
                        isPaused = petViewModel.isPaused,
                        onPauseToggle = { petViewModel.togglePause() },
                        onCancel = { petViewModel.stopMission() }
                    )
                }

                // 3. LA MASCOTA (Ocupando el resto del espacio)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).offset(y = (-60.dp))) {
                    PetIdleAnimation(
                        modifier = Modifier
                            .size(420.dp)
                            .graphicsLayer(alpha = if (isHabitsVisible || isLibraryVisible || isChatVisible) 0.60f else 1f)
                    )
                }
            }
        }
        // --- CAPA 2: HUD Y CONTROLES (ESTA CAPA NO SE DESENFOCA) ---

        // Detector de toques para cerrar controles de música
        if (showMusicControls && !isHabitsVisible && !isLibraryVisible && !isChatVisible) {
            Box(
                modifier = Modifier.fillMaxSize().zIndex(2f)
                    .pointerInput(Unit) { detectTapGestures { showMusicControls = false } }
            )
        }

        PetStatusHud(
                petStatus = PetStatus(petStatus.animo, petStatus.salud, petStatus.aseo),
                currentSong = currentSong, // <--- PASAR LA CANCIÓN ACTUAL
                isPlaying = isPlaying,
                currentProgress = currentProgress,
            bassLevel = bassLevel,
            isShuffle = isShuffle,
            isFavorite = isFavorite,
            showControls = showMusicControls,
            onToggleControls = {
                if (!showMusicControls) {
                    isHabitsVisible = false; isChatVisible = false; isLibraryVisible = false
                }
                showMusicControls = !showMusicControls
            },
            onActionResetTimer = { showMusicControls = false; showMusicControls = true },
            onPlayPause = { musicViewModel.togglePlayPause() },
            onNext = { musicViewModel.skipNext() },
            onPrevious = { musicViewModel.skipPrevious() },
            onShuffle = { musicViewModel.toggleShuffle() },
            onFavorite = { musicViewModel.toggleFavorite() },
            onSeek = { musicViewModel.seekTo(it) }, // <--- AÑADE ESTA LÍNEA
            modifier = Modifier

                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp)
                .zIndex(5f)
        )

        // Botones Laterales con Estética Smoked Glass
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).zIndex(5f).offset(y = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FloatingActionButton(
                onClick = { navigateTo("LIBRARY") },
                containerColor = ArcadeDark.copy(0.7f),
                contentColor = ElectricCyan,
                shape = CircleShape,
                modifier = Modifier.size(60.dp).border(1.dp, ElectricCyan.copy(0.3f), CircleShape)
            ) { Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(28.dp)) }

            FloatingActionButton(
                onClick = { navigateTo("CHAT") },
                containerColor = ArcadeDark.copy(0.7f),
                contentColor = ElectricCyan,
                shape = CircleShape,
                modifier = Modifier.size(60.dp).border(1.dp, ElectricCyan.copy(0.3f), CircleShape)
            ) { Icon(Icons.Default.ChatBubble, null, modifier = Modifier.size(28.dp)) }
        }

        // Botón Hábitos
        NeonHabitsButton(
            isHabitsVisible = isHabitsVisible,
            isManagementMode = isManagementMode,
            hasPending = listaHabitos.any { !it.completado },
            onClick = {
                if (!isHabitsVisible) navigateTo("HABITS")
                else isManagementMode = !isManagementMode
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp).zIndex(8f)
        )

        // --- CAPA 3: OVERLAYS (ESTILO MISIONES / TRANSPARENTE) ---

        // Busca donde llamas a MissionsOverlay en VirtualPetScreen
        MissionsOverlay(
            isVisible = isHabitsVisible,
            isManagementMode = isManagementMode,
            listaHabitos = listaHabitos,
            listState = listStateMisiones,
            onDismiss = { isHabitsVisible = false },
            onAddMission = { showAddDialog = true },
            onComplete = { petViewModel.completarHabito(it) },
            onDelete = { petViewModel.borrarHabito(it) },
            onStartMission = { habito -> checkPermissionAndStart(habito) }, // <--- CAMBIO AQUÍ
            modifier = Modifier.zIndex(7f)
        )


        PetChatOverlay(
            isVisible = isChatVisible,
            onDismiss = { isChatVisible = false },
            chatViewModel = chatViewModel,
            onSendMessage = { texto -> chatViewModel.sendMessageWithContext(texto, listaHabitos, currentSong) },
            modifier = Modifier.zIndex(10f)
        )

        MusicLibraryOverlay(
            isVisible = isLibraryVisible,
            songs = librarySongs,
            onSongSelect = { song -> musicViewModel.playSong(song); isLibraryVisible = false },
            onDismiss = { isLibraryVisible = false },
            onPermissionRequest = {
                val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    android.Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                }
                permissionLauncher.launch(permission)
            },
            modifier = Modifier.zIndex(10f)
        )
    }

     // Dentro de VirtualPetScreen, busca el bloque if(showAddDialog)
     if (showAddDialog) {
         AddHabitDialog(
             onDismiss = { showAddDialog = false },
             allSongs = librarySongs,
             onPermissionRequest = {
                 // Reutilizamos la lógica de permisos que ya tienes
                 val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                     android.Manifest.permission.READ_MEDIA_AUDIO
                 } else {
                     android.Manifest.permission.READ_EXTERNAL_STORAGE
                 }
                 permissionLauncher.launch(permission)
             },
             onConfirm = { n, m, d, tiempo, playlist ->
                 petViewModel.agregarHabito(n, m, d, tiempo, playlist)
                 showAddDialog = false
             }
         )
     }
}


@Composable
fun NeonHabitsButton(
    isHabitsVisible: Boolean,
    isManagementMode: Boolean,
    hasPending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    // CORRECCIÓN: El nombre correcto de la función es collectIsPressedAsState
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .height(64.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if(hasPending && !isHabitsVisible) 15.dp else 0.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = ElectricCyan
            ),
        shape = RoundedCornerShape(32.dp),
        // Usamos ArcadeDark con transparencia para el efecto "Smoked Glass"
        color = if (isHabitsVisible) ElectricCyan else ArcadeDark.copy(alpha = 0.8f),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isHabitsVisible) ElectricCyan else ElectricCyan.copy(alpha = 0.4f)
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 40.dp)) {
            Text(
                text = if (isHabitsVisible) (if (isManagementMode) "LISTO" else "GESTIONAR") else "HÁBITOS",
                style = ArcadeTextStyle.copy(
                    color = if(isHabitsVisible) ArcadeDark else Color.White,
                    fontSize = 16.sp,
                    shadow = null // Sin sombras internas para que se vea limpio
                )
            )
        }
    }
}



@Composable
fun MissionsOverlay(
    isVisible: Boolean,
    isManagementMode: Boolean,
    listaHabitos: List<Habito>,
    listState: LazyListState,
    onDismiss: () -> Unit,
    onAddMission: () -> Unit,
    onComplete: (Habito) -> Unit,
    onDelete: (Habito) -> Unit,
    onStartMission: (Habito) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Cerramos al tocar fuera o deslizar
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount -> if (dragAmount > 30) onDismiss() }
                }
        ) {
            // --- CAPA DE FONDO BORROSA ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)) // Oscurece un poco
                    .blur(15.dp) // <--- AQUÍ AGREGAMOS LA BORROSIDAD
            )

            // --- CONTENIDO (SIN BORROSIDAD) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
            ) {
                Spacer(modifier = Modifier.height(160.dp))

                Text(
                    text = "MISIONES",
                    style = ArcadeTextStyle.copy(
                        fontSize = 34.sp,
                        brush = Brush.linearGradient(listOf(ElectricCyan, GlassWhite))
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 220.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isManagementMode) {
                        item { AddMissionListItem(onAddMission) }
                    }
                    items(items = listaHabitos, key = { it.id }) { habito ->
                        HabitItemDB(
                            habito = habito,
                            isManagement = isManagementMode,
                            onCheck = { onComplete(habito) },
                            onDelete = { onDelete(habito) },
                            onStart = { onStartMission(habito) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PetStatusHud(
    petStatus: PetStatus,
    currentSong: Song?,
    isPlaying: Boolean,
    currentProgress: Float,
    bassLevel: Float,
    isShuffle: Boolean,
    isFavorite: Boolean,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onActionResetTimer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Memoria persistente para evitar reinicios al expandir
    val liftedStockBuffer = remember { FloatArray(500) { 0f } }
    var lastY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentProgress, bassLevel) {
        if (isPlaying) {
            val currentIndex = (currentProgress * (liftedStockBuffer.size - 1)).toInt().coerceIn(0, liftedStockBuffer.size - 1)
            val volatility = 50.dp.value * (bassLevel * 1.5f)
            val targetY = (Random.nextFloat() - 0.5f) * volatility - (bassLevel * 20.dp.value)
            lastY = lastY * 0.6f + targetY * 0.4f
            liftedStockBuffer[currentIndex] = lastY
        }
    }

    val musicBtnScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f + (bassLevel * 0.12f) else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = ""
    )

    Surface(
        modifier = modifier
            .width(320.dp)
            .drawBehind {
                val glowAlpha = if (isPlaying) (0.1f + bassLevel * 0.2f) else 0.05f
                drawRoundRect(
                    color = ElectricCyan.copy(alpha = glowAlpha),
                    size = size,
                    cornerRadius = CornerRadius(40.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            },
        color = ArcadeDark.copy(alpha = 0.85f),
        shape = RoundedCornerShape(40.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.15f))
    ) {
        Box {
            // CAPA FONDO: Detecta Tap/Drag para expandir SOLO si está cerrado
            // Si está abierto, permite cerrar al tocar fuera de los controles
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(showControls) {
                        detectTapGestures { onToggleControls() }
                    }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp).animateContentSize()
            ) {
                if (showControls) {
                    // MODO EXPANDIDO: Título, Artista y Controles
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(currentSong?.title?.uppercase() ?: "SIN REPRODUCCIÓN",
                            style = ArcadeTextStyle.copy(fontSize = 14.sp, brush = Brush.linearGradient(listOf(ElectricCyan, Color.White))))
                        Text(currentSong?.artist?.uppercase() ?: "RUMI OS",
                            style = TextStyle(color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(35.dp)) {
                        StatusCircleLarge(Icons.Default.SentimentSatisfiedAlt, petStatus.animo)
                        StatusCircleLarge(Icons.Default.Favorite, petStatus.salud)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { onShuffle(); onActionResetTimer() }) { Icon(Icons.Default.Shuffle, null, tint = if (isShuffle) ElectricCyan else Color.White.copy(0.2f), modifier = Modifier.size(20.dp)) }
                        IconButton(onClick = { onPrevious(); onActionResetTimer() }) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp)) }

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(62.dp).graphicsLayer(scaleX = musicBtnScale, scaleY = musicBtnScale).clip(CircleShape).background(ElectricCyan.copy(0.1f)).clickable { onPlayPause(); onActionResetTimer() }) {
                            CircularProgressIndicator(progress = { currentProgress }, modifier = Modifier.size(58.dp), color = ElectricCyan, strokeWidth = 3.dp)
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = ElectricCyan, modifier = Modifier.size(28.dp))
                        }

                        IconButton(onClick = { onNext(); onActionResetTimer() }) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                        IconButton(onClick = { onFavorite(); onActionResetTimer() }) { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFavorite) MutedCoral else Color.White.copy(0.3f), modifier = Modifier.size(20.dp)) }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    StockRhythmVisualizer(
                        progress = currentProgress,
                        bass = bassLevel,
                        isPlaying = isPlaying,
                        stockBuffer = liftedStockBuffer,
                        isSeekEnabled = true, // ACTIVADO
                        showLead = true,
                        onSeek = { onSeek(it); onActionResetTimer() },
                        modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 25.dp)
                    )
                } else {
                    // MODO MINIMALISTA
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        StatusCircleMedium(Icons.Default.SentimentSatisfiedAlt, petStatus.animo)
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp).graphicsLayer(scaleX = musicBtnScale, scaleY = musicBtnScale).clip(CircleShape).background(ElectricCyan.copy(0.05f)).clickable { onPlayPause() }) {
                            CircularProgressIndicator(progress = { currentProgress }, modifier = Modifier.fillMaxSize(), color = ElectricCyan, strokeWidth = 2.5.dp)
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        }
                        StatusCircleMedium(Icons.Default.Favorite, petStatus.salud)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    StockRhythmVisualizer(
                        progress = currentProgress,
                        bass = bassLevel,
                        isPlaying = isPlaying,
                        stockBuffer = liftedStockBuffer,
                        isSeekEnabled = false, // DESACTIVADO para que el toque abra el HUD
                        showLead = false,
                        onSeek = {},
                        modifier = Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StockRhythmVisualizer(
    progress: Float,
    bass: Float,
    isPlaying: Boolean,
    stockBuffer: FloatArray,
    isSeekEnabled: Boolean,
    showLead: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .clipToBounds() // SEGURIDAD 1: Corta cualquier dibujo que intente salir del área
            .then(
                if (isSeekEnabled) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isDragging = true
                            onSeek((down.position.x / size.width).coerceIn(0f, 1f))
                            drag(down.id) { change ->
                                change.consume()
                                onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                            }
                            isDragging = false
                        }
                    }
                } else Modifier
            )
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val progressWidth = width * progress

        // SEGURIDAD 2: Limitamos la altura máxima de la onda al 80% del contenedor
        // para dejar espacio al grosor del neón (Stroke) sin que choque con los bordes.
        val maxAmplitude = height * 0.4f

        val path = Path()
        val fillPath = Path()

        for (i in 0 until stockBuffer.size) {
            val xPos = (i.toFloat() / (stockBuffer.size - 1)) * width

            // Limitamos el valor del buffer para que no exceda el margen de seguridad
            val rawY = if (xPos <= progressWidth) stockBuffer[i] else 0f
            val clampedY = rawY.coerceIn(-maxAmplitude, maxAmplitude)

            if (i == 0) {
                path.moveTo(xPos, centerY + clampedY)
                fillPath.moveTo(xPos, height)
                fillPath.lineTo(xPos, centerY + clampedY)
            } else {
                path.lineTo(xPos, centerY + clampedY)
                fillPath.lineTo(xPos, centerY + clampedY)
            }
        }
        fillPath.lineTo(progressWidth, height)
        fillPath.close()

        // Dibujamos todo dentro de un clipRect general por si acaso
        clipRect {
            // 1. Relleno Líquido
            clipRect(right = progressWidth) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(ElectricCyan.copy(alpha = 0.2f * bass), Color.Transparent),
                        startY = centerY - maxAmplitude,
                        endY = height
                    )
                )
            }

            // 2. Línea Neón con Glow
            clipRect(right = progressWidth) {
                // Brillo (Glow)
                drawPath(
                    path = path,
                    color = ElectricCyan.copy(alpha = 0.3f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Línea principal
                drawPath(
                    path = path,
                    color = ElectricCyan,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 3. Línea de Futuro
            clipRect(left = progressWidth) {
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.1f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 4. Guía vertical y Punto de énfasis
            if (progress in 0.001f..0.999f) {
                val currentRawY = stockBuffer[(progress * (stockBuffer.size - 1)).toInt()]
                val currentClampedY = centerY + currentRawY.coerceIn(-maxAmplitude, maxAmplitude)

                if (isDragging && showLead) {
                    drawLine(
                        color = ElectricCyan.copy(alpha = 0.4f),
                        start = Offset(progressWidth, 0f),
                        end = Offset(progressWidth, height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                if (showLead) {
                    val bassPulse = 1f + (bass * 0.3f)
                    val radius = (if (isDragging) 14.dp else 10.dp).toPx() * bassPulse

                    drawCircle(ElectricCyan.copy(alpha = 0.4f), radius, Offset(progressWidth, currentClampedY))
                    drawCircle(Color.White, 3.dp.toPx(), Offset(progressWidth, currentClampedY))
                }
            }
        }
    }
}
// --- TAMAÑOS REDUCIDOS UN 10% ---

@Composable
fun StatusCircleMedium(icon: androidx.compose.ui.graphics.vector.ImageVector, progress: Float) {
    val color = when {
        progress < 0.35f -> MutedCoral
        progress < 0.70f -> Color(0xFFFFD600)
        else -> Color(0xFF00E676)
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
        CircularProgressIndicator(progress = { progress }, color = color, strokeWidth = 2.5.dp, modifier = Modifier.fillMaxSize(), strokeCap = StrokeCap.Round, trackColor = Color.White.copy(0.1f))
        Icon(icon, null, tint = color.copy(0.9f), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun StatusCircleLarge(icon: androidx.compose.ui.graphics.vector.ImageVector, progress: Float) {
    val color = when {
        progress < 0.35f -> MutedCoral
        progress < 0.70f -> Color(0xFFFFD600)
        else -> Color(0xFF00E676)
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(68.dp)) {
        CircularProgressIndicator(progress = { 1f }, color = Color.White.copy(0.05f), strokeWidth = 3.5.dp, modifier = Modifier.fillMaxSize())
        CircularProgressIndicator(progress = { progress }, color = color, strokeWidth = 3.5.dp, modifier = Modifier.fillMaxSize(), strokeCap = StrokeCap.Round)
        Icon(icon, null, tint = color.copy(0.9f), modifier = Modifier.size(24.dp))
    }
}
@Composable
fun AddMusicListItem(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = ElectricCyan.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LibraryAdd,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "MÚSICA",
                style = ArcadeTextStyle.copy(color = ElectricCyan, fontSize = 13.sp, shadow = null)
            )
        }
    }
}
@Composable
fun ParticleBackground(bass: Float, energy: Float) {
    val density = LocalDensity.current
    val starCount = 90
    val minStars = 40

    val stars = remember {
        List(starCount) {
            val sizeTier = Random.nextFloat().pow(4f)
            RadialStar(
                angle = Random.nextFloat() * 2 * PI.toFloat(),
                speedFactor = Random.nextFloat() * 0.5f + 0.5f,
                baseRadiusDp = 0.8f + sizeTier * 5f,
                baseAlpha = 0.2f + (sizeTier * 0.4f),
                timeOffset = Random.nextFloat()
            )
        }
    }

    val drift by rememberInfiniteTransition(label = "c").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(35000, easing = LinearEasing)), label = "d"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxDist = sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()

        val auraRadius = maxDist * 0.7f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricCyan.copy(alpha = 0.15f * bass.pow(2f)), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = auraRadius
            ),
            radius = auraRadius,
            center = Offset(centerX, centerY)
        )

        val starsToDraw = (minStars + (energy * (starCount - minStars))).toInt()

        stars.take(starsToDraw).forEach { star ->
            val progress = ((drift + star.timeOffset) * star.speedFactor) % 1f
            val musicPush = (bass.pow(2f) * 180f) * progress
            val dist = (progress * maxDist) + musicPush

            val x = centerX + cos(star.angle) * dist
            val y = centerY + sin(star.angle) * dist

            val musicScale = 1f + (energy * 1.5f) + (bass.pow(2f) * 2.5f)
            val radius = with(density) { (star.baseRadiusDp.dp * musicScale).toPx() }

            val starColor = lerp(Color.White, ElectricCyan, bass.coerceIn(0f, 1f))
            val finalAlpha = ((star.baseAlpha + (bass * 0.4f)) * (1f - progress).pow(2f)).coerceIn(0f, 1f)

            if (star.baseRadiusDp > 3.0f) {
                val glowRadius = radius * 6f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(starColor.copy(alpha = 0.3f * bass * finalAlpha), Color.Transparent),
                        center = Offset(x, y),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset(x, y)
                )
            }

            drawCircle(starColor, radius, Offset(x, y), alpha = finalAlpha)
        }
    }
}

@Composable
fun DynamicSpaceBackground(energy: Float) {
    val VoidBlack = Color(0xFF05070E)
    val DeepNebula = Color(0xFF120F30)
    val CoralGlow = MutedCoral.copy(alpha = 0.15f)

    Spacer(
        modifier = Modifier.fillMaxSize().drawBehind {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxDimension = max(size.width, size.height)

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(VoidBlack, DeepSpace, DeepNebula),
                    center = Offset(centerX, centerY),
                    radius = maxDimension * 0.8f
                )
            )

            val nebulaPulse = 0.8f + (energy * 0.3f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, SoftPurple.copy(0.3f), CoralGlow),
                    center = Offset(centerX, centerY),
                    radius = maxDimension * nebulaPulse,
                    tileMode = TileMode.Clamp
                ),
                blendMode = BlendMode.Screen
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(0.6f)),
                    center = Offset(centerX, centerY),
                    radius = maxDimension * 0.9f,
                )
            )
        }.blur(10.dp)
    )
}

@Composable
fun StatusCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, progressValue: Float) {
    val color = when {
        progressValue < 0.35f -> MutedCoral
        progressValue < 0.70f -> Color(0xFFFFD600)
        else -> Color(0xFF00E676)
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
        CircularProgressIndicator(progress = { 1f }, color = Color.White.copy(0.05f), strokeWidth = 4.dp, modifier = Modifier.size(55.dp))
        CircularProgressIndicator(progress = { progressValue }, color = color, strokeWidth = 4.dp, modifier = Modifier.size(55.dp), strokeCap = StrokeCap.Round)
        Icon(icon, null, tint = color.copy(0.9f), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun HabitItemDB(
    habito: Habito,
    isManagement: Boolean,
    onCheck: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit
) {
    val isDone = habito.completado

    Surface(
        color = if (isDone) Color.White.copy(0.03f) else Color.White.copy(0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDone) Color.Transparent else ElectricCyan.copy(0.2f)),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BOTÓN COMPLETAR (Check)
            IconButton(
                onClick = onCheck,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isDone) ElectricCyan else Color.White.copy(0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Rounded.Check,
                    null,
                    tint = if (isDone) ArcadeDark else Color.White.copy(0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    habito.nombre.uppercase(),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = if(isDone) Color.Gray else Color.White,
                        textDecoration = if(isDone) TextDecoration.LineThrough else null
                    )
                )
                Text(
                    "${habito.duracionMinutos} MIN",
                    style = TextStyle(fontSize = 10.sp, color = ElectricCyan.copy(0.5f))
                )
            }

            // ACCIONES LATERALES
            if (isManagement) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, null, tint = MutedCoral.copy(0.7f))
                }
            } else if (!isDone) {
                // Botón Iniciar con texto para que sea más claro
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan.copy(0.2f)),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                    Text("INICIAR", color = ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun MissionTimerDisplay(
    missionName: String,
    seconds: Int,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onCancel: () -> Unit
) {
    val mins = seconds / 60
    val secs = seconds % 60
    val timeText = String.format("%02d:%02d", mins, secs)

    // Contenedor tipo "Cápsula" para que ocupe menos espacio horizontal
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(horizontal = 16.dp),
        color = ArcadeDark.copy(alpha = 0.75f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // INFO Y RELOJ
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = missionName.uppercase(),
                    style = ArcadeTextStyle.copy(fontSize = 10.sp, color = ElectricCyan.copy(alpha = 0.6f))
                )
                Text(
                    text = timeText,
                    style = ArcadeTextStyle.copy(
                        fontSize = 32.sp,
                        color = if (isPaused) Color.Gray else Color.White,
                        shadow = if (isPaused) null else Shadow(ElectricCyan, Offset(0f, 0f), 15f)
                    )
                )
            }

            // BOTONES DE ACCIÓN
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play/Pause
                IconButton(
                    onClick = onPauseToggle,
                    modifier = Modifier
                        .size(45.dp)
                        .background(ElectricCyan.copy(0.1f), CircleShape)
                        .border(1.dp, ElectricCyan.copy(0.3f), CircleShape)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        null,
                        tint = ElectricCyan
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Cancelar
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(35.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = MutedCoral.copy(0.6f))
                }
            }
        }
    }
}

@Composable
fun AddMissionListItem(onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.White.copy(0.05f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(0.2f)), modifier = Modifier.fillMaxWidth().height(60.dp)) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Add, null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("AÑADIR MISIÓN", style = ArcadeTextStyle.copy(color = Color.White, fontSize = 14.sp, shadow = null))
        }
    }
}



@Composable
fun WavyProgressIndicator(
    progress: Float,
    bass: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // 1. Animación de fase: La velocidad aumenta cuando el bajo es fuerte
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 2000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val steps = 80 // Mayor resolución para suavidad

        fun drawWave(
            amplitudeMult: Float,
            frequency: Float,
            alpha: Float,
            color: Color,
            strokeWidth: Float,
            isForeground: Boolean = false
        ) {
            val path = Path()
            val progressWidth = width * progress

            for (i in 0..steps) {
                val x = (i.toFloat() / steps) * width
                val taper = sin(PI * i / steps).toFloat().pow(1.5f) // Suaviza extremos

                // --- MATEMÁTICA RÍTMICA ---
                // Combinamos una onda base con una de alta frecuencia influenciada por el bajo
                val baseWave = sin(i * frequency + phase)
                val rhythmWave = sin(i * frequency * 2.5f - phase * 1.5f) * (bass * 0.5f)

                val combinedWave = (baseWave + rhythmWave) * amplitudeMult * (if (isPlaying) 1f else 0.1f)
                val y = centerY + (combinedWave * taper * 15.dp.toPx())

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Dibujar fondo de la onda
            drawPath(
                path = path,
                color = color.copy(alpha = alpha * 0.2f),
                style = Stroke(width = strokeWidth + 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dibujar línea principal (Solo hasta el progreso si es foreground)
            if (isForeground) {
                // Usamos un clip para que solo se vea la parte del progreso con color fuerte
                clipRect(right = progressWidth) {
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                // El resto de la línea en un color apagado
                clipRect(left = progressWidth) {
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.1f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            } else {
                drawPath(
                    path = path,
                    color = color.copy(alpha = alpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // 1. Capa de profundidad (Onda lenta morada/cian oscura)
        drawWave(0.4f + (bass * 0.2f), 0.15f, 0.3f, SoftPurple, 2.dp.toPx())

        // 2. Capa de ritmo (Onda reactiva media)
        drawWave(0.6f + (bass * 0.5f), 0.25f, 0.5f, ElectricCyan.copy(0.4f), 1.5.dp.toPx())

        // 3. Capa de progreso (Onda principal enfocada)
        drawWave(0.3f + (bass * 0.8f), 0.35f, 1f, ElectricCyan, 2.5.dp.toPx(), isForeground = true)
    }
}
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    allSongs: List<Song>,
    onPermissionRequest: () -> Unit, // <--- NUEVO PARÁMETRO
    onConfirm: (String, String, Dificultad, Int, String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var dificultad by remember { mutableStateOf(Dificultad.MEDIA) }
    var duracionMision by remember { mutableFloatStateOf(20f) }
    var selectedPlaylistIds by remember { mutableStateOf("") }
    var showMusicPicker by remember { mutableStateOf(false) }

    // --- SOLUCIÓN: Selector de música en un Diálogo de nivel superior ---
    if (showMusicPicker) {
        Dialog(
            onDismissRequest = { showMusicPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MusicLibraryOverlay(
                isVisible = true,
                songs = allSongs,
                isSelectionMode = true,
                onConfirmSelection = { canciones ->
                    selectedPlaylistIds = canciones.map { it.id }.joinToString(",")
                    showMusicPicker = false
                },
                onSongSelect = {},
                onDismiss = { showMusicPicker = false },
                onPermissionRequest = onPermissionRequest // <--- AHORA SÍ ESTÁ CONECTADO
            )
        }
    }

    // --- DIÁLOGO DE NUEVA MISIÓN ---
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = ArcadeDark.copy(0.95f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, ElectricCyan.copy(0.4f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("NUEVA MISIÓN", style = ArcadeTextStyle.copy(color = ElectricCyan, fontSize = 22.sp))

                Spacer(modifier = Modifier.height(16.dp))
                GlassInput(nombre, { nombre = it }, "NOMBRE DE LA TAREA")
                Spacer(modifier = Modifier.height(12.dp))
                GlassInput(motivo, { motivo = it }, "OBJETIVO")

                Spacer(modifier = Modifier.height(24.dp))

                LabelText("TIEMPO: ${duracionMision.toInt()} MIN")
                Slider(
                    value = duracionMision,
                    onValueChange = { duracionMision = it },
                    valueRange = 5f..120f,
                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // BOTÓN PARA ABRIR LA PLAYLIST
                Surface(
                    onClick = { showMusicPicker = true },
                    color = if(selectedPlaylistIds.isEmpty()) Color.White.copy(0.05f) else ElectricCyan.copy(0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if(selectedPlaylistIds.isEmpty()) Color.White.copy(0.2f) else ElectricCyan)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        val numCanciones = if(selectedPlaylistIds.isEmpty()) 0 else selectedPlaylistIds.split(",").size
                        Text(
                            if(numCanciones == 0) "CONFIGURAR AMBIENTE"
                            else "$numCanciones CANCIONES SELECCIONADAS",
                            color = Color.White, fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { onConfirm(nombre, motivo, dificultad, duracionMision.toInt(), selectedPlaylistIds) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    enabled = nombre.isNotBlank()
                ) {
                    Text("GUARDAR OPERACIÓN", color = ArcadeDark, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
@Composable
fun LabelText(text: String) {
    Text(
        text = text,
        color = ElectricCyan,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun GlassInput(value: String, onValueChange: (String) -> Unit, hint: String) {
    Surface(
        color = Color.Black.copy(0.3f), // Un poco más oscuro para que la letra blanca resalte
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(18.dp),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(ElectricCyan),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(hint, color = Color.White.copy(0.25f), fontSize = 15.sp)
                inner()
            }
        )
    }
}


