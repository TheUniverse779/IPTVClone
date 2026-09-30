package com.iptvplayer.app.data.parser

import java.io.BufferedReader

/** One playable entry parsed from an M3U/M3U8 playlist. */
data class ParsedChannel(
    val name: String,
    val url: String,
    val logo: String = "",
    val group: String = DEFAULT_GROUP,
    val tvgId: String? = null,
    val tvgChno: String? = null,
    val userAgent: String? = null,
    val referrer: String? = null,
    /** Extra HTTP headers (Origin, Cookie, …) from `url|Header=value` or `#EXTVLCOPT`. */
    val headers: Map<String, String> = emptyMap(),
    /** `license_type` / `license_key` from `#KODIPROP:inputstream.adaptive.*`. */
    val drmType: String? = null,
    val drmKey: String? = null,
    val catchup: String? = null,
) {
    companion object { const val DEFAULT_GROUP = "Unknown" }
}

data class PlaylistHeader(val epgUrl: String? = null, val userAgent: String? = null)

/**
 * Streaming M3U parser.
 *
 * Fixes over the original app's four-regex parser:
 *  - generic `key="value"` attribute parsing (tvg-id, tvg-chno, catchup, …)
 *  - title is the text after the first comma that is *outside* quotes, so commas in attributes are safe
 *  - `tvg-name=""` no longer yields an empty name (falls back to the title)
 *  - `#EXTGRP`, `#EXTVLCOPT:http-user-agent/http-referrer/http-origin`, `#KODIPROP` license props
 *  - Kodi style `url|User-Agent=…&Referer=…`
 *  - a URL line without a preceding #EXTINF gets its own name instead of reusing the previous one
 */
object M3uParser {

    private val URL_PREFIXES = listOf("http://", "https://", "rtmp://", "rtmps://", "rtsp://", "rtp://", "udp://", "mms://", "mmsh://", "ftp://", "file://", "content://")

    fun isStreamUrl(line: String): Boolean {
        val l = line.trim().lowercase()
        return URL_PREFIXES.any { l.startsWith(it) } || l.endsWith(".m3u8") || l.endsWith(".ts")
    }

    /** Convenience wrapper: parses everything, calling [onChannel] per entry. Returns the header. */
    fun parse(reader: BufferedReader, onChannel: (ParsedChannel) -> Unit): PlaylistHeader {
        val s = Stream(reader)
        while (true) onChannel(s.next() ?: break)
        return s.header
    }

    /**
     * Pull-based reader: call [next] until it returns null. Lets the caller do suspend work
     * (batch DB inserts, progress, cancellation) between entries without buffering the playlist.
     */
    class Stream(private val reader: BufferedReader) {
        var header = PlaylistHeader(); private set
        private var pending: Pending? = null
        private var sawExtm3u = false
        private var index = 0

