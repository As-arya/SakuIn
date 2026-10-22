package Sakuin.Project.ui.screens.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import Sakuin.Project.R
import Sakuin.Project.data.local.model.CategoryTotal
import Sakuin.Project.data.local.model.PeriodTotals
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.data.repository.MonthTotal
import Sakuin.Project.ui.components.SakuinChip
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah
import Sakuin.Project.util.formatRupiahCompact
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

private val MonthLabel = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
private val MonthShort = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
private val DayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

/** At most this many categories get their own slice; the rest are grouped into "Others". */
private const val MaxSlices = 5

/** Figma 09: totals, six-month income vs expense bars and the expense breakdown donut. */
@Composable
fun StatsScreen() {
    val viewModel = sakuinViewModel { StatsViewModel(it.walletRepository, it.transactionRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val walletId by viewModel.walletId.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val zone = remember { ZoneId.systemDefault() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.title_stats),
                    modifier = Modifier.weight(1f),
                    style = SakuinTheme.typography.headlineLarge,
                    color = SakuinTheme.colors.textPrimary,
                )
                WalletFilter(wallets, walletId, viewModel::setWallet)
            }
        }
        item {
            PeriodRow(
                selection = selection,
                canGoNext = !selection.contains(LocalDate.now(zone), zone),
                onPeriod = viewModel::setPeriod,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
            )
        }
        item { SummaryRow(state.totals) }
        item { IncomeExpenseChart(state.months, highlight = YearMonth.from(selection.anchor)) }
        item { CategoryBreakdown(state.byCategory) }
    }
}

@Composable
private fun WalletFilter(wallets: List<WalletWithBalance>, selectedId: Long?, onSelect: (Long?) -> Unit) {
    val colors = SakuinTheme.colors
    var open by remember { mutableStateOf(false) }
    val label = wallets.firstOrNull { it.wallet.id == selectedId }?.wallet?.name ?: stringResource(R.string.stats_all_wallets)
    Box {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.bgSurface)
                .clickable(role = Role.DropdownList) { open = true }
                .padding(horizontal = SakuinTheme.spacing.md, vertical = SakuinTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = SakuinTheme.typography.labelMedium, color = colors.textPrimary)
            Icon(painterResource(R.drawable.ic_chevron_down), null, Modifier.size(16.dp), tint = colors.textSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = colors.bgSurface) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.stats_all_wallets)) },
                onClick = {
                    onSelect(null)
                    open = false
                },
            )
            wallets.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.wallet.name) },
                    onClick = {
                        onSelect(item.wallet.id)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PeriodRow(
    selection: PeriodSelection,
    canGoNext: Boolean,
    onPeriod: (StatsPeriod) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = SakuinTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
            listOf(
                StatsPeriod.Week to R.string.stats_week,
                StatsPeriod.Month to R.string.stats_month,
                StatsPeriod.Year to R.string.stats_year,
            ).forEach { (period, label) ->
                SakuinChip(stringResource(label), selected = selection.period == period, onClick = { onPeriod(period) })
            }
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.month_previous), Modifier.size(18.dp), tint = colors.textPrimary)
        }
        Text(periodLabel(selection), style = SakuinTheme.typography.labelMedium, color = colors.textSecondary)
        IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.size(32.dp)) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                stringResource(R.string.month_next),
                Modifier.size(18.dp),
                tint = if (canGoNext) colors.textPrimary else colors.border,
            )
        }
    }
}

private fun periodLabel(selection: PeriodSelection): String = when (selection.period) {
    StatsPeriod.Week -> {
        val start = selection.anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        if (start.month == end.month) "${start.dayOfMonth}–${DayMonth.format(end)}" else "${DayMonth.format(start)} – ${DayMonth.format(end)}"
    }
    StatsPeriod.Month -> MonthLabel.format(selection.anchor)
    StatsPeriod.Year -> selection.anchor.year.toString()
}

