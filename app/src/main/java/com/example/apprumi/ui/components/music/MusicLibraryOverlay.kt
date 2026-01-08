package com.example.apprumi.ui.components.music

import android.content.ContentUris
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.apprumi.R
import com.example.apprumi.model.music.Song
import com.example.apprumi.ui.screens.ArcadeDark
import com.example.apprumi.ui.screens.ArcadeTextStyle
import com.example.apprumi.ui.screens.ElectricCyan
import com.example.apprumi.ui.screens.GlassWhite
import com.example.apprumi.ui.screens.MutedCoral

@Composable
fun MusicLibraryOverlay(
    isVisible: Boolean,
    songs: List<Song>,
    isSelectionMode: Boolean = false, // <--- NUEVO: Modo selección para misiones
    onSongSelect: (Song) -> Unit,
    onConfirmSelection: (List<Song>) -> Unit = {}, // <--- NUEVO: Callback para la lista final
    onDismiss: () -> Unit,
    onPermissionRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFolder by remember { mutableStateOf<String?>(null) }

    // --- ESTADO DE SELECCIÓN MÚLTIPLE ---
    val selectedSongs = remember { mutableStateListOf<Song>() }

    val tabs = listOf("CANCIONES", "ARTISTAS", "ÁLBUMES")

    val filteredSongs = remember(songs, searchQuery) {
        songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.title.lowercase() }
    }
    val artists = remember(filteredSongs) { filteredSongs.groupBy { it.artist }.toSortedMap(compareBy { it.lowercase() }) }
    val albums = remember(filteredSongs) { filteredSongs.groupBy { it.album }.toSortedMap(compareBy { it.lowercase() }) }

    BackHandler(enabled = isVisible && activeFolder != null) { activeFolder = null }

    AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.85f)) // Un poco más oscuro para modo selección
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount -> if (dragAmount > 30) onDismiss() }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
                Spacer(modifier = Modifier.height(160.dp))

                // --- CABECERA ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeFolder != null) {
                        IconButton(onClick = { activeFolder = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ElectricCyan)
                        }
                    }
                    Text(
                        text = if(isSelectionMode) "PLAYLIST MISIÓN" else (activeFolder ?: "BIBLIOTECA"),
                        style = ArcadeTextStyle.copy(
                            fontSize = 28.sp,
                            brush = Brush.linearGradient(listOf(ElectricCyan, GlassWhite))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelectionMode) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }

                // --- BUSCADOR ---
                Surface(
                    modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                    color = Color.White.copy(0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.1f))
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it; if (it.isNotEmpty()) activeFolder = null },
                        placeholder = { Text("BUSCAR MÚSICA...", color = Color.White.copy(0.3f), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = ElectricCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (songs.isEmpty()) {
                    PermissionRequiredView(onGrantClick = onPermissionRequest)
                } else {
                    if (activeFolder == null) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = ElectricCyan,
                            indicator = { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(it[selectedTab]), color = ElectricCyan) },
                            divider = {}
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title, style = ArcadeTextStyle.copy(fontSize = 10.sp, color = if(selectedTab == index) ElectricCyan else Color.Gray)) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 150.dp)
                        ) {
                            val displaySongs = when {
                                activeFolder != null -> filteredSongs.filter { it.artist == activeFolder || it.album == activeFolder }
                                selectedTab == 0 -> filteredSongs
                                else -> emptyList()
                            }

                            if (selectedTab == 0 || activeFolder != null) {
                                items(displaySongs) { song ->
                                    val isSelected = selectedSongs.any { it.id == song.id }
                                    SongItemLiquid(
                                        song = song,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelectionMode) {
                                                if (isSelected) selectedSongs.removeIf { it.id == song.id }
                                                else selectedSongs.add(song)
                                            } else {
                                                onSongSelect(song)
                                            }
                                        }
                                    )
                                }
                            } else if (selectedTab == 1) {
                                items(artists.keys.toList()) { artist ->
                                    FolderItemLiquid(artist, "${artists[artist]?.size} temas") { activeFolder = artist }
                                }
                            } else if (selectedTab == 2) {
                                items(albums.keys.toList()) { album ->
                                    FolderItemLiquid(album, albums[album]?.firstOrNull()?.artist ?: "Varios") { activeFolder = album }
                                }
                            }
                        }

                        // --- BOTÓN FLOTANTE DE CONFIRMACIÓN (Solo en modo selección) ---
                        // Dentro de MusicLibraryOverlay.kt
                        if (isSelectionMode && selectedSongs.isNotEmpty()) {
                            Button(
                                onClick = {
                                    // .toList() crea una copia de la lista actual seleccionada
                                    onConfirmSelection(selectedSongs.toList())
                                },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                            ) {
                                Text("CONFIRMAR SELECCIÓN", style = ArcadeTextStyle.copy(color = ArcadeDark))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongItemLiquid(
    song: Song,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) ElectricCyan.copy(0.15f) else Color.White.copy(0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) ElectricCyan else Color.White.copy(0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Arte del álbum
            val artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(0.1f)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title.uppercase(),
                    color = if(isSelected) ElectricCyan else Color.White,
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    maxLines = 1
                )
                Text(
                    song.artist.uppercase(),
                    color = if(isSelected) ElectricCyan.copy(0.8f) else Color.White.copy(0.5f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }

            // INDICADOR DE SELECCIÓN (Check Neon)
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ElectricCyan else Color.Transparent)
                        .border(2.dp, if (isSelected) ElectricCyan else Color.White.copy(0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, null, tint = ArcadeDark, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FolderItemLiquid(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, null, tint = ElectricCyan, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle.uppercase(), color = Color.Gray, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = ElectricCyan.copy(0.5f))
        }
    }
}
@Composable
fun PermissionRequiredView(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, null, tint = MutedCoral, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("ACCESO REQUERIDO", style = ArcadeTextStyle.copy(fontSize = 22.sp, color = MutedCoral))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Necesitamos permiso para leer tu música y que tu mascota pueda reaccionar a ella.",
            color = Color.White.copy(0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGrantClick,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("CONCEDER PERMISO", color = ArcadeDark, style = ArcadeTextStyle.copy(fontSize = 14.sp))
        }
    }
}