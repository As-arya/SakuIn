package Sakuin.Project.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import Sakuin.Project.R
import Sakuin.Project.ui.theme.SakuinTheme

/** Temporary body for screens whose real content lands in later phases. */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {
        Text(
            text = title,
            style = SakuinTheme.typography.headlineLarge,
            color = SakuinTheme.colors.textPrimary,
        )
    },
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg),
    ) {
        header()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.placeholder_coming_soon),
                style = SakuinTheme.typography.bodyMedium,
                color = SakuinTheme.colors.textSecondary,
            )
        }
    }
}
