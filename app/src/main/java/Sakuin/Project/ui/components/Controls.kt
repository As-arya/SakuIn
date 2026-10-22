package Sakuin.Project.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import Sakuin.Project.R
import Sakuin.Project.ui.theme.SakuinTheme

/** Marks a screen or section title so TalkBack users can jump between headings. */
fun Modifier.asHeading(): Modifier = semantics { heading() }

/** App bar for detail and form screens (Figma `TopBar`). */
@Composable
fun SakuinTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = SakuinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.action_back),
                    tint = SakuinTheme.colors.textPrimary,
                )
            }
        } else {
            Spacer(Modifier.width(SakuinTheme.spacing.md))
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .asHeading(),
            style = SakuinTheme.typography.titleMedium,
            color = SakuinTheme.colors.textPrimary,
            maxLines = 1,
        )
        actions()
    }
}

/** Round icon button used in screen headers (e.g. settings, add wallet). */
@Composable
fun CircleIconButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = SakuinTheme.colors.bgSurface,
    tint: Color = SakuinTheme.colors.textPrimary,
    size: Int = 40,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription, modifier = Modifier.size(22.dp), tint = tint)
    }
}

/** Choice chip (Figma `Chip`): selected = solid primary, otherwise outlined. */
@Composable
fun SakuinChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    val colors = SakuinTheme.colors
    val content = if (selected) colors.onPrimary else colors.textPrimary
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.bgSurface)
            .border(1.dp, if (selected) Color.Transparent else colors.border, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = SakuinTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(16.dp), tint = content)
        Text(label, style = SakuinTheme.typography.labelMedium, color = content)
    }
}

/** Expense / Income / Transfer switch (Figma `SegmentedControl`). */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SakuinTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SakuinTheme.radius.lg))
            .background(colors.bgSurfaceVariant)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(SakuinTheme.radius.md))
                    .background(if (selected) colors.bgSurface else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = if (selected) SakuinTheme.typography.bodyStrong else SakuinTheme.typography.bodyMedium,
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                )
            }
        }
    }
}

/** Page indicator for the onboarding slides: the active page is a wider primary pill. */
@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(width = if (index == current) 22.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) SakuinTheme.colors.primary else SakuinTheme.colors.border),
            )
        }
    }
}
