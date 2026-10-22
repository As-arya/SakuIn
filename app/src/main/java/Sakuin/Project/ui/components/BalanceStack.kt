package Sakuin.Project.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import Sakuin.Project.R
import Sakuin.Project.data.repository.BalanceSummary
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah
import kotlin.math.abs
import kotlinx.coroutines.launch

private val CardHeight = 176.dp
private val PeekStep = 16.dp
private const val CardCount = 4

/**
 * Card silhouette from the Figma `BalanceStack`: rounded corners with a wave along the top edge.
 * Corners scale with the height so they stay round on any width; only the wave stretches.
 */
private val StackCardShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val s = h / 176f
    val fx = w / 328f
    moveTo(0f, 22f * s)
    cubicTo(0f, 9.85f * s, 9.85f * s, 0f, 22f * s, 0f)
    lineTo(120f * fx, 0f)
    cubicTo(150f * fx, 0f, 170f * fx, 30f * s, 214f * fx, 30f * s)
    cubicTo(250f * fx, 30f * s, 262f * fx, 8f * s, 290f * fx, 2f * s)
    cubicTo(311f * fx, -2f * s, w, 8f * s, w, 24f * s)
    lineTo(w, h - 24f * s)
    cubicTo(w, h - 10.75f * s, w - 10.75f * s, h, w - 24f * s, h)
    lineTo(24f * s, h)
    cubicTo(10.75f * s, h, 0f, h - 10.75f * s, 0f, h - 24f * s)
    close()
}

private data class StackCard(
    val label: Int,
    @param:DrawableRes val icon: Int,
    val amount: Long,
    val color: Color,
    val content: Color,
)

/**
 * Home balance stack (Figma `BalanceStack`): Total → Cash → Card → E-wallet. Swiping the front card
 * either way (or tapping it) sends it to the back and brings the next one forward.
 */
@Composable
fun BalanceStack(
    summary: BalanceSummary,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SakuinTheme.colors
    val cards = listOf(
        StackCard(R.string.balance_total, R.drawable.ic_wallet, summary.total, colors.walletTotal, colors.onWallet),
        StackCard(R.string.wallet_type_cash, R.drawable.ic_cash, summary.cash, colors.walletCash, colors.onWallet),
        StackCard(R.string.wallet_type_card, R.drawable.ic_card, summary.card, colors.walletCard, colors.onWalletDark),
        StackCard(R.string.wallet_type_ewallet, R.drawable.ic_ewallet, summary.ewallet, colors.walletEwallet, colors.onWallet),
    )
    var front by rememberSaveable { mutableIntStateOf(0) }
    val dragX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val nextLabel = stringResource(R.string.balance_next_card)

    fun advance() {
        front = (front + 1) % CardCount
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight + PeekStep * (CardCount - 1))
            .semantics {
                customActions = listOf(CustomAccessibilityAction(nextLabel) { advance(); true })
            },
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val threshold = widthPx * 0.25f
        cards.forEachIndexed { index, card ->
            val depth = (index - front + CardCount) % CardCount
            val isFront = depth == 0
            val y by animateDpAsState(PeekStep * (CardCount - 1 - depth), tween(300), label = "cardY")
            val contentAlpha by animateFloatAsState(if (isFront) 1f else 0f, tween(200), label = "cardContent")
            Box(
                modifier = Modifier
                    .zIndex(-depth.toFloat())
                    .offset(y = y)
                    .fillMaxWidth()
                    .height(CardHeight)
                    .graphicsLayer {
                        if (isFront) {
                            translationX = dragX.value
                            rotationZ = dragX.value / widthPx * 6f
                        }
                    }
                    .clip(StackCardShape)
                    .background(card.color)
                    .then(
                        if (isFront) {
                            Modifier
                                .pointerInput(front) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            scope.launch {
                                                if (abs(dragX.value) > threshold) {
                                                    dragX.animateTo(if (dragX.value < 0) -widthPx else widthPx, tween(180))
                                                    advance()
                                                    dragX.snapTo(0f)
                                                } else {
                                                    dragX.animateTo(0f)
                                                }
                                            }
                                        },
                                        onDragCancel = { scope.launch { dragX.animateTo(0f) } },
                                    ) { change, amount ->
                                        change.consume()
                                        scope.launch { dragX.snapTo(dragX.value + amount) }
                                    }
                                }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { advance() }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                CardContent(
                    card = card,
                    position = index,
                    isFront = isFront,
                    hidden = hidden,
                    onToggleHidden = onToggleHidden,
                    modifier = Modifier.alpha(contentAlpha),
                )
            }
        }
    }
}

@Composable
private fun CardContent(
    card: StackCard,
    position: Int,
    isFront: Boolean,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // Cards behind the front one are decoration only.
            .then(if (isFront) Modifier else Modifier.clearAndSetSemantics { }),
    ) {
        // Title, eye button and amount sit together near the top of the card.
        Column(
            modifier = Modifier.padding(start = 20.dp, end = SakuinTheme.spacing.sm, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(card.icon), null, Modifier.size(18.dp), tint = card.content)
                Text(
                    stringResource(card.label),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = SakuinTheme.spacing.sm),
                    style = SakuinTheme.typography.bodyStrong,
                    color = card.content,
                )
                IconButton(onClick = onToggleHidden) {
                    Icon(
                        painterResource(R.drawable.ic_eye),
                        stringResource(if (hidden) R.string.balance_show else R.string.balance_hide),
                        Modifier.size(20.dp),
                        tint = card.content,
                    )
                }
            }
            Text(
                text = if (hidden) "Rp ••••••" else formatRupiah(card.amount),
                modifier = Modifier.padding(end = SakuinTheme.spacing.md),
                style = SakuinTheme.typography.displayAmount,
                color = card.content,
                maxLines = 1,
                softWrap = false,
            )
        }
        // The page dots stay in the bottom-right corner.
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(CardCount) { dot ->
                Box(
                    Modifier
                        .size(width = if (dot == position) 14.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(card.content.copy(alpha = if (dot == position) 1f else 0.45f)),
                )
            }
        }
    }
}
