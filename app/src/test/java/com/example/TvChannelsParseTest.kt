package com.example

import com.example.data.remote.TvChannels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvChannelsParseTest {
    private val sample = """
#EXTM3U
#EXTINF:-1 tvg-id="ATNBangla.bd@SD",ATN Bangla (576p)
https://example.com/atn.m3u8
#EXTINF:-1 tvg-id="Aastha.in@HD",Aastha (720p)
https://aasthaott.akamaized.net/a.m3u8
#EXTINF:-1 tvg-id="Aastha.in@SD",Aastha SD (576p)
#EXTVLCOPT:http-user-agent=#EXTVLCOPT:http-user-agent=Mozilla/5.0 Chrome/147
http://103.175.73.12:8080/live/338/master.m3u8
#EXTINF:-1 tvg-id="ETNow.in@SD",ET Now (720p) [Geo-blocked]
#EXTVLCOPT:http-referrer=https://www.timesnownews.com/
https://dztlhgid9me95.cloudfront.net/etnow_master.m3u8
#EXTINF:-1 tvg-id="ColorsKannada.in@HD",Colors Kannada HD (1080i)
https://da86m1sqpm3o0.cloudfront.net/c.m3u8
#EXTINF:-1 tvg-id="ABPAnanda.in@SD",ABP Ananda (1080p)
https://d2l4ar6y3mrs4k.cloudfront.net/ananda.m3u8
#EXTINF:-1 tvg-id="ABPAnanda.in@SD",ABP Ananda (576p)
https://mumt05.tangotv.in/ananda/index.m3u8
""".trimIndent()

    @Test fun parsesIndiaPlaylist() {
        val list = TvChannels.parse(sample)
        val names = list.map { it.name }
        println(list.joinToString("\n") { "${it.name} | ${it.group} | ${it.tags} | ${it.tvgId} | ${it.sources}" })
        assertEquals("ATN Bangla", names.first())
        assertTrue("Bangladesh" in list.first().tags)
        val abp = list.first { it.name == "ABP Ananda" }
        assertEquals(2, abp.sources.size)
        assertEquals("1080p", abp.sources.first().quality)
        assertTrue("Bangla" in abp.tags && "News" in abp.tags && "India" in abp.tags)
        val sd = list.first { it.name == "Aastha SD" }
        assertEquals("Mozilla/5.0 Chrome/147", sd.sources.first().userAgent)
        assertEquals("Devotional", sd.group)
        assertEquals("https://www.timesnownews.com/", list.first { it.name == "ET Now" }.sources.first().referer)
        assertEquals("1080p", list.first { it.name == "Colors Kannada HD" }.sources.first().quality)
        assertEquals("Aastha.in", sd.tvgId)
    }
}
