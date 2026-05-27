package com.example.playback

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.Song
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class MusicPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val binder = LocalBinder()

    // Playlist state
    private var currentPlaylist: List<Song> = emptyList()
    private var currentSongIndex: Int = -1

    // Flows for tracking media state across the app
    private val _currentPlayingSong = MutableStateFlow<Song?>(null)
    val currentPlayingSong: StateFlow<Song?> = _currentPlayingSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    // Track active song cover bitmap for notification
    private var currentCoverBitmap: Bitmap? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "YT_MUSIC_PLAYBACK_CHANNEL"

        const val ACTION_PLAY = "com.example.playback.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.playback.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.playback.ACTION_NEXT"
        const val ACTION_PREV = "com.example.playback.ACTION_PREV"
        const val ACTION_CLOSE = "com.example.playback.ACTION_CLOSE"
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaPlayer()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> resumePlayback()
            ACTION_PAUSE -> pausePlayback()
            ACTION_NEXT -> playNext()
            ACTION_PREV -> playPrevious()
            ACTION_CLOSE -> stopService()
        }
        return START_NOT_STICKY
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { mp ->
                mp.start()
                _isPlaying.value = true
                _duration.value = mp.duration
                startProgressTracker()
                updateNotification()
            }
            setOnCompletionListener {
                playNext()
            }
            setOnErrorListener { _, _, _ ->
                _isPlaying.value = false
                stopProgressTracker()
                true
            }
        }
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        currentPlaylist = songs
        currentSongIndex = startIndex.coerceIn(0, songs.size - 1)
        playSong(currentPlaylist[currentSongIndex])
    }

    fun playSong(song: Song) {
        _currentPlayingSong.value = song
        _isPlaying.value = false
        _currentPosition.value = 0
        _duration.value = song.durationSeconds * 1000 // Fallback estimate until prepared

        stopProgressTracker()

        serviceScope.launch {
            var playUrl = song.streamUrl
            
            // Resolve direct YouTube music stream URL if song id is a YouTube video ID
            val isYtId = song.id.length >= 8 && !song.id.startsWith("trend") && !song.id.startsWith("saved") && !song.id.startsWith("local")
            if (isYtId) {
                val resolvedUrl = withContext(Dispatchers.IO) {
                    resolvePipedStreamUrl(song.id)
                }
                if (resolvedUrl != null) {
                    playUrl = resolvedUrl
                }
            }

            mediaPlayer?.let { player ->
                try {
                    player.reset()
                    player.setDataSource(playUrl)
                    player.prepareAsync() // Asynchronous preparation for online streaming
                    _isPlaying.value = false // Set temporarily to false during load
                    updateNotification()

                    // Load cover art in background to show in notification
                    loadCoverBitmap(song.coverUrl)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun resolvePipedStreamUrl(videoId: String): String? {
        val instances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.colby.land",
            "https://piped-api.lunar.icu",
            "https://api-piped.mha.fi"
        )
        for (baseUrl in instances) {
            try {
                val urlStr = "$baseUrl/streams/$videoId"
                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    if (json.has("audioStreams")) {
                        val audioStreams = json.getJSONArray("audioStreams")
                        if (audioStreams.length() > 0) {
                            // Find the first audio stream, preferably M4A/MP4 or opus
                            var bestUrl: String? = null
                            for (i in 0 until audioStreams.length()) {
                                val stream = audioStreams.getJSONObject(i)
                                val streamUrl = stream.getString("url")
                                val mime = stream.optString("mimeType", "")
                                if (mime.contains("audio/mp4") || mime.contains("m4a")) {
                                    return streamUrl
                                }
                                if (bestUrl == null) {
                                    bestUrl = streamUrl
                                }
                            }
                            if (bestUrl != null) {
                                return bestUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun resumePlayback() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                startProgressTracker()
                updateNotification()
            }
        }
    }

    fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopProgressTracker()
                updateNotification()
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun playNext() {
        if (currentPlaylist.isEmpty()) return
        currentSongIndex = (currentSongIndex + 1) % currentPlaylist.size
        playSong(currentPlaylist[currentSongIndex])
    }

    fun playPrevious() {
        if (currentPlaylist.isEmpty()) return
        currentSongIndex = if (currentSongIndex - 1 < 0) currentPlaylist.size - 1 else currentSongIndex - 1
        playSong(currentPlaylist[currentSongIndex])
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun loadCoverBitmap(urlStr: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlStr)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)
                withContext(Dispatchers.Main) {
                    currentCoverBitmap = bitmap
                    updateNotification()
                }
            } catch (e: Exception) {
                currentCoverBitmap = null
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background playback control notification"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val song = _currentPlayingSong.value ?: return

        // PendingIntent for clicking notification to open our app
        val mainActivityIntent = (packageManager.getLaunchIntentForPackage(packageName) ?: Intent(this, MainActivity::class.java)).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Action PendingIntents
        val playIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PLAY }
        val playPendingIntent = PendingIntent.getService(this, 1, playIntent, PendingIntent.FLAG_IMMUTABLE)

        val pauseIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_IMMUTABLE)

        val prevIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(this, 4, prevIntent, PendingIntent.FLAG_IMMUTABLE)

        val closeIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_CLOSE }
        val closePendingIntent = PendingIntent.getService(this, 5, closeIntent, PendingIntent.FLAG_IMMUTABLE)

        val isMusicPlaying = _isPlaying.value

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album)
            .setLargeIcon(currentCoverBitmap)
            .setContentIntent(mainActivityPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add Playback Control Buttons
        notificationBuilder.addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
        if (isMusicPlaying) {
            notificationBuilder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        } else {
            notificationBuilder.addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
        }
        notificationBuilder.addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
        notificationBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", closePendingIntent)

        // Set style
        notificationBuilder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
        )

        try {
            val notification = notificationBuilder.build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        _isPlaying.value = false
        stopProgressTracker()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopProgressTracker()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
