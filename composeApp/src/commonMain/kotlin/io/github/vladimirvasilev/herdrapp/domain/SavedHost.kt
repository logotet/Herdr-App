package io.github.vladimirvasilev.herdrapp.domain

import kotlinx.coroutines.flow.StateFlow

data class SavedHost(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 8787,
    val token: String,
) {
    companion object {
        /** A bridge is identified by where it listens, so pairing the same one again replaces it. */
        fun idFor(host: String, port: Int): String = "$host:$port"
    }
}

/** This list with [host] added, replacing any entry for the same id or the same address. */
fun List<SavedHost>.withHost(host: SavedHost): List<SavedHost> =
    (filterNot { it.id == host.id || (it.host == host.host && it.port == host.port) } + host).sortedBy { it.name }

interface HostRepository {
    val hosts: StateFlow<List<SavedHost>>
    suspend fun upsert(host: SavedHost)
    suspend fun delete(id: String)
}