        fun next(): ParsedChannel? {
            while (true) {
                val raw = reader.readLine() ?: return null
                val line = raw.trim().removePrefix("﻿")
                if (line.isEmpty()) continue
                when {
                    line.startsWith("#EXTM3U", ignoreCase = true) -> {
                        sawExtm3u = true
                        val a = parseAttributes(line.substringAfter("#EXTM3U", ""))
                        header = PlaylistHeader(
                            epgUrl = a["url-tvg"] ?: a["x-tvg-url"] ?: a["tvg-url"],
                            userAgent = a["user-agent"] ?: a["http-user-agent"],
                        )
                    }
                    line.startsWith("#EXTINF", ignoreCase = true) -> pending = parseExtinf(line)
                    line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                        val g = line.substringAfter(':').trim()
                        if (g.isNotEmpty()) pending = (pending ?: Pending()).also { if (it.group == null) it.group = g }
                    }
                    line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                        val p = (pending ?: Pending()).also { pending = it }
                        val opt = line.substringAfter(':')
                        val value = opt.substringAfter('=', "").trim()
                        when (opt.substringBefore('=').trim().lowercase()) {
                            "http-user-agent" -> p.userAgent = value
                            "http-referrer", "http-referer" -> p.referrer = value
                            "http-origin" -> p.headers["Origin"] = value
                        }
                    }
                    line.startsWith("#KODIPROP:", ignoreCase = true) -> {
                        val p = (pending ?: Pending()).also { pending = it }
                        val prop = line.substringAfter(':')
                        val value = prop.substringAfter('=', "").trim()
                        when (prop.substringBefore('=').trim().lowercase()) {
                            "inputstream.adaptive.license_type" -> p.drmType = value
                            "inputstream.adaptive.license_key" -> p.drmKey = value
                            "inputstream.adaptive.stream_headers" -> p.headers.putAll(parseHeaderString(value))
                        }
                    }
                    line.startsWith("#") -> Unit
                    isStreamUrl(line) || (sawExtm3u && pending != null) -> {
                        val p = pending ?: Pending()
                        pending = null
                        val (url, urlHeaders) = splitKodiUrl(line)
                        val headers = LinkedHashMap(p.headers).apply { putAll(urlHeaders) }
                        val ua = headers.remove("User-Agent") ?: p.userAgent ?: header.userAgent
                        val ref = headers.remove("Referer") ?: p.referrer
                        index++
                        return ParsedChannel(
                            name = p.name?.takeIf { it.isNotBlank() } ?: fallbackName(url, index),
                            url = url,
                            logo = p.logo.orEmpty(),
                            group = p.group?.takeIf { it.isNotBlank() } ?: ParsedChannel.DEFAULT_GROUP,
                            tvgId = p.tvgId, tvgChno = p.tvgChno, userAgent = ua, referrer = ref,
                            headers = headers, drmType = p.drmType, drmKey = p.drmKey, catchup = p.catchup,
                        )
                    }
                }
            }
        }
    }

    private class Pending(
        var name: String? = null, var logo: String? = null, var group: String? = null,
        var tvgId: String? = null, var tvgChno: String? = null, var catchup: String? = null,
        var userAgent: String? = null, var referrer: String? = null,
        val headers: MutableMap<String, String> = LinkedHashMap(),
        var drmType: String? = null, var drmKey: String? = null,
    )

    private fun parseExtinf(line: String): Pending {
        val body = line.substringAfter(':', "")
        val comma = firstCommaOutsideQuotes(body)
        val attrPart = if (comma >= 0) body.substring(0, comma) else body
        val title = if (comma >= 0) body.substring(comma + 1).trim() else ""
        val a = parseAttributes(attrPart)
        return Pending(
            name = title.ifBlank { a["tvg-name"] },
            logo = a["tvg-logo"] ?: a["logo"],
            group = a["group-title"],
            tvgId = a["tvg-id"],
            tvgChno = a["tvg-chno"],
            catchup = a["catchup"] ?: a["catchup-type"],
            userAgent = a["user-agent"] ?: a["http-user-agent"],
            referrer = a["http-referrer"] ?: a["referrer"],
        )
    }

    internal fun firstCommaOutsideQuotes(s: String): Int {
        var inQuotes = false
        s.forEachIndexed { i, c ->
            if (c == '"') inQuotes = !inQuotes
            else if (c == ',' && !inQuotes) return i
        }
        return -1
    }

    private val ATTR = Regex("""([A-Za-z0-9_\-]+)\s*=\s*"([^"]*)"""")

    internal fun parseAttributes(s: String): Map<String, String> =
        ATTR.findAll(s).associate { it.groupValues[1].lowercase() to it.groupValues[2].trim() }

    /** `http://a/b.m3u8|User-Agent=X&Referer=Y` → url + headers. */
    internal fun splitKodiUrl(line: String): Pair<String, Map<String, String>> {
        val bar = line.indexOf('|')
        if (bar < 0) return line to emptyMap()
        return line.substring(0, bar).trim() to parseHeaderString(line.substring(bar + 1))
    }

    private fun parseHeaderString(s: String): Map<String, String> =
        s.split('&').mapNotNull { kv ->
            val k = kv.substringBefore('=', "").trim()
            val v = kv.substringAfter('=', "").trim()
            if (k.isEmpty()) null else normalizeHeader(k) to java.net.URLDecoder.decode(v, "UTF-8")
        }.toMap()

    private fun normalizeHeader(k: String) = when (k.lowercase()) {
        "user-agent" -> "User-Agent"
        "referer", "referrer" -> "Referer"
        "origin" -> "Origin"
        "cookie" -> "Cookie"
        else -> k
    }

    private fun fallbackName(url: String, index: Int): String {
        val last = url.substringBefore('?').trimEnd('/').substringAfterLast('/')
        return last.substringBeforeLast('.').ifBlank { "Channel $index" }
    }
}
