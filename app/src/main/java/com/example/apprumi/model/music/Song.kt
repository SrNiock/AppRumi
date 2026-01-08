package com.example.apprumi.model.music

import android.net.Uri

data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long, // <--- Nuevo
    val duration: Int
)