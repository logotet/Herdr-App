package io.github.vladimirvasilev.herdrapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.domain.SavedHost
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.HerdrInfo
import io.github.vladimirvasilev.herdrapp.domain.ConnectionState

@Composable
internal fun ConnectionBanner(state: ConnectionState, host: SavedHost?, onHosts: () -> Unit) {
    val text = when (state) {
        ConnectionState.Disconnected -> "Disconnected"
        ConnectionState.Connecting -> "Connecting to ${host?.name ?: "host"}?"
        is ConnectionState.Connected -> "Connected to ${state.bridgeName}"
        is ConnectionState.Error -> "Connection error: ${state.message}"
    }
    val color = when (state) {
        is ConnectionState.Connected -> Color(0xFF23452C)
        is ConnectionState.Error -> Color(0xFF4A2027)
        else -> Color(0xFF303044)
    }
    Row(
        Modifier.fillMaxWidth().background(color).clickable { onHosts() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("Hosts", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun HerdrUnavailableBanner(status: HerdrInfo?) {
    if (status?.available != false) return
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF5A3418)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "herdr not running on PC",
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD8A8),
            modifier = Modifier.weight(1f),
        )
        Text("Bridge connected", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD8A8))
    }
}
