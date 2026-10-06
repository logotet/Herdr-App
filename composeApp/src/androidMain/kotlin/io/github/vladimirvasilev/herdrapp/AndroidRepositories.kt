package io.github.vladimirvasilev.herdrapp

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.vladimirvasilev.herdrapp.data.HostRepository
import io.github.vladimirvasilev.herdrapp.data.SavedHost
import io.github.vladimirvasilev.herdrapp.data.SettingsRepository
import io.github.vladimirvasilev.herdrapp.protocol.BridgeJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

private val Context.dataStore by preferencesDataStore(name = "herdr_app")
private val HOSTS = stringPreferencesKey("hosts_json")
private val FONT_SIZE = floatPreferencesKey("terminal_font_size")

class AndroidHostRepository(context: Context, scope: CoroutineScope) : HostRepository {
    private val appContext = context.applicationContext
    override val hosts: StateFlow<List<SavedHost>> = appContext.dataStore.data.map { prefs ->
        val raw = prefs[HOSTS].orEmpty()
        if (raw.isBlank()) emptyList() else runCatching { BridgeJson.json.decodeFromString(ListSerializer(SavedHost.serializer()), raw) }.getOrDefault(emptyList())
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun upsert(host: SavedHost) {
        appContext.dataStore.edit { prefs ->
            val next = (hosts.value.filterNot { it.id == host.id || (it.host == host.host && it.port == host.port) } + host).sortedBy { it.name }
            prefs[HOSTS] = BridgeJson.json.encodeToString(ListSerializer(SavedHost.serializer()), next)
        }
    }

    override suspend fun delete(id: String) {
        appContext.dataStore.edit { prefs -> prefs[HOSTS] = BridgeJson.json.encodeToString(ListSerializer(SavedHost.serializer()), hosts.value.filterNot { it.id == id }) }
    }
}

class AndroidSettingsRepository(context: Context, scope: CoroutineScope) : SettingsRepository {
    private val appContext = context.applicationContext
    override val terminalFontSize: StateFlow<Float> = appContext.dataStore.data.map { it[FONT_SIZE] ?: 14f }.stateIn(scope, SharingStarted.Eagerly, 14f)
    override suspend fun setTerminalFontSize(sizeSp: Float) { appContext.dataStore.edit { it[FONT_SIZE] = sizeSp.coerceIn(8f, 28f) } }
}
