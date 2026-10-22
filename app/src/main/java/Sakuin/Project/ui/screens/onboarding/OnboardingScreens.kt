package Sakuin.Project.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.WalletType
import Sakuin.Project.ui.components.PageDots
import Sakuin.Project.ui.components.asHeading
import Sakuin.Project.ui.components.SakuinButton
import Sakuin.Project.ui.components.SakuinTextField
import Sakuin.Project.ui.components.ThousandsSeparatorTransformation
import Sakuin.Project.ui.components.WalletIconBox
import Sakuin.Project.ui.components.label
import Sakuin.Project.ui.components.sanitizeAmountInput
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah
import kotlinx.coroutines.launch

private data class Slide(@param:DrawableRes val art: Int, @param:StringRes val title: Int, @param:StringRes val body: Int)

private val Slides = listOf(
    Slide(R.drawable.ill_track, R.string.onboarding_track_title, R.string.onboarding_track_body),
    Slide(R.drawable.ill_offline, R.string.onboarding_offline_title, R.string.onboarding_offline_body),
    Slide(R.drawable.ill_scan, R.string.onboarding_scan_title, R.string.onboarding_scan_body),
)

/** Figma 00b–00d: three swipeable intro slides with Skip. */
@Composable
fun OnboardingIntroScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { Slides.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == Slides.lastIndex
    OnboardingSurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SakuinTheme.spacing.sm),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = stringResource(R.string.action_skip),
                modifier = Modifier
                    .alpha(if (isLast) 0f else 1f)
                    .clip(CircleShape)
                    .clickable(enabled = !isLast, role = Role.Button, onClick = onFinish)
                    .padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.md),
                style = SakuinTheme.typography.labelMedium,
                color = SakuinTheme.colors.textSecondary,
            )
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val slide = Slides[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SakuinTheme.spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Illustration(slide.art, width = 300)
                Spacer(Modifier.height(40.dp))
                Text(
                    stringResource(slide.title),
                    modifier = Modifier.asHeading(),
                    style = SakuinTheme.typography.headlineLarge,
                    color = SakuinTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(SakuinTheme.spacing.md))
                Text(
                    stringResource(slide.body),
                    style = SakuinTheme.typography.bodyMedium,
                    color = SakuinTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        PageDots(
            count = Slides.size,
            current = pager.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = SakuinTheme.spacing.lg),
        )
        OnboardingFooter(
            text = stringResource(if (isLast) R.string.action_get_started else R.string.action_next),
            onClick = {
                if (isLast) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
        )
    }
}

/** Figma 00e: step 1/2, the user's name. */
@Composable
fun OnboardingNameScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    val viewModel = sakuinViewModel { OnboardingNameViewModel(it.settingsRepository) }
    OnboardingSurface {
        StepHeader(step = 1, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.xl),
        ) {
            Illustration(R.drawable.ill_profile, width = 240, modifier = Modifier.align(Alignment.CenterHorizontally))
            StepTitle(R.string.onboarding_name_title, R.string.onboarding_name_body)
            SakuinTextField(
                value = viewModel.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.field_your_name),
                leadingIcon = R.drawable.ic_user,
                isError = viewModel.showError,
                helper = if (viewModel.showError) stringResource(R.string.error_name_required) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.save(onContinue) }),
            )
        }
        OnboardingFooter(text = stringResource(R.string.action_continue), onClick = { viewModel.save(onContinue) })
    }
}

/** Figma 00f: step 2/2, the first wallet. */
@Composable
fun OnboardingWalletScreen(onBack: () -> Unit, onCreated: () -> Unit) {
    val defaultName = stringResource(R.string.onboarding_default_wallet_name)
    val viewModel = sakuinViewModel {
        OnboardingWalletViewModel(defaultName, it.walletRepository, it.settingsRepository)
    }
    OnboardingSurface {
        StepHeader(step = 2, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.xl),
        ) {
            StepTitle(R.string.onboarding_wallet_title, R.string.onboarding_wallet_body)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WalletType.entries.forEach { type ->
                    TypeTile(
                        type = type,
                        selected = viewModel.type == type,
                        onClick = { viewModel.type = type },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            SakuinTextField(
                value = viewModel.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.field_wallet_name),
                leadingIcon = R.drawable.ic_wallet,
                isError = viewModel.showNameError,
                helper = if (viewModel.showNameError) stringResource(R.string.error_name_required) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            SakuinTextField(
                value = viewModel.balanceDigits,
                onValueChange = { viewModel.balanceDigits = sanitizeAmountInput(it) },
                label = stringResource(R.string.field_starting_balance),
                leadingIcon = R.drawable.ic_coins,
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.create(onCreated) }),
                visualTransformation = ThousandsSeparatorTransformation,
            )
        }
        OnboardingFooter(
            text = stringResource(R.string.action_continue),
            onClick = { viewModel.create(onCreated) },
            enabled = !viewModel.saving,
        )
    }
}