@Composable
private fun SummaryRow(totals: PeriodTotals) {
    val colors = SakuinTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
        SummaryTile(R.string.tab_income, formatRupiahCompact(totals.income), colors.income, Modifier.weight(1f))
        SummaryTile(R.string.tab_expense, formatRupiahCompact(totals.expense), colors.expense, Modifier.weight(1f))
        SummaryTile(
            R.string.stats_net,
            formatRupiahCompact(totals.net),
            if (totals.net < 0) colors.expense else colors.textPrimary,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryTile(@StringRes label: Int, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SakuinTheme.colors.bgSurface, RoundedCornerShape(14.dp))
            .padding(SakuinTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(stringResource(label), style = SakuinTheme.typography.caption, color = SakuinTheme.colors.textSecondary)
        Text(value, style = SakuinTheme.typography.bodyStrong, color = color, maxLines = 1)
    }
}

@Composable
private fun ChartCard(title: String, legend: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SakuinTheme.colors.bgSurface, RoundedCornerShape(SakuinTheme.radius.xl))
            .padding(SakuinTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = SakuinTheme.typography.titleSmall, color = SakuinTheme.colors.textPrimary)
            legend()
        }
        content()
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = SakuinTheme.typography.caption, color = SakuinTheme.colors.textSecondary)
    }
}

@Composable
private fun IncomeExpenseChart(months: List<MonthTotal>, highlight: YearMonth) {
    val colors = SakuinTheme.colors
    val incomeLabel = stringResource(R.string.tab_income)
    val expenseLabel = stringResource(R.string.tab_expense)
    val max = months.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1) ?: 1
    val description = months.joinToString("; ") {
        "${MonthLabel.format(it.month)}: $incomeLabel ${formatRupiah(it.income)}, $expenseLabel ${formatRupiah(it.expense)}"
    }
    ChartCard(
        title = stringResource(R.string.stats_income_vs_expense),
        legend = {
            Row(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md)) {
                LegendDot(colors.income, incomeLabel)
                LegendDot(colors.expense, expenseLabel)
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = description },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            months.forEach { month ->
                val emphasis = if (month.month == highlight) 1f else 0.35f
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                        Bar(month.income, max, colors.income.copy(alpha = emphasis))
                        Bar(month.expense, max, colors.expense.copy(alpha = emphasis))
                    }
                    Text(
                        MonthShort.format(month.month),
                        style = SakuinTheme.typography.caption,
                        color = if (month.month == highlight) colors.textPrimary else colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Bar(value: Long, max: Long, color: Color) {
    val height = if (value <= 0) 0.dp else (110.dp * (value.toFloat() / max)).coerceAtLeast(3.dp)
    Box(
        Modifier
            .width(12.dp)
            .height(height)
            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
    )
}

private data class Slice(val label: String, val amount: Long, val color: Color)

@Composable
private fun CategoryBreakdown(byCategory: List<CategoryTotal>) {
    val colors = SakuinTheme.colors
    val palette = listOf(colors.walletTotal, colors.walletCard, colors.walletEwallet, colors.walletCash, Color(0xFF8B5CF6))
    val uncategorized = stringResource(R.string.stats_uncategorized)
    val others = stringResource(R.string.stats_others)
    val sorted = byCategory.filter { it.total > 0 }.sortedByDescending { it.total }
    val slices = sorted.take(MaxSlices).mapIndexed { i, c -> Slice(c.categoryName ?: uncategorized, c.total, palette[i]) } +
        sorted.drop(MaxSlices).sumOf { it.total }.takeIf { it > 0 }?.let { listOf(Slice(others, it, colors.textSecondary)) }.orEmpty()
    val total = slices.sumOf { it.amount }

    ChartCard(title = stringResource(R.string.stats_expense_by_category)) {
        if (total == 0L) {
            Text(
                stringResource(R.string.stats_no_expenses),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SakuinTheme.spacing.lg),
                style = SakuinTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            return@ChartCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(124.dp), contentAlignment = Alignment.Center) {
                Donut(slices, total)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.stats_total), style = SakuinTheme.typography.caption, color = colors.textSecondary)
                    Text(formatRupiahCompact(total), style = SakuinTheme.typography.bodyStrong, color = colors.textPrimary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                slices.forEach { slice ->
                    Row(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(slice.color, CircleShape))
                        Text(
                            slice.label,
                            modifier = Modifier.weight(1f),
                            style = SakuinTheme.typography.labelMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                        )
                        Text(
                            "${(slice.amount * 100.0 / total).roundToInt()}%",
                            style = SakuinTheme.typography.labelMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Donut(slices: List<Slice>, total: Long) {
    Canvas(Modifier.fillMaxSize()) {
        val stroke = size.minDimension * 0.15f
        val inset = stroke / 2
        val gap = if (slices.size > 1) 2f else 0f
        var start = -90f
        slices.forEach { slice ->
            val sweep = slice.amount * 360f / total
            drawArc(
                color = slice.color,
                startAngle = start + gap / 2,
                sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke),
            )
            start += sweep
        }
    }
}
