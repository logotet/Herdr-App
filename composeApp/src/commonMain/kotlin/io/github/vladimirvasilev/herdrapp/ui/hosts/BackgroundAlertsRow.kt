package io.github.vladimirvasilev.herdrapp.ui.hosts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import herdrapp.composeapp.generated.resources.Res
import herdrapp.composeapp.generated.resources.hosts_alerts_body
import herdrapp.composeapp.generated.resources.hosts_alerts_denied
import herdrapp.composeapp.generated.resources.hosts_alerts_title
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import org.jetbrains.compose.resources.stringResource

/** The switch for staying connected in the background and notifying about agents. */
@Composable
internal fun BackgroundAlertsRow(enabled: Boolean, denied: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.hosts_alerts_title), fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.hosts_alerts_body), color = HerdrTheme.colors.muted)
            if (denied) {
                Text(stringResource(Res.string.hosts_alerts_denied), color = MaterialTheme.colorScheme.error)
            }
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}
