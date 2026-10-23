package Sakuin.Project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.model.TransactionWithDetails
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah

/**
 * Transaction row (Figma `TransactionRow`): neutral icon tile, title + subtitle, amount on the right.
 * Income is green with "+", expense red with "−", transfers stay neutral.
 */
@Composable
fun TransactionRow(
    item: TransactionWithDetails,
    trailingCaption: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = SakuinTheme.colors
    val t = item.transaction
    val transferTitle = stringResource(R.string.transaction_transfer)
    val title = when {
        t.title.isNotBlank() -> t.title
        t.type == TransactionType.TRANSFER -> t.note ?: transferTitle
        else -> t.note ?: item.categoryName.orEmpty()
    }
    val feeCaption = if (t.fee > 0) stringResource(R.string.transfer_fee_caption, formatRupiah(t.fee)) else null
    val subtitle = when (t.type) {
        TransactionType.TRANSFER -> listOfNotNull("${item.walletName} → ${item.toWalletName.orEmpty()}", feeCaption).joinToString(" · ")
        else -> listOfNotNull(item.categoryName, item.walletName).joinToString(" · ")
    }
    val (amountText, amountColor) = when (t.type) {
        TransactionType.INCOME -> "+ ${formatRupiah(t.amount)}" to colors.income
        TransactionType.EXPENSE -> "− ${formatRupiah(t.amount)}" to colors.expense
        TransactionType.TRANSFER -> formatRupiah(t.amount) to colors.textPrimary
    }
    val icon = if (t.type == TransactionType.TRANSFER) R.drawable.ic_transfer else categoryIcon(item.categoryIconKey)

    val shape = RoundedCornerShape(SakuinTheme.radius.lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.bgSurface)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(SakuinTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.bgSurfaceVariant, RoundedCornerShape(SakuinTheme.radius.md)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp), tint = colors.textPrimary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SakuinTheme.typography.bodyStrong, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = SakuinTheme.typography.caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(amountText, style = SakuinTheme.typography.bodyStrong, color = amountColor)
            Text(trailingCaption, style = SakuinTheme.typography.caption, color = colors.textSecondary)
        }
    }
}
