package io.github.vladimirvasilev.herdrapp.domain

object PairUriParser {
    private const val PREFIX = "herdr-bridge://pair?"
    private const val DEFAULT_PORT = 8787

    fun parse(uri: String): SavedHost? {
        val trimmed = uri.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        val values = trimmed.substringAfter('?').split('&')
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) null else decode(part.substring(0, idx)) to decode(part.substring(idx + 1))
            }.toMap()
        val host = values["host"]?.takeIf { it.isNotBlank() } ?: return null
        val token = values["token"]?.takeIf { it.isNotBlank() } ?: return null
        val port = values["port"]?.toIntOrNull() ?: DEFAULT_PORT
        val name = values["name"]?.takeIf { it.isNotBlank() } ?: host
        return SavedHost(id = SavedHost.idFor(host, port), name = name, host = host, port = port, token = token)
    }

    /** Decodes a query component: `+` is a space and `%XX` is one byte of UTF-8. */
    private fun decode(s: String): String {
        val out = StringBuilder()
        // Consecutive escapes are decoded together, since one character can span several of them.
        val pending = ArrayList<Byte>()
        fun flush() {
            if (pending.isEmpty()) return
            out.append(pending.toByteArray().decodeToString())
            pending.clear()
        }
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val escaped = if (c == '%' && i + 2 < s.length) s.substring(i + 1, i + 3).toIntOrNull(16) else null
            if (escaped != null) {
                pending += escaped.toByte()
                i += 3
                continue
            }
            flush()
            out.append(if (c == '+') ' ' else c)
            i++
        }
        flush()
        return out.toString()
    }
}
