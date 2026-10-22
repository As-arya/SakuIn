package Sakuin.Project.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.CategoryKind
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.model.TransactionWithDetails
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.data.repository.TransactionFilter
import Sakuin.Project.ui.components.ButtonStyle
import Sakuin.Project.ui.components.asHeading
import Sakuin.Project.ui.components.CircleIconButton
import Sakuin.Project.ui.components.EmptyState
import Sakuin.Project.ui.components.SakuinButton
import Sakuin.Project.ui.components.SakuinChip
import Sakuin.Project.ui.components.TransactionRow
import Sakuin.Project.ui.components.categoryIcon
import Sakuin.Project.ui.components.icon
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val MonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val DayFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** Figma 02: month-by-month list grouped per day, with type chips, search and a filter sheet. */
@Composable
fun TransactionsScreen(onOpenTransaction: (Long) -> Unit) {
    val viewModel = sakuinViewModel {
        TransactionsViewModel(it.walletRepository, it.categoryRepository, it.transactionRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val colors = SakuinTheme.colors
    val zone = remember { ZoneId.systemDefault() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFilters by remember { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.transaction_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val extraFilters = filter.walletId != null || filter.categoryId != null

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.title_transactions), modifier = Modifier.asHeading(), style = SakuinTheme.typography.headlineLarge, color = colors.textPrimary)
                    CircleIconButton(
                        icon = R.drawable.ic_filter,
                        contentDescription = stringResource(R.string.filters_title),
                        onClick = { showFilters = true },
                        background = if (extraFilters) colors.primary else colors.bgSurface,
                        tint = if (extraFilters) colors.onPrimary else colors.textPrimary,
                    )
                }
            }
            item { SearchField(query, viewModel::setQuery, Modifier.padding(top = SakuinTheme.spacing.xs)) }
            item {
                MonthPicker(
                    month = month,
                    canGoNext = month < YearMonth.now(zone),
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                )
            }
            item { TypeChips(filter.type, viewModel::setType) }
            if (!state.loading && state.groups.isEmpty()) {
                item {
                    val filtered = query.isNotBlank() || filter.type != null || extraFilters
                    if (filtered) {
                        EmptyState(
                            icon = R.drawable.ic_search,
                            title = stringResource(R.string.transactions_no_matches_title),
                            body = stringResource(R.string.transactions_no_matches_body),
                            action = {
                                SakuinButton(
                                    text = stringResource(R.string.transactions_clear_filters),
                                    onClick = viewModel::clearAll,
                                    style = ButtonStyle.Secondary,
                                )
                            },
                        )
                    } else {
                        EmptyState(
                            icon = R.drawable.ic_receipt,
                            title = stringResource(R.string.transactions_empty_title),
                            body = stringResource(R.string.transactions_empty_body, MonthFormat.format(month)),
                        )
                    }
                }
            }
            state.groups.forEach { group ->
                item(key = "day-${group.date}") { DayHeader(group, zone) }
                // updatedAt is part of the key so a row restored by "Undo" gets fresh (not swiped-away) swipe state.
                items(group.items, key = { "${it.transaction.id}-${it.transaction.updatedAt}" }) { row ->
                    SwipeToDeleteRow(
                        item = row,
                        caption = TimeFormat.format(java.time.Instant.ofEpochMilli(row.transaction.occurredAt).atZone(zone)),
                        onClick = { onOpenTransaction(row.transaction.id) },
                        onDelete = {
                            viewModel.delete(row.transaction.id) { deleted ->
                                scope.launch {
                                    val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Long)
                                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(deleted)
                                }
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(SakuinTheme.radius.lg),
                containerColor = colors.textPrimary,
                contentColor = colors.bgSurface,
                actionColor = colors.walletTotal,
            )
        }
    }

    if (showFilters) {
        FilterSheet(
            wallets = wallets,
            categories = categories,
            filter = filter,
            onWallet = viewModel::setWallet,
            onCategory = viewModel::setCategory,
            onClear = viewModel::clearExtraFilters,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = SakuinTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bgSurface)
            .padding(start = 14.dp, end = SakuinTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_search), null, Modifier.size(20.dp), tint = colors.textSecondary)
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(stringResource(R.string.transactions_search), style = SakuinTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = SakuinTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onChange("") }) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_clear), Modifier.size(18.dp), tint = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun MonthPicker(month: YearMonth, canGoNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    val colors = SakuinTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.month_previous), tint = colors.textPrimary)
        }
        Text(
            MonthFormat.format(month),
            modifier = Modifier.weight(1f),
            style = SakuinTheme.typography.titleSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                stringResource(R.string.month_next),
                tint = if (canGoNext) colors.textPrimary else colors.border,
            )
        }
    }
}

