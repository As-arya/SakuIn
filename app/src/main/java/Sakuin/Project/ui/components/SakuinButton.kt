package Sakuin.Project.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import Sakuin.Project.ui.theme.SakuinTheme

/** Variants of the Figma `Button` component. */
enum class ButtonStyle { Primary, Secondary, Outline, Danger }

@Composable
fun SakuinButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    val colors = SakuinTheme.colors
    val (background, content) = when (style) {
        ButtonStyle.Primary -> colors.primary to colors.onPrimary
        ButtonStyle.Secondary -> colors.bgSurfaceVariant to colors.textPrimary
        ButtonStyle.Outline -> colors.bgSurface to colors.textPrimary
        ButtonStyle.Danger -> colors.expense to colors.onPrimary
    }
    val shape = RoundedCornerShape(SakuinTheme.radius.lg)
    Row(
        modifier = modifier
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(background)
            .border(1.dp, if (style == ButtonStyle.Outline) colors.border else Color.Transparent, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = SakuinTheme.spacing.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp), tint = content)
            Spacer(Modifier.width(SakuinTheme.spacing.sm))
        }
        Text(text = text, style = SakuinTheme.typography.bodyStrong, color = content)
    }
}