/** Figma 00g: confirmation with the wallet that was just created. */
@Composable
fun OnboardingDoneScreen(onGoHome: () -> Unit) {
    val viewModel = sakuinViewModel { OnboardingDoneViewModel(it.settingsRepository, it.walletRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onGoHome)
    OnboardingSurface {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg, Alignment.CenterVertically),
        ) {
            Illustration(R.drawable.ill_done, width = 280)
            Text(
                stringResource(R.string.onboarding_done_title, state.userName),
                modifier = Modifier.asHeading(),
                style = SakuinTheme.typography.headlineLarge,
                color = SakuinTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.onboarding_done_body),
                style = SakuinTheme.typography.bodyMedium,
                color = SakuinTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            state.wallet?.let { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SakuinTheme.colors.bgApp, RoundedCornerShape(SakuinTheme.radius.lg))
                        .padding(SakuinTheme.spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WalletIconBox(item.wallet.type)
                    Column(Modifier.weight(1f)) {
                        Text(item.wallet.name, style = SakuinTheme.typography.bodyStrong, color = SakuinTheme.colors.textPrimary)
                        Text(stringResource(item.wallet.type.label), style = SakuinTheme.typography.caption, color = SakuinTheme.colors.textSecondary)
                    }
                    Text(formatRupiah(item.balance), style = SakuinTheme.typography.bodyStrong, color = SakuinTheme.colors.textPrimary)
                }
            }
        }
        OnboardingFooter(text = stringResource(R.string.action_go_home), onClick = onGoHome)
    }
}

@Composable
private fun OnboardingSurface(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SakuinTheme.colors.bgSurface)
            .imePadding(),
        content = content,
    )
}

@Composable
private fun OnboardingFooter(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    SakuinButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SakuinTheme.spacing.xl, vertical = SakuinTheme.spacing.lg),
    )
}

@Composable
private fun StepHeader(step: Int, onBack: () -> Unit, total: Int = 2) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SakuinTheme.spacing.xs, end = SakuinTheme.spacing.xl, top = SakuinTheme.spacing.sm, bottom = SakuinTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.action_back), tint = SakuinTheme.colors.textPrimary)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (index < step) SakuinTheme.colors.primary else SakuinTheme.colors.border),
                )
            }
        }
        Text(stringResource(R.string.onboarding_step, step, total), style = SakuinTheme.typography.labelMedium, color = SakuinTheme.colors.textSecondary)
    }
}

@Composable
private fun StepTitle(@StringRes title: Int, @StringRes body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
        Text(stringResource(title), modifier = Modifier.asHeading(), style = SakuinTheme.typography.headlineLarge, color = SakuinTheme.colors.textPrimary)
        Text(stringResource(body), style = SakuinTheme.typography.bodyMedium, color = SakuinTheme.colors.textSecondary)
    }
}

@Composable
private fun TypeTile(type: WalletType, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(SakuinTheme.radius.lg)
    Column(
        modifier = modifier
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) SakuinTheme.colors.primary else SakuinTheme.colors.border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = SakuinTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WalletIconBox(type, size = 48.dp)
        Text(stringResource(type.label), style = SakuinTheme.typography.bodyStrong, color = SakuinTheme.colors.textPrimary)
    }
}

@Composable
private fun Illustration(@DrawableRes art: Int, width: Int, modifier: Modifier = Modifier) {
    val painter = painterResource(art)
    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
            .width(width.dp)
            .aspectRatio(painter.intrinsicSize.width / painter.intrinsicSize.height),
    )
}
