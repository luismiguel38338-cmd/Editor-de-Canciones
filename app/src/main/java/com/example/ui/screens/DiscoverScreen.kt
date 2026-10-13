package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.theme.MeloTurquoise
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class OnlineTrackResult(
    val identifier: String,
    val title: String,
    val creator: String,
    val year: String?,
    val detailsUrl: String,
    val audioStreamUrl: String
)

@Composable
fun DiscoverScreen(
    onPlayOnlineSong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Archive.org, 1: Servicios Externos

    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<OnlineTrackResult>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    fun performSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            hasSearched = false
            errorMessage = null
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            hasSearched = true

            try {
                val results = withContext(Dispatchers.IO) {
                    val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                    val apiUrl = "https://archive.org/advancedsearch.php?q=${encoded}+AND+mediatype:audio&fl[]=identifier,title,creator,description,year&sort[]=downloads+desc&rows=25&output=json"

                    val connection = URL(apiUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "MeloTune-Android/2.0")

                    if (connection.responseCode == 200) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = reader.readText()
                        reader.close()

                        val json = JSONObject(response)
                        val docs = json.getJSONObject("response").getJSONArray("docs")
                        val list = mutableListOf<OnlineTrackResult>()

                        for (i in 0 until docs.length()) {
                            val item = docs.getJSONObject(i)
                            val id = item.optString("identifier", "")
                            if (id.isNotBlank()) {
                                val title = item.optString("title", id)
                                val creator = item.optString("creator", "Música libre / Dominio público")
                                val year = item.optString("year", null)
                                val details = "https://archive.org/details/$id"
                                val stream = "https://archive.org/download/$id/${id}_vbr.mp3"

                                list.add(
                                    OnlineTrackResult(
                                        identifier = id,
                                        title = if (title.isBlank()) id else title,
                                        creator = if (creator.isBlank()) "Artista Desconocido" else creator,
                                        year = year,
                                        detailsUrl = details,
                                        audioStreamUrl = stream
                                    )
                                )
                            }
                        }
                        list
                    } else {
                        throw Exception("Servidor respondió con código ${connection.responseCode}")
                    }
                }

                searchResults = results
            } catch (e: Exception) {
                errorMessage = "No se pudo conectar (${e.localizedMessage ?: "Error de red"})."
                searchResults = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBg)
    ) {
        // Cabecera compacta
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Descubrir",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            if (selectedTab == 0 && searchQuery.isNotBlank()) {
                Button(
                    onClick = { performSearch(searchQuery) },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MeloTurquoise,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("execute_discover_search")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Buscar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Buscador sencillo
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar en fuentes autorizadas...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        searchResults = emptyList()
                        hasSearched = false
                        errorMessage = null
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .height(48.dp)
                .testTag("discover_search_input"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StudioSurface,
                unfocusedContainerColor = StudioSurface,
                focusedBorderColor = MeloTurquoise.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Pestañas sutiles
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MeloTurquoise,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MeloTurquoise,
                    height = 2.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Catálogo Abierto",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) MeloTurquoise else TextSecondary
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Servicios Externos",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) MeloTurquoise else TextSecondary
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (selectedTab == 0) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MeloTurquoise, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Buscando en Archive.org...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "No se pudo conectar",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                hasSearched && searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin resultados para \"$searchQuery\". MeloTune no genera canciones ficticias.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                !hasSearched -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Introduce un término de búsqueda para consultar obras de dominio público y Creative Commons.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        items(searchResults, key = { it.identifier }) { item ->
                            CleanOnlineTrackRow(
                                track = item,
                                onPlay = {
                                    val onlineSong = Song(
                                        id = item.identifier.hashCode().toLong(),
                                        title = item.title,
                                        artist = item.creator,
                                        album = "Archive.org",
                                        durationMs = 0L,
                                        uri = Uri.parse(item.audioStreamUrl),
                                        albumArtUri = null,
                                        folderName = "En línea",
                                        isFavorite = false,
                                        originalTitle = item.title,
                                        originalArtist = item.creator
                                    )
                                    onPlayOnlineSong(onlineSong)
                                },
                                onOpenDetails = {
                                    openUrl(context, item.detailsUrl)
                                }
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                        }
                    }
                }
            }
        } else {
            // Servicios Externos en lista limpia
            val queryOrMusic = if (searchQuery.isNotBlank()) searchQuery else "música"
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                item {
                    CleanExternalServiceRow(
                        name = "Spotify",
                        subtitle = "Catálogo oficial",
                        onClick = {
                            val encoded = URLEncoder.encode(queryOrMusic, "UTF-8")
                            openIntentOrWeb(context, "spotify:search:$encoded", "https://open.spotify.com/search/$encoded")
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                }

                item {
                    CleanExternalServiceRow(
                        name = "YouTube Music",
                        subtitle = "Búsqueda oficial",
                        onClick = {
                            val encoded = URLEncoder.encode(queryOrMusic, "UTF-8")
                            openUrl(context, "https://music.youtube.com/search?q=$encoded")
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                }

                item {
                    CleanExternalServiceRow(
                        name = "SoundCloud",
                        subtitle = "Artistas independientes",
                        onClick = {
                            val encoded = URLEncoder.encode(queryOrMusic, "UTF-8")
                            openUrl(context, "https://soundcloud.com/search?q=$encoded")
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                }

                item {
                    CleanExternalServiceRow(
                        name = "Bandcamp",
                        subtitle = "Música directa de creadores",
                        onClick = {
                            val encoded = URLEncoder.encode(queryOrMusic, "UTF-8")
                            openUrl(context, "https://bandcamp.com/search?q=$encoded")
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                }

                item {
                    CleanExternalServiceRow(
                        name = "Jamendo",
                        subtitle = "Licencias Creative Commons",
                        onClick = {
                            val encoded = URLEncoder.encode(queryOrMusic, "UTF-8")
                            openUrl(context, "https://www.jamendo.com/search?qs=q=$encoded")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanOnlineTrackRow(
    track: OnlineTrackResult,
    onPlay: () -> Unit,
    onOpenDetails: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(StudioSurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                tint = MeloTurquoise,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.creator + if (track.year != null) " • ${track.year}" else "",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onOpenDetails,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Detalles",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onPlay,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MeloTurquoise)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Reproducir",
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CleanExternalServiceRow(
    name: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(StudioSurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MeloTurquoise,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            )
        }

        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun openIntentOrWeb(context: Context, appUri: String, webUrl: String) {
    try {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appUri)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(appIntent)
    } catch (_: Exception) {
        openUrl(context, webUrl)
    }
}
