package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "liked_songs")
data class Song(
    @PrimaryKey val id: String, // Dynamic video or generated ID
    val title: String,
    val artist: String,
    val album: String = "YouTube Music",
    val durationSeconds: Int = 180,
    val streamUrl: String,
    val coverUrl: String,
    val category: String = "Mixed",
    val lyrics: String = "No lyrics available for this song.",
    val isLiked: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
) : Serializable {
    val durationString: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
