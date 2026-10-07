package io.github.vladimirvasilev.herdrapp.domain

import kotlinx.serialization.Serializable

@Serializable
data class SavedHost(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 8787,
    val token: String
)

interface HostRepository {
    val hosts: kotlinx.coroutines.flow.StateFlow<List<SavedHost>>
    suspend fun upsert(host: SavedHost)
    suspend fun delete(id: String)
}
