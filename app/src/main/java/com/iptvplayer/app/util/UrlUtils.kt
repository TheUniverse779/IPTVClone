package com.iptvplayer.app.util

import android.net.Uri

object UrlUtils {
    /** Adds http:// when missing and strips trailing slashes/whitespace (avoids the `//` bug in the original). */
    fun normalizeServer(input: String): String {
        var s = input.trim().trimEnd('/')
        if (!s.contains("://")) s = "http://$s"
        return s
    }

    data class XtreamCreds(val server: String, val username: String, val password: String)

    /**
     * Extracts Xtream credentials from text the user pasted:
     *  - `http://host:port/get.php?username=U&password=P&type=m3u_plus`
     *  - `http://host:port/player_api.php?username=U&password=P`
     *  - `Server URL: "…" ,User Name: "…" ,Password: "…"` (the format Community copies)
     *  - `http://host:port/live/U/P/123.ts`
     */
    fun parseXtream(text: String): XtreamCreds? {
        val t = text.trim()
        Regex("""(https?://[^\s"?]+?)/(?:get|player_api)\.php\?[^\s"]*""", RegexOption.IGNORE_CASE).find(t)?.let { m ->
            val uri = Uri.parse(m.value)
            val u = uri.getQueryParameter("username"); val p = uri.getQueryParameter("password")
            if (!u.isNullOrBlank() && !p.isNullOrBlank()) return XtreamCreds(normalizeServer(m.groupValues[1]), u, p)
        }
        val server = Regex("""server\s*url\s*:?\s*"?([^\s",]+)""", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        val user = Regex("""user\s*name\s*:?\s*"?([^\s",]+)""", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        val pass = Regex("""password\s*:?\s*"?([^\s",]+)""", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        if (server != null && user != null && pass != null) return XtreamCreds(normalizeServer(server), user, pass)
        Regex("""(https?://[^/\s]+)/(?:live|movie|series)/([^/\s]+)/([^/\s]+)/\d+""", RegexOption.IGNORE_CASE).find(t)?.let {
            return XtreamCreds(it.groupValues[1], it.groupValues[2], it.groupValues[3])
        }
        return null
    }

    fun firstUrl(text: String): String? = Regex("""(https?|rtmp|rtsp|udp)://\S+""", RegexOption.IGNORE_CASE).find(text)?.value

    fun host(url: String): String = runCatching { Uri.parse(url).host ?: url }.getOrDefault(url)

    fun isPlaylistLink(url: String): Boolean {
        val l = url.lowercase().substringBefore('#')
        return l.contains(".m3u") || l.contains("get.php?") || l.contains("type=m3u")
    }
}
