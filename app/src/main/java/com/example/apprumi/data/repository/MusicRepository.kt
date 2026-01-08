package com.example.apprumi.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.apprumi.model.music.Song

class MusicRepository(private val context: Context) {
    fun fetchLocalSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.ALBUM} ASC" // Ordenamos por álbum para ayudar a la agrupación
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleCol) ?: cursor.getString(nameCol) ?: "Desconocido"
                val artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Artista Desconocido"
                val album = cursor.getString(albumCol)?.takeIf { it != "<unknown>" } ?: "Álbum Desconocido"

                songs.add(Song(
                    id = cursor.getLong(idCol),
                    uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol)),
                    title = title,
                    artist = artist,
                    album = album,
                    albumId = cursor.getLong(albumIdCol),
                    duration = cursor.getInt(durationCol)
                ))
            }
        }
        return songs
    }
}