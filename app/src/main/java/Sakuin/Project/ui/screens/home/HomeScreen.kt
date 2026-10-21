package Sakuin.Project.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import Sakuin.Project.R
import Sakuin.Project.ui.components.PlaceholderScreen
import Sakuin.Project.ui.theme.SakuinTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userName: String = "there",
    onOpenSettings: () -> Unit = {},
) {
    val greeting = stringResource(R.string.greeting, userName)
    PlaceholderScreen(
        title = greeting,
        modifier = modifier,
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = greeting,
                    style = SakuinTheme.typography.titleMedium,
                    color = SakuinTheme.colors.textPrimary,
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SakuinTheme.colors.bgSurface)
                        .clickable(role = Role.Button, onClick = onOpenSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = SakuinTheme.colors.textPrimary,
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SakuinTheme { HomeScreen(userName = "Arya") }
}
