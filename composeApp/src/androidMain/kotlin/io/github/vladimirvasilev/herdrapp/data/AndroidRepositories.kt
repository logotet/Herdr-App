package io.github.vladimirvasilev.herdrapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.vladimirvasilev.herdrapp.domain.HomeView
import io.github.vladimirvasilev.herdrapp.domain.HostRepository
import io.github.vladimirvasilev.herdrapp.domain.SavedHost
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.domain.withHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "herdr_app")
private val HOSTS = stringPreferencesKey("hosts_json")
private val FONT_SIZE = floatPreferencesKey("terminal_font_size")
// Stored as the enum's name; do not rename the constants of HomeView.
private val HOME_VIEW = stringPreferencesKey("home_view")
private val BACKGROUND_ALERTS = booleanPreferencesKey("background_alerts")

/** The stored form of a host. Field names are the on-disk format; do not rename them. */
@Serializable
private data class StoredHost(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 8787,
    val token: String,
) {
    fun toDomain() = SavedHost(id = id, name = name, host = host, port = port, token = token)
}

private fun SavedHost.toStored() = StoredHost(id = id, name = name, host = host, port = port, token = token)

private val storageJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
private val storedHosts = ListSerializer(StoredHost.serializer())

private fun decodeHosts(raw: String): List<SavedHost> {
    if (raw.isBlank()) return emptyList()
    return runCatching { storageJson.decodeFromString(storedHosts, raw) }.getOrDefault(emptyList()).map { it.toDomain() }
}

private fun encodeHosts(hosts: List<SavedHost>): String = storageJson.encodeToString(storedHosts, hosts.map { it.toStored() })

class AndroidHostRepository(context: Context, scope: CoroutineScope) : HostRepository {
    private val appContext = context.applicationContext
    override val hosts: StateFlow<List<SavedHost>> = appContext.dataStore.data
        .map { prefs -> decodeHosts(prefs[HOSTS].orEmpty()) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun upsert(host: SavedHost) {
        // Read inside the transaction: the cached list in [hosts] can lag behind a write in flight.
        appContext.dataStore.edit { prefs ->
            prefs[HOSTS] = encodeHosts(decodeHosts(prefs[HOSTS].orEmpty()).withHost(host))
        }
    }

    override suspend fun delete(id: String) {
        appContext.dataStore.edit { prefs ->
            prefs[HOSTS] = encodeHosts(decodeHosts(prefs[HOSTS].orEmpty()).filterNot { it.id == id })
        }
    }
}

class AndroidSettingsRepository(context: Context, scope: CoroutineScope) : SettingsRepository {
    private val appContext = context.applicationContext
    override val terminalFontSize: StateFlow<Float> = appContext.dataStore.data
        .map { it[FONT_SIZE] ?: 14f }
        .stateIn(scope, SharingStarted.Eagerly, 14f)

    override suspend fun setTerminalFontSize(sizeSp: Float) {
        appContext.dataStore.edit { it[FONT_SIZE] = sizeSp.coerceIn(8f, 28f) }
    }

    override val homeView: StateFlow<HomeView> = appContext.dataStore.data
        .map { prefs -> HomeView.entries.firstOrNull { it.name == prefs[HOME_VIEW] } ?: HomeView.WORKSPACES }
        .stateIn(scope, SharingStarted.Eagerly, HomeView.WORKSPACES)

    override suspend fun setHomeView(view: HomeView) {
        appContext.dataStore.edit { it[HOME_VIEW] = view.name }
    }

    override val backgroundAlerts: StateFlow<Boolean> = appContext.dataStore.data
        .map { it[BACKGROUND_ALERTS] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun setBackgroundAlerts(enabled: Boolean) {
        appContext.dataStore.edit { it[BACKGROUND_ALERTS] = enabled }
    }
}
