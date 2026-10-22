package Sakuin.Project.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import Sakuin.Project.ui.theme.SakuinTheme

/**
 * Shown where a list has nothing yet: an icon tile, a title and a short hint, in the style of the
 * scan screen's offline state. [tileColor] should contrast with the screen behind it.
 */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tileColor: Color = SakuinTheme.colors.bgSurface,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = SakuinTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SakuinTheme.spacing.xl, vertical = SakuinTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(SakuinTheme.radius.lg))
                .background(tileColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, Modifier.size(28.dp), tint = colors.textSecondary)
        }
        Spacer(Modifier.height(SakuinTheme.spacing.sm))
        Text(title, style = SakuinTheme.typography.titleSmall, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(body, style = SakuinTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(SakuinTheme.spacing.md))
            action()
        }
    }
}
