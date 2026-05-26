package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val coverUrl: String = "",
    val songIdsJson: String = "[]", // Serialized string of song IDs (e.g., "[id1, id2]")
    val dateCreated: Long = System.currentTimeMillis()
)
