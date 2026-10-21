package Sakuin.Project.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Primitives, mirrored from the Figma "Primitives" collection.
internal object Palette {
    val Gray0 = Color(0xFFFFFFFF)
    val Gray50 = Color(0xFFF5F6F8)
    val Gray100 = Color(0xFFEDEFF2)
    val Gray200 = Color(0xFFE1E4E9)
    val Gray400 = Color(0xFF9BA1AB)
    val Gray500 = Color(0xFF6A707A)
    val Gray700 = Color(0xFF363B44)
    val Gray800 = Color(0xFF1E2228)
    val Gray850 = Color(0xFF16191E)
    val Gray900 = Color(0xFF0E1014)

    val Blue600 = Color(0xFF1F5BDD)
    val Blue300 = Color(0xFF7FA3F4)
    val Blue950 = Color(0xFF0A1633)
    val Green600 = Color(0xFF15934A)
    val Green300 = Color(0xFF5FD08C)
    val Red600 = Color(0xFFD13A2C)
    val Red300 = Color(0xFFF27A6E)
    val Amber500 = Color(0xFFF59E0B)
    val Amber400 = Color(0xFFFBBF24)

    val StackBlue = Color(0xFF4C7EEB)
    val StackRed = Color(0xFFDD4B39)
    val StackYellow = Color(0xFFF2B630)
    val StackGreen = Color(0xFF3FA45B)
}

/**
 * Semantic colors, mirrored from the Figma "Color" collection (Light/Dark modes).
 * Hierarchy: neutrals carry most of the UI, [primary] is only for the main action,
 * selection, active tab and links, wallet colors only identify wallet types,
 * [income] is green and [expense] is red.
 */
@Immutable
data class SakuinColors(
    val bgApp: Color,
    val bgSurface: Color,
    val bgSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textBrand: Color,
    val border: Color,
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val warning: Color,
    val walletTotal: Color,
    val walletCash: Color,
    val walletCard: Color,
    val walletEwallet: Color,
    val onWallet: Color,
    val onWalletDark: Color,
)

internal val LightSakuinColors = SakuinColors(
    bgApp = Palette.Gray50,
    bgSurface = Palette.Gray0,
    bgSurfaceVariant = Palette.Gray100,
    primary = Palette.Blue600,
    onPrimary = Palette.Gray0,
    textPrimary = Palette.Gray900,
    textSecondary = Palette.Gray500,
    textBrand = Palette.Blue600,
    border = Palette.Gray200,
    income = Palette.Green600,
    expense = Palette.Red600,
    transfer = Palette.Blue600,
    warning = Palette.Amber500,
    walletTotal = Palette.StackBlue,
    walletCash = Palette.StackRed,
    walletCard = Palette.StackYellow,
    walletEwallet = Palette.StackGreen,
    onWallet = Palette.Gray0,
    onWalletDark = Palette.Gray900,
)

internal val DarkSakuinColors = SakuinColors(
    bgApp = Palette.Gray900,
    bgSurface = Palette.Gray850,
    bgSurfaceVariant = Palette.Gray800,
    primary = Palette.Blue300,
    onPrimary = Palette.Blue950,
    textPrimary = Palette.Gray50,
    textSecondary = Palette.Gray400,
    textBrand = Palette.Blue300,
    border = Palette.Gray700,
    income = Palette.Green300,
    expense = Palette.Red300,
    transfer = Palette.Blue300,
    warning = Palette.Amber400,
    walletTotal = Palette.StackBlue,
    walletCash = Palette.StackRed,
    walletCard = Palette.StackYellow,
    walletEwallet = Palette.StackGreen,
    onWallet = Palette.Gray0,
    onWalletDark = Palette.Gray900,
)
