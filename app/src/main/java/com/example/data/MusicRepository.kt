package com.example.data

import com.example.data.db.SongDao
import com.example.data.db.PlaylistDao
import com.example.model.Playlist
import com.example.model.Song
import com.example.gemini.GeminiMusicSearcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import java.util.UUID

class MusicRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) {
    val likedSongs: Flow<List<Song>> = songDao.getLikedSongs()
    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun searchSongs(query: String): List<Song> {
        if (query.isBlank()) {
            return GeminiMusicSearcher.getCuratedTrendingSongs()
        }
        return GeminiMusicSearcher.searchSongsOnYouTube(query)
    }

    fun isSongLikedFlow(songId: String): Flow<Boolean> {
        return songDao.isSongLikedFlow(songId)
    }

    suspend fun toggleLike(song: Song) {
        val alreadyLiked = songDao.isSongLiked(song.id)
        if (alreadyLiked) {
            songDao.deleteSong(song)
        } else {
            songDao.insertSong(song.copy(isLiked = true, dateAdded = System.currentTimeMillis()))
        }
    }

    suspend fun createPlaylist(name: String, description: String = "") {
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80&fit=crop", // Party speaker stage art
            songIdsJson = "[]"
        )
        playlistDao.insertPlaylist(newPlaylist)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        val currentIds = try {
            val jsonArray = JSONArray(playlist.songIdsJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            mutableListOf()
        }

        if (!currentIds.contains(songId)) {
            currentIds.add(songId)
            val updatedJson = JSONArray(currentIds).toString()
            playlistDao.updatePlaylist(playlist.copy(songIdsJson = updatedJson))
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        val currentIds = try {
            val jsonArray = JSONArray(playlist.songIdsJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            mutableListOf()
        }

        if (currentIds.remove(songId)) {
            val updatedJson = JSONArray(currentIds).toString()
            playlistDao.updatePlaylist(playlist.copy(songIdsJson = updatedJson))
        }
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist)
    }
}
