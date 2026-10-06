package io.github.vladimirvasilev.herdrapp.data

import kotlinx.serialization.Serializable

@Serializable
data class SavedHost(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 8787,
    val token: String
) {
    val wsUrl: String get() = "ws://$host:$port/ws"
}

interface HostRepository {
    val hosts: kotlinx.coroutines.flow.StateFlow<List<SavedHost>>
    suspend fun upsert(host: SavedHost)
    suspend fun delete(id: String)
}
