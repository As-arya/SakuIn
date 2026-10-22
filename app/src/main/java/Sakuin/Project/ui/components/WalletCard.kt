package Sakuin.Project.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import Sakuin.Project.data.local.entity.WalletType
import Sakuin.Project.ui.theme.SakuinTheme

/** Solid wallet-colored card (Figma `WalletCard`): type label, name and balance. */
@Composable
fun WalletCard(
    type: WalletType,
    name: String,
    balance: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val content = type.contentColor
    Column(
        modifier = modifier
            .background(type.color, RoundedCornerShape(SakuinTheme.radius.xl))
            .padding(SakuinTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(SakuinTheme.radius.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(type.icon), null, Modifier.size(18.dp), tint = content)
            }
            Text(stringResource(type.label), style = SakuinTheme.typography.caption, color = content.copy(alpha = 0.85f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = caption ?: name,
                style = SakuinTheme.typography.labelMedium,
                color = content.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                balance,
                style = SakuinTheme.typography.headlineLarge,
                color = content,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = SakuinTheme.typography.headlineLarge.fontSize),
            )
        }
    }
}
