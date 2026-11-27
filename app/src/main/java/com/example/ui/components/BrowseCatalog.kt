package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** A language or country category (MovieBox-style browsing). */
data class BrowseTarget(
    val kind: String,          // "language" or "country"
    val value: String,
    val label: String,
    val flag: String,
    val colors: List<Color>,
    val country: String = "",  // sent to the catalog filter
    val keywords: List<String> = emptyList(),
    val countryWords: List<String> = emptyList(),
    val languageWords: List<String> = emptyList()
) {
    fun matches(item: MediaItem): Boolean {
        if (kind == "language") return BrowseCatalog.languageOf(item) == value
        val c = item.country.lowercase()
        val lang = (item.languageBadge + " " + item.title).lowercase()
        val byCountry = countryWords.any { it.lowercase() in c }
        val byLanguage = languageWords.any { it.lowercase() in lang }
        return byCountry || byLanguage
    }
}

object BrowseCatalog {
    val languages = listOf(
        BrowseTarget("language", "bangla", "বাংলা", "🇧🇩", listOf(Color(0xFF16A34A), Color(0xFFDC2626)), "Bangladesh",
            listOf("Bangla", "Bengali"), listOf("bangladesh"), listOf("bangla", "bengali")),
        BrowseTarget("language", "hindi", "Hindi", "🇮🇳", listOf(Color(0xFFF97316), Color(0xFF15803D)), "India",
            listOf("Hindi"), emptyList(), listOf("hindi")),
        BrowseTarget("language", "english", "English", "🇺🇸", listOf(Color(0xFF2563EB), Color(0xFFB91C1C)), "United States",
            listOf("English"), listOf("united states", "usa", "united kingdom", "uk"), listOf("english")),
        BrowseTarget("language", "korean", "Korean", "🇰🇷", listOf(Color(0xFF1D4ED8), Color(0xFFE11D48)), "South Korea",
            listOf("Korean"), listOf("korea"), listOf("korean")),
        BrowseTarget("language", "chinese", "Chinese", "🇨🇳", listOf(Color(0xFFDC2626), Color(0xFFF59E0B)), "China",
            listOf("Chinese"), listOf("china", "taiwan", "hong kong"), listOf("chinese", "mandarin", "cantonese")),
        BrowseTarget("language", "japanese", "Japanese", "🇯🇵", listOf(Color(0xFFE11D48), Color(0xFF64748B)), "Japan",
            listOf("Japanese"), listOf("japan"), listOf("japanese")),
        BrowseTarget("language", "tamil", "Tamil", "🎬", listOf(Color(0xFFDB2777), Color(0xFF7C3AED)), "India",
            listOf("Tamil"), emptyList(), listOf("tamil")),
        BrowseTarget("language", "telugu", "Telugu", "🎞️", listOf(Color(0xFF0EA5E9), Color(0xFF6D28D9)), "India",
            listOf("Telugu"), emptyList(), listOf("telugu")),
        BrowseTarget("language", "turkish", "Turkish", "🇹🇷", listOf(Color(0xFFDC2626), Color(0xFF7F1D1D)), "Turkey",
            listOf("Turkish"), listOf("turkey", "türkiye"), listOf("turkish")),
        BrowseTarget("language", "spanish", "Spanish", "🇪🇸", listOf(Color(0xFFF59E0B), Color(0xFFB91C1C)), "Spain",
            listOf("Spanish"), listOf("spain", "mexico"), listOf("spanish")),
        BrowseTarget("language", "thai", "Thai", "🇹🇭", listOf(Color(0xFF1E3A8A), Color(0xFFDC2626)), "Thailand",
            listOf("Thai"), listOf("thailand"), listOf("thai"))
    )

    val countries = listOf(
        BrowseTarget("country", "bangladesh", "Bangladesh", "🇧🇩", listOf(Color(0xFF16A34A), Color(0xFF14532D)), "Bangladesh",
            listOf("Bangla"), listOf("bangladesh"), listOf("bangla")),
        BrowseTarget("country", "india", "India", "🇮🇳", listOf(Color(0xFFF97316), Color(0xFF9A3412)), "India",
            listOf("Hindi", "Bollywood"), listOf("india"), emptyList()),
        BrowseTarget("country", "usa", "USA", "🇺🇸", listOf(Color(0xFF2563EB), Color(0xFF1E3A8A)), "United States",
            listOf("Hollywood"), listOf("united states", "usa"), emptyList()),
        BrowseTarget("country", "uk", "UK", "🇬🇧", listOf(Color(0xFF1D4ED8), Color(0xFF991B1B)), "United Kingdom",
            listOf("British"), listOf("united kingdom", "uk"), emptyList()),
        BrowseTarget("country", "korea", "Korea", "🇰🇷", listOf(Color(0xFFE11D48), Color(0xFF1E40AF)), "South Korea",
            listOf("Korean drama"), listOf("korea"), listOf("korean")),
        BrowseTarget("country", "china", "China", "🇨🇳", listOf(Color(0xFFDC2626), Color(0xFF7F1D1D)), "China",
            listOf("Chinese drama"), listOf("china"), listOf("chinese")),
        BrowseTarget("country", "japan", "Japan", "🇯🇵", listOf(Color(0xFFF43F5E), Color(0xFF881337)), "Japan",
            listOf("Japanese"), listOf("japan"), listOf("japanese")),
        BrowseTarget("country", "turkey", "Turkey", "🇹🇷", listOf(Color(0xFFEF4444), Color(0xFF7F1D1D)), "Turkey",
            listOf("Turkish"), listOf("turkey"), listOf("turkish")),
        BrowseTarget("country", "pakistan", "Pakistan", "🇵🇰", listOf(Color(0xFF15803D), Color(0xFF14532D)), "Pakistan",
            listOf("Pakistani drama"), listOf("pakistan"), listOf("urdu")),
        BrowseTarget("country", "thailand", "Thailand", "🇹🇭", listOf(Color(0xFF1E40AF), Color(0xFF991B1B)), "Thailand",
            listOf("Thai"), listOf("thailand"), listOf("thai")),
        BrowseTarget("country", "france", "France", "🇫🇷", listOf(Color(0xFF2563EB), Color(0xFFDC2626)), "France",
            listOf("French"), listOf("france"), listOf("french"))
    )

