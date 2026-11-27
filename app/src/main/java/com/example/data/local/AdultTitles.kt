package com.example.data.local

import com.example.data.model.MediaItem
import java.util.concurrent.ConcurrentHashMap

/** Titles shown in the 18+ Zone during this app session (kept out of history). */
object AdultTitles {
    private val keys = ConcurrentHashMap.newKeySet<String>()
    fun addAll(items: List<MediaItem>) { items.forEach { keys.add(it.id) } }
    fun contains(item: MediaItem): Boolean = item.id.isNotBlank() && item.id in keys
}
