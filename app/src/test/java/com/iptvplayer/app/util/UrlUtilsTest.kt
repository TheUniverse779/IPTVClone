package com.iptvplayer.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests (UrlUtils.parseXtream uses android.net.Uri for get.php, covered on-device). */
class UrlUtilsTest {
    @Test fun `normalize server adds scheme and strips trailing slash`() {
        assertEquals("http://host:8080", UrlUtils.normalizeServer(" host:8080/ "))
        assertEquals("https://a.b", UrlUtils.normalizeServer("https://a.b//"))
    }

    @Test fun `community copy format is parsed`() {
        val c = UrlUtils.parseXtream("""Server URL: "http://line.tv:8080" ,User Name: "demo" ,Password: "x7Kp2"""")!!
        assertEquals("http://line.tv:8080", c.server); assertEquals("demo", c.username); assertEquals("x7Kp2", c.password)
    }

    @Test fun `live stream url is parsed`() {
        val c = UrlUtils.parseXtream("http://line.tv:8080/live/u1/p1/123.ts")!!
        assertEquals("http://line.tv:8080", c.server); assertEquals("u1", c.username); assertEquals("p1", c.password)
    }

    @Test fun `playlist link detection`() {
        assertTrue(UrlUtils.isPlaylistLink("https://x/a.m3u"))
        assertTrue(UrlUtils.isPlaylistLink("https://x/a.m3u8?t=1"))
        assertTrue(UrlUtils.isPlaylistLink("http://h/get.php?username=a&password=b&type=m3u_plus"))
        assertFalse(UrlUtils.isPlaylistLink("https://github.com/iptv-org/iptv"))
    }
}
