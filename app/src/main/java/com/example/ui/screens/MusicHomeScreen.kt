package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.ScreenState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MusicHomeScreen(
    viewModel: MusicViewModel,
    userEmail: String = "bodruddoja4@gmail.com"
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentSong by viewModel.currentPlayingSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    var showFullPlayer by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (!showFullPlayer) {
                Column {
                    // Persistent Mini player bar if a song is loaded
                    if (currentSong != null) {
                        MiniPlayerBar(
                            song = currentSong!!,
                            isPlaying = isPlaying,
                            position = currentPosition,
                            duration = duration,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNext() },
                            onClick = { showFullPlayer = true }
                        )
                    }

                    // Bottom Tab Navigation Bar
                    NavigationBar(
                        containerColor = CharcoalDark,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBarItem(
                            selected = currentScreen is ScreenState.Home,
                            onClick = { viewModel.navigateTo(ScreenState.Home) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = RedYTMVariant,
                                unselectedIconColor = DarkGreyText,
                                unselectedTextColor = DarkGreyText
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen is ScreenState.Search,
                            onClick = { viewModel.navigateTo(ScreenState.Search) },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = RedYTMVariant,
                                unselectedIconColor = DarkGreyText,
                                unselectedTextColor = DarkGreyText
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen is ScreenState.Library,
                            onClick = { viewModel.navigateTo(ScreenState.Library) },
                            icon = { Icon(Icons.Default.List, contentDescription = "Library") },
                            label = { Text("Library", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = RedYTMVariant,
                                unselectedIconColor = DarkGreyText,
                                unselectedTextColor = DarkGreyText
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                is ScreenState.Home -> HomeTab(viewModel = viewModel, userEmail = userEmail)
                is ScreenState.Search -> SearchTab(viewModel = viewModel)
                is ScreenState.Library -> LibraryTab(viewModel = viewModel)
            }

            // Expandable Full-Screen Music Player Overlay
            AnimatedVisibility(
                visible = showFullPlayer && currentSong != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 300)
                )
            ) {
                if (currentSong != null) {
                    FullPlayerOverlay(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        position = currentPosition,
                        duration = duration,
                        viewModel = viewModel,
                        onSeek = { viewModel.seekTo(it) },
                        onClose = { showFullPlayer = false }
                    )
                }
            }
        }
    }
}

// ---------------- HOME TAB COMPONENT ----------------

@Composable
fun HomeTab(viewModel: MusicViewModel, userEmail: String) {
    val trendingFeed by viewModel.trendingFeed.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Upper logo and header bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .background(RedYTM, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Music",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Profile Bubble
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = RedYTMVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userEmail.substringBefore("@").take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Chip categories of mood
        item {
            val moods = listOf("Relax", "Energize", "Focus", "Commute", "Workout")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(moods) { mood ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CharcoalDark,
                        modifier = Modifier.clickable {
                            viewModel.updateSearchQuery(mood)
                            viewModel.navigateTo(ScreenState.Search)
                            viewModel.performSearch()
                        }
                    ) {
                        Text(
                            text = mood,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Mixed recommendations Section
        item {
            Text(
                text = "Mixed for You",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(trendingFeed) { song ->
                    Column(
                        modifier = Modifier
                            .width(140.dp)
                            .clickable { viewModel.playSong(song, trendingFeed) }
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CharcoalDark),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                        Text(
                            text = song.artist,
                            color = DarkGreyText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Trending songs title list
        item {
            Text(
                text = "Trending Songs",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
            )
        }

        items(trendingFeed) { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.playSong(song, trendingFeed) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CharcoalDark),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist}  •  ${song.album}",
                        color = DarkGreyText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.toggleLikeSong(song) }) {
                    Icon(
                        imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (song.isLiked) RedYTM else Color.White
                    )
                }
            }
        }
    }
}

// ---------------- SEARCH TAB COMPONENT ----------------

@Composable
fun SearchTab(viewModel: MusicViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // YT Styled Red Outlined Search Input Field
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search YouTube Music...", color = DarkGreyText) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field_input"),
            trailingIcon = {
                IconButton(onClick = { viewModel.performSearch() }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = RedYTM)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = RedYTM,
                unfocusedBorderColor = CharcoalLight,
                focusedContainerColor = CharcoalDark,
                unfocusedContainerColor = CharcoalDark
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RedYTM)
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = CharcoalLight
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Enter a song, artist, or vibe to stream from YouTube",
                        color = DarkGreyText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(results) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playSong(song, results) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CharcoalDark),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${song.artist}  •  ${song.category}",
                                color = DarkGreyText,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.toggleLikeSong(song) }) {
                            val isLiked = song.isLiked
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) RedYTM else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- LIBRARY / PLAYLISTS TAB COMPONENT ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryTab(viewModel: MusicViewModel) {
    val likedSongs by viewModel.likedSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var playlistDesc by remember { mutableStateOf("") }

    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Live Playlist", color = Color.White) },
            containerColor = CharcoalDark,
            text = {
                Column {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedYTM,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = playlistDesc,
                        onValueChange = { playlistDesc = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedYTM,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName, playlistDesc)
                            playlistName = ""
                            playlistDesc = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("CREATE", color = RedYTM)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("CANCEL", color = Color.White)
                }
            }
        )
    }

    if (selectedPlaylistForDetail != null) {
        PlaylistDetailScreen(
            playlist = selectedPlaylistForDetail!!,
            viewModel = viewModel,
            onBack = { selectedPlaylistForDetail = null }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // My Library section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Library",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Playlist", tint = RedYTM)
                    }
                }
            }

            // Custom Playlists Title
            item {
                Text(
                    text = "Playlists",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (playlists.isEmpty()) {
                item {
                    Text(
                        text = "No custom playlists created. Tap the '+' icon above to create one.",
                        color = DarkGreyText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(playlists) { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { selectedPlaylistForDetail = pl },
                                onLongClick = { viewModel.deletePlaylist(pl) }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = pl.coverUrl,
                            contentDescription = pl.name,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CharcoalLight),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.name,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = pl.description.ifEmpty { "Custom YouTube Mixed Playlist" },
                                color = DarkGreyText,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.deletePlaylist(pl) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = DarkGreyText)
                        }
                    }
                }
            }

            // Liked Songs Section
            item {
                Text(
                    text = "Liked Songs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                )
            }

            if (likedSongs.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = CharcoalLight,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Songs you've liked will show up here.",
                            color = DarkGreyText,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(likedSongs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playSong(song, likedSongs) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CharcoalDark),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = DarkGreyText,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.toggleLikeSong(song) }) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Liked",
                                tint = RedYTM
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val trendingFeed by viewModel.trendingFeed.collectAsState()
    val likedSongs by viewModel.likedSongs.collectAsState()
    
    // Resolve matching playlist songs
    val playlistSongs = remember(playlist.songIdsJson, trendingFeed, likedSongs) {
        val list = mutableListOf<Song>()
        val ids = try {
            val jsonArray = org.json.JSONArray(playlist.songIdsJson)
            val temp = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                temp.add(jsonArray.getString(i))
            }
            temp
        } catch (e: Exception) {
            emptyList()
        }
        
        // Find in standard pools or mock
        val allAvailable = trendingFeed + likedSongs
        for (id in ids) {
            val found = allAvailable.find { it.id == id }
            if (found != null) {
                list.add(found)
            } else {
                list.add(
                    Song(
                        id = id,
                        title = "Saved Track $id",
                        artist = "Stored Sourced Artist",
                        streamUrl = "https://assets.mixkit.co/music/preview/mixkit-sleepy-cat-135.mp3",
                        coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80&fit=crop",
                    )
                )
            }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = playlist.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (playlistSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No songs added to this playlist yet.\n(During active play, tap More Options on any track to append live).",
                    color = DarkGreyText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(playlistSongs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playSong(song, playlistSongs) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CharcoalDark),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = song.artist,
                                color = DarkGreyText,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { viewModel.removeSongFromPlaylist(playlist.id, song.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RedYTM)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- MINI PLAYER INTERFACE ----------------

@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    position: Int,
    duration: Int,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalDark)
            .clickable { onClick() }
    ) {
        // Slim running glowing slider bar
        LinearProgressIndicator(
            progress = { progress },
            color = RedYTM,
            trackColor = CharcoalLight,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CharcoalDark),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = DarkGreyText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.scale(if (isPlaying) 1f else 1.2f)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
            }
        }
    }
}

