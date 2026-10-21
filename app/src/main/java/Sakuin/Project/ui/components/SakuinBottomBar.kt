package Sakuin.Project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import Sakuin.Project.R
import Sakuin.Project.ui.navigation.TopLevelDestination
import Sakuin.Project.ui.theme.SakuinTheme

/**
 * Icon-only bottom navigation from the Figma `BottomNav` component:
 * four tabs with the add button in the middle. The active tab is tinted
 * with the primary color and gets a dot underneath.
 */
@Composable
fun SakuinBottomBar(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SakuinTheme.colors
    val tabs = TopLevelDestination.entries
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.take(2).forEach { tab ->
                NavItem(tab = tab, selected = tab == selected, onClick = { onSelect(tab) })
            }
            AddButton(onClick = onAddClick)
            tabs.drop(2).forEach { tab ->
                NavItem(tab = tab, selected = tab == selected, onClick = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = SakuinTheme.colors
    Column(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(tab.icon),
            contentDescription = stringResource(tab.label),
            tint = if (selected) colors.textBrand else colors.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(if (selected) colors.primary else Color.Transparent, CircleShape),
        )
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val colors = SakuinTheme.colors
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.primary)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = stringResource(R.string.nav_add_transaction),
            tint = colors.onPrimary,
        )
    }
}

@Preview
@Composable
private fun SakuinBottomBarPreview() {
    SakuinTheme {
        SakuinBottomBar(selected = TopLevelDestination.Home, onSelect = {}, onAddClick = {})
    }
}
