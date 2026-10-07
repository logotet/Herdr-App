package io.github.vladimirvasilev.herdrapp.domain

import kotlinx.coroutines.flow.StateFlow

data class SavedHost(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 8787,
    val token: String,
)

interface HostRepository {
    val hosts: StateFlow<List<SavedHost>>
    suspend fun upsert(host: SavedHost)
    suspend fun delete(id: String)
}
