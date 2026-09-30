package com.iptvplayer.app.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {

    private fun parse(text: String): Pair<PlaylistHeader, List<ParsedChannel>> {
        val out = mutableListOf<ParsedChannel>()
        val header = M3uParser.parse(text.reader().buffered()) { out += it }
        return header to out
    }

    @Test fun `basic iptv-org entry`() {
        val (header, list) = parse(
            """
            #EXTM3U x-tvg-url="https://epg.example/guide.xml"
            #EXTINF:-1 tvg-id="AnGiangTV1.vn@SD" tvg-logo="https://i.imgur.com/a.png" group-title="General",An Giang TV 1 (1080p) [Geo-blocked]
            https://tv.angiangtv.vn/live/kgtv/kgtv.m3u8
            """.trimIndent()
        )
        assertEquals("https://epg.example/guide.xml", header.epgUrl)
        assertEquals(1, list.size)
        with(list[0]) {
            assertEquals("An Giang TV 1 (1080p) [Geo-blocked]", name)
            assertEquals("AnGiangTV1.vn@SD", tvgId)
            assertEquals("https://i.imgur.com/a.png", logo)
            assertEquals("General", group)
            assertEquals("https://tv.angiangtv.vn/live/kgtv/kgtv.m3u8", url)
        }
    }

    @Test fun `comma inside attribute does not break title`() {
        val (_, list) = parse(
            """
            #EXTM3U
            #EXTINF:-1 group-title="News, World" tvg-name="BBC",BBC World News
            http://x/bbc.m3u8
            """.trimIndent()
        )
        assertEquals("BBC World News", list[0].name)
        assertEquals("News, World", list[0].group)
    }

    @Test fun `empty tvg-name falls back to title and missing group is Unknown`() {
        val (_, list) = parse("#EXTM3U\n#EXTINF:-1 tvg-name=\"\",Title Here\nhttp://x/a.ts")
        assertEquals("Title Here", list[0].name)
        assertEquals(ParsedChannel.DEFAULT_GROUP, list[0].group)
    }

    @Test fun `vlcopt, kodiprop and pipe headers`() {
        val (_, list) = parse(
            """
            #EXTM3U
            #EXTINF:-1 group-title="Sports",Sport 1
            #EXTVLCOPT:http-user-agent=MyUA/1.0
            #EXTVLCOPT:http-referrer=https://ref.example/
            #KODIPROP:inputstream.adaptive.license_type=clearkey
            #KODIPROP:inputstream.adaptive.license_key=abc:def
            https://cdn.example/s1.mpd
            #EXTINF:-1,Sport 2
            https://cdn.example/s2.m3u8|User-Agent=Other%20UA&Referer=https://r2.example/&Origin=https://o.example
            """.trimIndent()
        )
        with(list[0]) {
            assertEquals("MyUA/1.0", userAgent); assertEquals("https://ref.example/", referrer)
            assertEquals("clearkey", drmType); assertEquals("abc:def", drmKey)
        }
        with(list[1]) {
            assertEquals("https://cdn.example/s2.m3u8", url)
            assertEquals("Other UA", userAgent); assertEquals("https://r2.example/", referrer)
            assertEquals("https://o.example", headers["Origin"])
            assertNull(drmType)
        }
    }

    @Test fun `extgrp and orphan url`() {
        val (_, list) = parse("#EXTM3U\n#EXTINF:-1,A\n#EXTGRP:Kids\nhttp://x/a.m3u8\nhttp://x/path/orphan.m3u8")
        assertEquals("Kids", list[0].group)
        assertEquals("orphan", list[1].name)
        assertEquals(ParsedChannel.DEFAULT_GROUP, list[1].group)
    }

    @Test fun `json playlist`() {
        val out = mutableListOf<ParsedChannel>()
        JsonPlaylistParser.parse("""{"channels":[{"name":"A","url":"http://x/a.m3u8","group":"G","logo":"l"}]}""".reader()) { out += it }
        assertEquals(1, out.size); assertEquals("G", out[0].group); assertEquals("l", out[0].logo)
    }
}
