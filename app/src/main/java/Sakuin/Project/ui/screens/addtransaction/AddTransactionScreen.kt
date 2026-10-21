package Sakuin.Project.ui.screens.addtransaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import Sakuin.Project.R
import Sakuin.Project.ui.components.PlaceholderScreen
import Sakuin.Project.ui.theme.SakuinTheme

@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.title_add_transaction)
    PlaceholderScreen(
        title = title,
        modifier = modifier,
        header = {
            Row(
                // Pull the 48dp touch target back so the chevron lines up with the content edge.
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = (-12).dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = SakuinTheme.colors.textPrimary,
                    )
                }
                Text(
                    text = title,
                    style = SakuinTheme.typography.titleMedium,
                    color = SakuinTheme.colors.textPrimary,
                )
            }
        },
    )
}
