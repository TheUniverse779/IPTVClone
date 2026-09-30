package com.iptvplayer.app.data.parser

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.Reader

/**
 * JSON playlist: either `[ {name,url,logo,group}, … ]` or `{ "channels": [ … ] }`.
 * The original app advertised JSON support but never implemented it.
 */
object JsonPlaylistParser {
    fun looksLikeJson(firstNonBlankChar: Char?) = firstNonBlankChar == '[' || firstNonBlankChar == '{'

    fun parse(reader: Reader, onChannel: (ParsedChannel) -> Unit) {
        val root = JsonParser.parseReader(reader)
        val arr: JsonArray = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> root.asJsonObject.let { o ->
                listOf("channels", "items", "streams").firstNotNullOfOrNull { k -> o.get(k)?.takeIf { it.isJsonArray }?.asJsonArray }
            } ?: JsonArray()
            else -> JsonArray()
        }
        arr.forEach { el ->
            if (!el.isJsonObject) return@forEach
            val o = el.asJsonObject
            val url = o.str("url", "link", "stream", "src") ?: return@forEach
            onChannel(
                ParsedChannel(
                    name = o.str("name", "title") ?: url.substringAfterLast('/'),
                    url = url,
                    logo = o.str("logo", "icon", "tvg-logo").orEmpty(),
                    group = o.str("group", "category", "group-title") ?: ParsedChannel.DEFAULT_GROUP,
                    tvgId = o.str("tvg-id", "id"),
                    userAgent = o.str("user-agent", "userAgent"),
                    referrer = o.str("referer", "referrer"),
                )
            )
        }
    }

    private fun JsonObject.str(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { k -> get(k)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() } }
}