    /**
     * Puts every title in one language automatically: a language tag in the title ("[Hindi]")
     * wins, then the audio/language badge, then the country it comes from.
     */
    fun languageOf(item: MediaItem): String? {
        val tag = Regex("\\[([^\\]]+)]").findAll(item.title).joinToString(" ") { it.groupValues[1] }.lowercase()
        val badge = item.languageBadge.lowercase()
        fun pick(text: String): String? = when {
            text.isBlank() -> null
            "bangla" in text || "bengali" in text || "বাংলা" in text -> "bangla"
            "hindi" in text -> "hindi"
            "tamil" in text -> "tamil"
            "telugu" in text -> "telugu"
            "korean" in text -> "korean"
            "chinese" in text || "mandarin" in text || "cantonese" in text -> "chinese"
            "japanese" in text -> "japanese"
            "turkish" in text -> "turkish"
            "spanish" in text -> "spanish"
            "thai" in text -> "thai"
            "english" in text -> "english"
            else -> null
        }
        pick(tag)?.let { return it }
        // first language in the badge ("English,Es" -> english)
        pick(badge.substringBefore(',').substringBefore('/'))?.let { return it }
        val c = item.country.lowercase()
        return when {
            "bangladesh" in c -> "bangla"
            "india" in c -> "hindi"
            "korea" in c -> "korean"
            "china" in c || "taiwan" in c || "hong kong" in c -> "chinese"
            "japan" in c -> "japanese"
            "turkey" in c || "türkiye" in c -> "turkish"
            "thailand" in c -> "thai"
            "spain" in c || "mexico" in c || "argentina" in c -> "spanish"
            "united states" in c || "usa" in c || "united kingdom" in c || c == "uk" || "canada" in c || "australia" in c -> "english"
            else -> pick(badge)
        }
    }

    fun find(kind: String, value: String): BrowseTarget? =
        (if (kind == "country") countries else languages).firstOrNull { it.value == value }

    /** The category page that is open right now (null = closed). */
    var open by mutableStateOf<BrowseTarget?>(null)
}

/** Row of flag tiles; tapping one opens its category page. */
@Composable
fun BrowseTiles(title: String, targets: List<BrowseTarget>) {
    Column {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(targets.distinctBy { it.kind + it.value }, key = { it.kind + it.value }) { t ->
                Box(
                    Modifier
                        .width(112.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(t.colors))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                        .clickable { BrowseCatalog.open = t }
                        .padding(10.dp)
                ) {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent))))
                    Text(
                        t.label.take(1).uppercase(),
                        color = Color.White.copy(alpha = 0.22f), fontWeight = FontWeight.Black, fontSize = 44.sp,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    Text(t.label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.align(Alignment.BottomStart))
                }
            }
        }
    }
}

/** Full-screen category page: type tabs, poster grid, load more. */
@Composable
fun BrowsePage(
    target: BrowseTarget,
    load: suspend (type: String, page: Int) -> List<MediaItem>,
    onOpen: (MediaItem) -> Unit,
    onClose: () -> Unit
) {
    var type by remember { mutableStateOf("All") }
    var page by remember(type) { mutableIntStateOf(1) }
    var items by remember(type) { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember(type) { mutableStateOf(true) }
    var done by remember(type) { mutableStateOf(false) }
    LaunchedEffect(type, page) {
        loading = true
        val more = load(type, page)
        val before = items.size
        items = (items + more).distinctBy { it.title.lowercase().trim() }
        if (items.size == before) done = true
        loading = false
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(Modifier.fillMaxSize().glassBackground()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(target.colors.first().copy(alpha = 0.55f), ObsidianBackground)))
                    .statusBarsPadding()
                    .padding(bottom = 10.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 6.dp)) {
                        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                        Column {
                            Text(target.label, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            Text(
                                if (target.kind == "country") "Movies & series from ${target.label}" else "Movies & series in ${target.label}",
                                color = TextSecondary, fontSize = 12.5.sp
                            )
                        }
                    }
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Movies", "Series").forEach { t ->
                            val sel = t == type
                            Text(
                                t,
                                color = if (sel) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (sel) CinetorRed else Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, if (sel) CinetorRed else Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable { type = t }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            if (items.isEmpty() && loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CinetorRed, strokeWidth = 2.5.dp)
                }
            } else if (items.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Nothing found here yet", color = TextMuted)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items.distinctBy { it.source + it.id + it.title }, key = { it.source + it.id + it.title }) { item ->
                        PosterCard(item = item, onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth())
                    }
                    item(span = { GridItemSpan(3) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            when {
                                loading -> CircularProgressIndicator(color = CinetorRed, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                !done -> Text(
                                    "Load more",
                                    color = Color.White, fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(CinetorRed)
                                        .clickable { page++ }
                                        .padding(horizontal = 22.dp, vertical = 10.dp)
                                )
                                else -> Text("That's everything", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