@Composable
private fun TypeChips(selected: TransactionType?, onSelect: (TransactionType?) -> Unit) {
    val options = listOf(
        null to R.string.filter_all,
        TransactionType.EXPENSE to R.string.tab_expense,
        TransactionType.INCOME to R.string.tab_income,
        TransactionType.TRANSFER to R.string.tab_transfer,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SakuinTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
    ) {
        options.forEach { (type, label) ->
            SakuinChip(label = stringResource(label), selected = selected == type, onClick = { onSelect(type) })
        }
    }
}

@Composable
private fun DayHeader(group: DayGroup, zone: ZoneId) {
    val today = LocalDate.now(zone)
    val label = when (group.date) {
        today -> stringResource(R.string.day_today)
        today.minusDays(1) -> stringResource(R.string.day_yesterday)
        else -> DayFormat.format(group.date)
    }
    val net = when {
        group.net > 0 -> "+ ${formatRupiah(group.net)}"
        group.net < 0 -> "− ${formatRupiah(-group.net)}"
        else -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SakuinTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = SakuinTheme.typography.labelMedium, color = SakuinTheme.colors.textSecondary)
        Text(net, style = SakuinTheme.typography.labelMedium, color = SakuinTheme.colors.textSecondary)
    }
}

@Composable
private fun SwipeToDeleteRow(
    item: TransactionWithDetails,
    caption: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            // Only paint while swiping, so the red never peeks out around the row's rounded corners.
            if (state.dismissDirection != SwipeToDismissBoxValue.EndToStart) return@SwipeToDismissBox
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(SakuinTheme.radius.lg))
                    .background(SakuinTheme.colors.expense)
                    .padding(horizontal = SakuinTheme.spacing.xl),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(painterResource(R.drawable.ic_trash), stringResource(R.string.action_delete), tint = SakuinTheme.colors.onPrimary)
            }
        },
    ) {
        TransactionRow(item = item, trailingCaption = caption, onClick = onClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    wallets: List<WalletWithBalance>,
    categories: List<CategoryEntity>,
    filter: TransactionFilter,
    onWallet: (Long?) -> Unit,
    onCategory: (Long?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SakuinTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bgSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg),
        ) {
            Text(stringResource(R.string.filters_title), style = SakuinTheme.typography.titleMedium, color = colors.textPrimary)
            FilterGroup(R.string.filters_wallet) {
                SakuinChip(stringResource(R.string.filter_all), selected = filter.walletId == null, onClick = { onWallet(null) })
                wallets.forEach { item ->
                    SakuinChip(
                        label = item.wallet.name,
                        selected = filter.walletId == item.wallet.id,
                        onClick = { onWallet(item.wallet.id) },
                        icon = item.wallet.type.icon,
                    )
                }
            }
            listOf(CategoryKind.EXPENSE to R.string.filters_expense_category, CategoryKind.INCOME to R.string.filters_income_source)
                .forEach { (kind, label) ->
                    FilterGroup(label) {
                        categories.filter { it.kind == kind }.forEach { category ->
                            SakuinChip(
                                label = category.name,
                                selected = filter.categoryId == category.id,
                                onClick = { onCategory(if (filter.categoryId == category.id) null else category.id) },
                                icon = categoryIcon(category.iconKey),
                            )
                        }
                    }
                }
            Row(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md)) {
                SakuinButton(
                    text = stringResource(R.string.filters_clear),
                    onClick = onClear,
                    style = ButtonStyle.Outline,
                    modifier = Modifier.weight(1f),
                )
                SakuinButton(text = stringResource(R.string.action_done), onClick = onDismiss, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(label: Int, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
        Text(stringResource(label), style = SakuinTheme.typography.labelMedium, color = SakuinTheme.colors.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
        ) { chips() }
    }
}
