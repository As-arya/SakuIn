package Sakuin.Project.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.WalletType
import Sakuin.Project.ui.theme.SakuinTheme

/** Wallet identity colors: Cash red, Card yellow, E-wallet green. */
val WalletType.color: Color
    @Composable @ReadOnlyComposable get() = when (this) {
        WalletType.CASH -> SakuinTheme.colors.walletCash
        WalletType.CARD -> SakuinTheme.colors.walletCard
        WalletType.EWALLET -> SakuinTheme.colors.walletEwallet
    }

/** Text/icon color on top of [color]; yellow needs dark ink to stay readable. */
val WalletType.contentColor: Color
    @Composable @ReadOnlyComposable get() =
        if (this == WalletType.CARD) SakuinTheme.colors.onWalletDark else SakuinTheme.colors.onWallet

@get:DrawableRes
val WalletType.icon: Int
    get() = when (this) {
        WalletType.CASH -> R.drawable.ic_cash
        WalletType.CARD -> R.drawable.ic_card
        WalletType.EWALLET -> R.drawable.ic_ewallet
    }

@get:StringRes
val WalletType.label: Int
    get() = when (this) {
        WalletType.CASH -> R.string.wallet_type_cash
        WalletType.CARD -> R.string.wallet_type_card
        WalletType.EWALLET -> R.string.wallet_type_ewallet
    }

/** Solid wallet-colored square with the wallet type icon. */
@Composable
fun WalletIconBox(type: WalletType, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(type.color, RoundedCornerShape(SakuinTheme.radius.md)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(type.icon), contentDescription = null, modifier = Modifier.size(size / 2), tint = type.contentColor)
    }
}
