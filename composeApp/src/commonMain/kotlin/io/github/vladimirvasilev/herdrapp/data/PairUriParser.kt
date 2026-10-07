package io.github.vladimirvasilev.herdrapp.data

import io.github.vladimirvasilev.herdrapp.domain.SavedHost

import kotlin.random.Random

object PairUriParser {
    fun parse(uri: String): SavedHost? {
        val trimmed = uri.trim()
        if (!trimmed.startsWith("herdr-bridge://pair?")) return null
        val query = trimmed.substringAfter('?')
        val values = query.split('&')
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) null else decode(part.substring(0, idx)) to decode(part.substring(idx + 1))
            }.toMap()
        val host = values["host"]?.takeIf { it.isNotBlank() } ?: return null
        val token = values["token"]?.takeIf { it.isNotBlank() } ?: return null
        val port = values["port"]?.toIntOrNull() ?: 8787
        val name = values["name"]?.takeIf { it.isNotBlank() } ?: host
        return SavedHost(id = stableId(name, host, port), name = name, host = host, port = port, token = token)
    }

    private fun stableId(name: String, host: String, port: Int): String =
        (name + host + port).hashCode().toUInt().toString(16) + "-" + Random.nextInt(1000, 9999)

    private fun decode(s: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c == '+' -> out.append(' ')
                c == '%' && i + 2 < s.length -> {
                    val hex = s.substring(i + 1, i + 3).toIntOrNull(16)
                    if (hex != null) { out.append(hex.toChar()); i += 2 } else out.append(c)
                }
                else -> out.append(c)
            }
            i++
        }
        return out.toString()
    }
}
