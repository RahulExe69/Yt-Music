package com.example.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.db.AppDatabase
import com.example.model.Playlist
import com.example.model.Song
import com.example.playback.MusicPlaybackService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ScreenState {
    object Home : ScreenState
    object Search : ScreenState
    object Library : ScreenState
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository

    // Screen states
    private val _currentScreen = MutableStateFlow<ScreenState>(ScreenState.Home)
    val currentScreen: StateFlow<ScreenState> = _currentScreen

    val likedSongs: StateFlow<List<Song>>
    val playlists: StateFlow<List<Playlist>>

    // Media Service reference
    private var isServiceBound = false
    private var playbackService: MusicPlaybackService? = null

    // Flows derived directly from the service state
    private val _currentPlayingSong = MutableStateFlow<Song?>(null)
    val currentPlayingSong: StateFlow<Song?> = _currentPlayingSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    // Search results & loading
    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _trendingFeed = MutableStateFlow<List<Song>>(emptyList())
    val trendingFeed: StateFlow<List<Song>> = _trendingFeed

    // Service Connection binder
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlaybackService.LocalBinder
            playbackService = binder.getService()
            isServiceBound = true

            // Co-observe flows from playback service for UI binding
            viewModelScope.launch {
                playbackService?.currentPlayingSong?.collect { _currentPlayingSong.value = it }
            }
            viewModelScope.launch {
                playbackService?.isPlaying?.collect { _isPlaying.value = it }
            }
            viewModelScope.launch {
                playbackService?.currentPosition?.collect { _currentPosition.value = it }
            }
            viewModelScope.launch {
                playbackService?.duration?.collect { _duration.value = it }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isServiceBound = false
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MusicRepository(database.songDao(), database.playlistDao())

        likedSongs = repository.likedSongs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        playlists = repository.playlists.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Bind playback background service
        val intent = Intent(application, MusicPlaybackService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        application.startService(intent) // Ensure service stays alive after unbinding

        // Fetch curated trending initially
        viewModelScope.launch {
            _trendingFeed.value = repository.searchSongs("")
        }
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
        }
    }

    fun performSearch() {
        val query = _searchQuery.value
        if (query.isBlank()) return

        _isSearching.value = true
        viewModelScope.launch {
            try {
                val results = repository.searchSongs(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    // Media Control API wrappers for UI Action Buttons
    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val playlistIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackService?.setPlaylist(queue, playlistIndex)
    }

    fun togglePlayPause() {
        playbackService?.togglePlayPause()
    }

    fun playNext() {
        playbackService?.playNext()
    }

    fun playPrev() {
        playbackService?.playPrevious()
    }

    fun seekTo(positionMs: Int) {
        playbackService?.seekTo(positionMs)
    }

    fun toggleLikeSong(song: Song) {
        viewModelScope.launch {
            repository.toggleLike(song)
            // If the song is currently playing, update its state context if appropriate
            _currentPlayingSong.value?.let { current ->
                if (current.id == song.id) {
                    _currentPlayingSong.value = current.copy(isLiked = !current.isLiked)
                }
            }
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}
