package io.github.vladimirvasilev.herdrapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import herdrapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.domain.ConnectionState

@Composable
internal fun ConnectionBanner(state: ConnectionState, hostName: String?, onHosts: () -> Unit) {
    val text = when (state) {
        ConnectionState.Disconnected -> stringResource(Res.string.connection_disconnected)
        ConnectionState.Connecting ->
            stringResource(Res.string.connection_connecting, hostName ?: stringResource(Res.string.connection_connecting_unknown_host))
        is ConnectionState.Connected -> stringResource(Res.string.connection_connected, state.bridgeName)
        is ConnectionState.Error -> stringResource(Res.string.connection_error, state.message)
    }
    val color = when (state) {
        is ConnectionState.Connected -> HerdrTheme.colors.bannerConnected
        is ConnectionState.Error -> HerdrTheme.colors.bannerError
        else -> HerdrTheme.colors.bannerNeutral
    }
    Row(
        Modifier.fillMaxWidth().background(color).clickable { onHosts() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(Res.string.connection_hosts), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun HerdrUnavailableBanner(herdrAvailable: Boolean) {
    if (herdrAvailable) return
    Row(
        Modifier.fillMaxWidth().background(HerdrTheme.colors.bannerWarning).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.herdr_unavailable),
            fontWeight = FontWeight.Bold,
            color = HerdrTheme.colors.onBannerWarning,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(Res.string.herdr_unavailable_bridge_ok),
            style = MaterialTheme.typography.labelSmall,
            color = HerdrTheme.colors.onBannerWarning,
        )
    }
}
