package com.example.ui.components

import com.example.data.model.StreamOption

/**
 * Fixed list of qualities shown everywhere (play + download). When the chosen quality
 * isn't available for a video, the closest available one is used.
 */
object QualityLadder {
    data class Step(val value: Int, val label: String, val badge: String)

    val steps = listOf(
        Step(2160, "2160p", "4K"),
        Step(1440, "1440p", "2K"),
        Step(1080, "1080p", "FHD"),
        Step(720, "720p", "HD"),
        Step(480, "480p", "SD"),
        Step(360, "360p", "SD")
    )

    /** Number like 1080 from labels such as "1080p", "FHD", "4K", "HD 720". */
    fun valueOf(label: String): Int {
        val l = label.lowercase()
        Regex("(\\d{3,4})\\s*p?").findAll(l).mapNotNull { it.groupValues[1].toIntOrNull() }
            .firstOrNull { it in 144..4320 }?.let { return it }
        return when {
            "8k" in l -> 4320
            "4k" in l || "uhd" in l -> 2160
            "2k" in l || "qhd" in l -> 1440
            "fhd" in l || "full hd" in l -> 1080
            "hd" in l -> 720
            "sd" in l -> 480
            else -> 0
        }
    }

    fun valueOf(option: StreamOption): Int = valueOf(option.quality.ifBlank { option.label })

    /** The step a stream belongs to (closest step). */
    fun stepFor(value: Int): Step? =
        if (value <= 0) null else steps.minByOrNull { kotlin.math.abs(it.value - value) }

    fun available(streams: List<StreamOption>): Set<Int> =
        streams.mapNotNull { stepFor(valueOf(it))?.value }.toSet()

    /**
     * Best stream for [target]: exact step if present, otherwise the highest quality below it,
     * otherwise the lowest quality above it.
     */
    fun pick(streams: List<StreamOption>, target: Int, preferAudio: String? = null): StreamOption? {
        if (streams.isEmpty()) return null
        val pool = preferAudio?.let { a -> streams.filter { it.audioLanguage == a }.ifEmpty { streams } } ?: streams
        val withValue = pool.map { it to valueOf(it) }
        val exact = withValue.firstOrNull { stepFor(it.second)?.value == target }
        if (exact != null) return exact.first
        val below = withValue.filter { it.second in 1 until target }.maxByOrNull { it.second }
        if (below != null) return below.first
        val above = withValue.filter { it.second > target }.minByOrNull { it.second }
        return (above ?: withValue.first()).first
    }

    /** Same idea for video tracks inside an HLS stream (labels like "720p"). */
    fun pickTrack(tracks: List<PlayerTrackOption>, target: Int): PlayerTrackOption? {
        if (tracks.isEmpty()) return null
        val withValue = tracks.map { it to valueOf(it.label) }
        withValue.firstOrNull { stepFor(it.second)?.value == target }?.let { return it.first }
        withValue.filter { it.second in 1 until target }.maxByOrNull { it.second }?.let { return it.first }
        return withValue.filter { it.second > target }.minByOrNull { it.second }?.first ?: tracks.first()
    }
}
