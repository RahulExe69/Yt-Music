package com.example.data.db

import androidx.room.*
import com.example.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM liked_songs ORDER BY dateAdded DESC")
    fun getLikedSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("SELECT EXISTS(SELECT * FROM liked_songs WHERE id = :songId)")
    fun isSongLikedFlow(songId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT * FROM liked_songs WHERE id = :songId)")
    suspend fun isSongLiked(songId: String): Boolean
}