// ---------------- FULL MUSIC PLAYER SHEET OVERLAY ----------------

@Composable
fun FullPlayerOverlay(
    song: Song,
    isPlaying: Boolean,
    position: Int,
    duration: Int,
    viewModel: MusicViewModel,
    onSeek: (Int) -> Unit,
    onClose: () -> Unit
) {
    var showPlaylistChooser by remember { mutableStateOf(false) }
    val playlists by viewModel.playlists.collectAsState()

    var showLyricsTab by remember { mutableStateOf(false) }

    // Parse synchronized timed lyrics
    val animatedLyricLines = remember(song.lyrics) { parseLyrics(song.lyrics) }
    val activeLyricIndex = remember(position, animatedLyricLines) {
        val index = animatedLyricLines.indexOfLast { position >= it.timestampMs }
        if (index == -1) 0 else index
    }

    if (showPlaylistChooser) {
        AlertDialog(
            onDismissRequest = { showPlaylistChooser = false },
            title = { Text("Add Track to Playlist", color = Color.White) },
            containerColor = CharcoalDark,
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists created. Go to Library tab to create one.", color = DarkGreyText)
                } else {
                    LazyColumn {
                        items(playlists) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(pl.id, song.id)
                                        showPlaylistChooser = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pl.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CharcoalLight,
                        PureBlack
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                
                // Audio Lyrics/Video switch tabs
                Row(
                    modifier = Modifier
                        .background(CharcoalLight, RoundedCornerShape(16.dp))
                        .padding(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (!showLyricsTab) RedYTMVariant else Color.Transparent,
                        modifier = Modifier.clickable { showLyricsTab = false }
                    ) {
                        Text("Song", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (showLyricsTab) RedYTMVariant else Color.Transparent,
                        modifier = Modifier.clickable { showLyricsTab = true }
                    ) {
                        Text("Lyrics", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                }

                IconButton(onClick = { showPlaylistChooser = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Add to playlist", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            if (!showLyricsTab) {
                // Heartbeat disk layout
                val infiniteTransition = rememberInfiniteTransition(label = "Vinyl rotation")
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(15000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "Vinyl rotation degrees"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CharcoalDark)
                ) {
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = "Cover",
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(if (isPlaying) rotationAngle else 0f),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                // TIMED SYNCHRONIZED LYRICS CONTAINER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CharcoalDark.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(animatedLyricLines.size) { index ->
                            val line = animatedLyricLines[index]
                            val isActive = index == activeLyricIndex
                            
                            val lyricColor = if (isActive) Color.White else DarkGreyText
                            val lyricScale = if (isActive) 1.1f else 0.95f
                            val lyricWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                            Text(
                                text = line.text,
                                color = lyricColor,
                                fontSize = 18.sp,
                                fontWeight = lyricWeight,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .scale(lyricScale)
                                    .clickable { onSeek(line.timestampMs) }
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Title, artist, and Like/Add actions row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkGreyText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleLikeSong(song) }) {
                        Icon(
                            imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (song.isLiked) RedYTM else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = { showPlaylistChooser = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Playlist",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Seekbar slider controls
            Slider(
                value = position.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = RedYTM,
                    activeTrackColor = RedYTM,
                    inactiveTrackColor = CharcoalLight
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(position), color = DarkGreyText, fontSize = 12.sp)
                Text(formatTime(duration), color = DarkGreyText, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Controls buttons bar (Shuffle, Prev, Play/Pause circle, Next, Repeat)
            var shuffleActive by remember { mutableStateOf(false) }
            var repeatActive by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { shuffleActive = !shuffleActive }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleActive) RedYTM else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = { viewModel.playPrev() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Prev",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Primary Playpause container
                Surface(
                    shape = RoundedCornerShape(40.dp),
                    color = Color.White,
                    modifier = Modifier
                        .size(72.dp)
                        .clickable { viewModel.togglePlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { repeatActive = !repeatActive }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatActive) RedYTM else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Bottom Real YouTube Music style tabs indicator (UP NEXT | LYRICS | RELATED)
            Spacer(modifier = Modifier.weight(0.1f))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UP NEXT",
                    color = Color.White.copy(alpha = if (showLyricsTab) 0.5f else 1.0f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showLyricsTab = false }
                        .padding(8.dp)
                )
                Text(
                    text = "LYRICS",
                    color = Color.White.copy(alpha = if (showLyricsTab) 1.0f else 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showLyricsTab = true }
                        .padding(8.dp)
                )
                Text(
                    text = "RELATED",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { /* decorative/related feature if needed */ }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}

// Timed lyric modeling
data class LyricLine(val timestampMs: Int, val text: String)

fun parseLyrics(lyricsStr: String): List<LyricLine> {
    val lines = lyricsStr.split("\n")
    val list = mutableListOf<LyricLine>()
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            val stampStr = trimmed.substringAfter("[").substringBefore("]")
            val lyricText = trimmed.substringAfter("]").trim()
            val parts = stampStr.split(":")
            if (parts.size == 2) {
                val mins = parts[0].toIntOrNull() ?: 0
                val secs = parts[1].toIntOrNull() ?: 0
                val totalMs = (mins * 60 + secs) * 1000
                list.add(LyricLine(totalMs, lyricText))
            }
        } else if (trimmed.isNotEmpty()) {
            list.add(LyricLine(0, trimmed))
        }
    }
    return list.sortedBy { it.timestampMs }
}

fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
