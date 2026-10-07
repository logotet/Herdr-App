package io.github.vladimirvasilev.herdrapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import herdrapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus

@Composable
internal fun StatusBadge(status: AgentStatus) {
    Box(Modifier.statusShape(status).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(
            stringResource(status.label()),
            style = MaterialTheme.typography.labelSmall,
            color = if (status == AgentStatus.UNKNOWN) HerdrTheme.colors.muted else HerdrTheme.colors.onStatus,
        )
    }
}

/** The status as a coloured dot, for places too narrow for [StatusBadge]. */
@Composable
internal fun StatusDot(status: AgentStatus) {
    val label = stringResource(status.label())
    Box(
        Modifier
            .size(10.dp)
            .statusShape(status)
            .semantics { contentDescription = label },
    )
}

/** Filled with the status colour, pulsing while working; an outline when the status is unknown. */
@Composable
private fun Modifier.statusShape(status: AgentStatus): Modifier {
    val color = when (status) {
        AgentStatus.BLOCKED -> HerdrTheme.colors.statusBlocked
        AgentStatus.WORKING -> HerdrTheme.colors.statusWorking
        AgentStatus.DONE -> HerdrTheme.colors.statusDone
        AgentStatus.IDLE -> HerdrTheme.colors.statusIdle
        AgentStatus.UNKNOWN -> Color.Transparent
    }
    val transition = rememberInfiniteTransition(label = "working")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    val shape = RoundedCornerShape(50)
    return alpha(if (status == AgentStatus.WORKING) alpha else 1f)
        .then(
            if (status == AgentStatus.UNKNOWN) {
                Modifier.border(1.dp, HerdrTheme.colors.muted, shape)
            } else {
                Modifier.background(color, shape)
            },
        )
}

private fun AgentStatus.label(): StringResource = when (this) {
    AgentStatus.IDLE -> Res.string.status_idle
    AgentStatus.WORKING -> Res.string.status_working
    AgentStatus.BLOCKED -> Res.string.status_blocked
    AgentStatus.DONE -> Res.string.status_done
    AgentStatus.UNKNOWN -> Res.string.status_unknown
}
