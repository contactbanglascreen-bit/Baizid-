package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.TvChannel
import com.example.data.remote.TvChannels
import com.example.data.remote.TvHealth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ui.components.glassAccent
import com.example.ui.components.glassBackground
import com.example.ui.components.glassBubble
import com.example.ui.components.liquidGlass
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val tilePalettes = listOf(
    listOf(Color(0xFFFF5A6A), Color(0xFFB0102A)), listOf(Color(0xFF4FACFE), Color(0xFF1A4F9C)),
    listOf(Color(0xFF43E97B), Color(0xFF15803D)), listOf(Color(0xFFFFB347), Color(0xFFC2410C)),
    listOf(Color(0xFFA78BFA), Color(0xFF5B21B6)), listOf(Color(0xFFF472B6), Color(0xFF9D174D)),
    listOf(Color(0xFF2DD4BF), Color(0xFF0F766E)), listOf(Color(0xFFFACC15), Color(0xFFA16207))
)

/** Live TV: Bangladeshi and other channels from the playlist, grouped, with search. */
@Composable
fun TvScreen(onPlay: (TvChannel) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var channels by remember { mutableStateOf<List<TvChannel>?>(null) }
    var group by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable { mutableStateOf("") }
    var checking by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(Unit) {
        TvHealth.lastChecked?.let { channels = it; return@LaunchedEffect }
        val list = TvChannels.load(context)
        channels = TvHealth.applyKnown(context, list)
        // Official logos are filled in as soon as they are ready (first time needs a download).
        val withLogos = TvChannels.withLogos(context, list)
        channels = TvHealth.applyKnown(context, withLogos)
        // Only working channels are kept: every link is tested from this phone.
        if (!TvHealth.enabled) return@LaunchedEffect
        checking = 0 to withLogos.size
        val checked = TvHealth.check(context, withLogos) { done, total, current ->
            withContext(Dispatchers.Main) {
                checking = done to total
                channels = current
            }
        }
        channels = checked
        checking = null
    }

    val all = channels.orEmpty()
    val groups = remember(all) { TvChannels.groups.filter { g -> g == "All" || all.any { g in it.tags } } }
    val shown = remember(all, group, query) {
        all.filter { (group == "All" || group in it.tags) && (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true)) }
    }

    Column(modifier.fillMaxSize().glassBackground()) {
        // Header
        Column(
            Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                .statusBarsPadding()
                .padding(top = 10.dp, bottom = 12.dp)
        ) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).glassAccent(RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.LiveTv, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Live TV", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text(
                        checking?.let { (d, t) -> "${all.size} চ্যানেল • চালু কিনা দেখা হচ্ছে $d/$t" } ?: "${all.size} চালু চ্যানেল • 24/7",
                        color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    Modifier.clip(CircleShape).background(CinetorRed).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Spacer(Modifier.width(5.dp))
                    Text("LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().glassBubble(RoundedCornerShape(18.dp)).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                TextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    placeholder = { Text("চ্যানেল খুঁজুন…", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = CinetorRed
                    )
                )
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups, key = { it }) { g ->
                    val sel = g == group
                    Box(
                        Modifier.height(34.dp)
                            .then(if (sel) Modifier.glassAccent(CircleShape) else Modifier.glassBubble(CircleShape))
                            .clickable { group = g }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(g, color = if (sel) Color.White else TextPrimary, fontSize = 13.5.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }

        when {
            channels == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CinetorRed)
            }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো চ্যানেল পাওয়া যায়নি", color = TextSecondary)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(if (group == "All") "সব চ্যানেল" else group, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                }
                items(shown, key = { it.id }) { ch -> ChannelTile(ch) { onPlay(ch) } }
            }
        }
    }
}

@Composable
private fun ChannelTile(ch: TvChannel, onClick: () -> Unit) {
    val colors = tilePalettes[ch.name.hashCode().mod(tilePalettes.size)]
    var logoFailed by remember(ch.logo) { mutableStateOf(false) }
    val showLogo = ch.logo.isNotBlank() && !logoFailed
    Column(
        Modifier.glassBubble(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(14.dp))
                .background(
                    if (showLogo) Brush.linearGradient(listOf(Color(0xFFF7F7FA), Color(0xFFE4E6EE)))
                    else Brush.linearGradient(colors)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (showLogo) {
                AsyncImage(
                    model = ch.logo,
                    contentDescription = ch.name,
                    contentScale = ContentScale.Fit,
                    onError = { logoFailed = true },
                    modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 8.dp)
                )
            } else {
                val initials = ch.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
                Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp, modifier = Modifier.padding(top = 12.dp))
            }
            Row(
                Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFFF3B3B)))
                Spacer(Modifier.width(3.dp))
                Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(ch.name, color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            ch.sources.firstOrNull()?.quality?.takeIf { it != "Live" } ?: ch.group,
            color = TextMuted, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}
