package Sakuin.Project.ui.screens.addtransaction

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.createSavedStateHandle
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.ui.components.AmountInput
import Sakuin.Project.ui.components.CircleIconButton
import Sakuin.Project.ui.components.DateTimeField
import Sakuin.Project.ui.components.ReceiptPhotoDialog
import Sakuin.Project.ui.components.ReceiptThumbnail
import Sakuin.Project.ui.components.SakuinButton
import Sakuin.Project.ui.components.SakuinChip
import Sakuin.Project.ui.components.SakuinPickerField
import Sakuin.Project.ui.components.SakuinTextField
import Sakuin.Project.ui.components.SakuinTopBar
import Sakuin.Project.ui.components.SegmentedControl
import Sakuin.Project.ui.components.ThousandsSeparatorTransformation
import Sakuin.Project.ui.components.WalletIconBox
import Sakuin.Project.ui.components.WalletPickerSheet
import Sakuin.Project.ui.components.categoryIcon
import Sakuin.Project.ui.components.icon
import Sakuin.Project.ui.components.sanitizeAmountInput
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah
import Sakuin.Project.util.parseRupiahInput
import java.io.File

private enum class PickerTarget { Wallet, From, To }

/** Usual Indonesian fees: e-wallet top-up, BI-FAST, and an online interbank transfer. */
private val CommonFees = listOf(1_000L, 2_500L, 6_500L)

/** Figma 03–05: Expense / Income / Transfer form, also used to edit an existing transaction. */
@Composable
fun AddTransactionScreen(onBack: () -> Unit, onAddWallet: () -> Unit, onScan: () -> Unit, onDone: () -> Unit) {
    val viewModel = sakuinViewModel {
        AddTransactionViewModel(
            createSavedStateHandle(),
            it.walletRepository,
            it.categoryRepository,
            it.transactionRepository,
            it.receiptStore,
            it.settingsRepository,
        )
    }
    val colors = SakuinTheme.colors
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val scanEnabled by viewModel.scanEnabled.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf<PickerTarget?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var viewingReceipt by remember { mutableStateOf(false) }
    val tabs = AddTransactionViewModel.Tabs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .imePadding(),
    ) {
        SakuinTopBar(
            title = stringResource(if (viewModel.isEdit) R.string.title_edit_transaction else R.string.title_add_transaction),
            onBack = onBack,
            actions = {
                if (viewModel.isEdit) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(painterResource(R.drawable.ic_trash), stringResource(R.string.action_delete), tint = colors.expense)
                    }
                } else if (viewModel.tab == TransactionType.EXPENSE && scanEnabled) {
                    IconButton(onClick = onScan) {
                        Icon(painterResource(R.drawable.ic_scan), stringResource(R.string.title_scan), tint = colors.textPrimary)
                    }
                }
            },
        )
        SegmentedControl(
            options = listOf(
                stringResource(R.string.tab_expense),
                stringResource(R.string.tab_income),
                stringResource(R.string.tab_transfer),
            ),
            selectedIndex = tabs.indexOf(viewModel.tab),
            onSelect = { viewModel.selectTab(tabs[it]) },
            modifier = Modifier.padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.sm),
        )

        val needed = if (viewModel.tab == TransactionType.TRANSFER) 2 else 1
        if (wallets.size < needed) {
            NeedWallets(viewModel.tab, onAddWallet)
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg),
        ) {
            AmountInput(
                digits = viewModel.amountDigits,
                onDigitsChange = viewModel::onAmountChange,
                color = when (viewModel.tab) {
                    TransactionType.EXPENSE -> colors.expense
                    TransactionType.INCOME -> colors.income
                    TransactionType.TRANSFER -> colors.textPrimary
                },
            )
            if (viewModel.error == FormError.AmountRequired) {
                ErrorText(R.string.error_amount_required, Modifier.fillMaxWidth(), center = true)
            }
            when (viewModel.tab) {
                TransactionType.EXPENSE -> ExpenseFields(viewModel, wallets, onPickWallet = { picking = PickerTarget.Wallet })
                TransactionType.INCOME -> IncomeFields(viewModel, wallets)
                TransactionType.TRANSFER -> TransferFields(
                    viewModel,
                    wallets,
                    onPickFrom = { picking = PickerTarget.From },
                    onPickTo = { picking = PickerTarget.To },
                )
            }
            DateTimeField(
                millis = viewModel.occurredAt,
                onChange = { viewModel.occurredAt = it },
                label = stringResource(R.string.field_date),
            )
            SakuinTextField(
                value = viewModel.note,
                onValueChange = { viewModel.note = it.take(100) },
                label = stringResource(R.string.field_note_optional),
                leadingIcon = R.drawable.ic_edit,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            )
            viewModel.receiptFile?.let { file -> ReceiptPhotoRow(file, onClick = { viewingReceipt = true }) }
            if (viewModel.tab == TransactionType.TRANSFER) {
                Text(stringResource(R.string.transfer_info), style = SakuinTheme.typography.caption, color = colors.textSecondary)
            }
        }
        SakuinButton(
            text = stringResource(
                when {
                    viewModel.isEdit -> R.string.action_save_changes
                    viewModel.tab == TransactionType.EXPENSE -> R.string.expense_save
                    viewModel.tab == TransactionType.INCOME -> R.string.income_save
                    else -> R.string.transfer_save
                },
            ),
            onClick = { viewModel.save(onDone) },
            enabled = !viewModel.saving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.md),
        )
    }

    picking?.let { target ->
        WalletPickerSheet(
            wallets = wallets,
            selectedId = when (target) {
                PickerTarget.Wallet -> viewModel.walletId
                PickerTarget.From -> viewModel.fromWalletId
                PickerTarget.To -> viewModel.toWalletId
            },
            onSelect = { id ->
                when (target) {
                    PickerTarget.Wallet -> viewModel.selectWallet(id)
                    PickerTarget.From -> viewModel.selectFrom(id)
                    PickerTarget.To -> viewModel.selectTo(id)
                }
                picking = null
            },
            onDismiss = { picking = null },
            onAddWallet = {
                picking = null
                onAddWallet()
            },
        )
    }

    val receipt = viewModel.receiptFile
    if (viewingReceipt && receipt != null) {
        ReceiptPhotoDialog(file = receipt, onDismiss = { viewingReceipt = false })
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = { Text(stringResource(R.string.delete_transaction_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDone)
                }) { Text(stringResource(R.string.action_delete), color = colors.expense) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            containerColor = colors.bgSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseFields(viewModel: AddTransactionViewModel, wallets: List<WalletWithBalance>, onPickWallet: () -> Unit) {
    val categories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val suggestions by viewModel.titleSuggestions.collectAsStateWithLifecycle()
    val wallet = wallets.firstOrNull { it.wallet.id == viewModel.walletId }
    val amount = parseRupiahInput(viewModel.amountDigits) ?: 0

    CategoryChips(R.string.field_category, categories, viewModel.expenseCategoryId, viewModel::selectCategory)
    if (viewModel.error == FormError.CategoryRequired) ErrorText(R.string.error_category_required)
    Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
        SakuinTextField(
            value = viewModel.title,
            onValueChange = viewModel::onTitleChange,
            label = stringResource(R.string.field_item_name),
            leadingIcon = R.drawable.ic_tag,
            placeholder = stringResource(R.string.field_item_name_placeholder),
            isError = viewModel.error == FormError.TitleRequired,
            helper = if (viewModel.error == FormError.TitleRequired) stringResource(R.string.error_item_name_required) else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
                suggestions.forEach { suggestion ->
                    SakuinChip(label = suggestion, selected = false, onClick = { viewModel.onTitleChange(suggestion) })
                }
            }
        }
    }
    SakuinPickerField(
        value = wallet?.wallet?.name.orEmpty(),
        label = stringResource(R.string.field_pay_from),
        onClick = onPickWallet,
        leadingIcon = wallet?.wallet?.type?.icon ?: R.drawable.ic_wallet,
    )
    if (wallet != null && amount > 0 && viewModel.balanceAfter(wallet, amount) < 0) {
        Text(
            stringResource(R.string.transfer_negative_warning, wallet.wallet.name),
            style = SakuinTheme.typography.caption,
            color = SakuinTheme.colors.warning,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IncomeFields(viewModel: AddTransactionViewModel, wallets: List<WalletWithBalance>) {
    val categories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    CategoryChips(R.string.field_source, categories, viewModel.incomeCategoryId, viewModel::selectCategory)
    if (viewModel.error == FormError.CategoryRequired) ErrorText(R.string.error_category_required)
    Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
        Text(stringResource(R.string.field_to_wallet), style = SakuinTheme.typography.labelMedium, color = SakuinTheme.colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
            wallets.forEach { item ->
                SakuinChip(
                    label = item.wallet.name,
                    selected = item.wallet.id == viewModel.walletId,
                    onClick = { viewModel.selectWallet(item.wallet.id) },
                    icon = item.wallet.type.icon,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.TransferFields(
    viewModel: AddTransactionViewModel,
    wallets: List<WalletWithBalance>,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
) {
    val from = wallets.firstOrNull { it.wallet.id == viewModel.fromWalletId }
    val to = wallets.firstOrNull { it.wallet.id == viewModel.toWalletId }
    val amount = parseRupiahInput(viewModel.amountDigits) ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
        WalletSelectRow(R.string.transfer_from, from, onClick = onPickFrom)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            CircleIconButton(
                icon = R.drawable.ic_arrow_down,
                contentDescription = stringResource(R.string.transfer_swap),
                onClick = viewModel::swapWallets,
                background = SakuinTheme.colors.bgSurfaceVariant,
                size = 36,
            )
        }
        WalletSelectRow(R.string.transfer_to, to, onClick = onPickTo)
    }
    Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
        SakuinTextField(
            value = viewModel.feeDigits,
            onValueChange = { viewModel.onFeeChange(sanitizeAmountInput(it)) },
            label = stringResource(R.string.transfer_fee),
            leadingIcon = R.drawable.ic_coins,
            placeholder = "0",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            visualTransformation = ThousandsSeparatorTransformation,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
            CommonFees.forEach { preset ->
                val selected = viewModel.feeDigits == preset.toString()
                SakuinChip(
                    label = formatRupiah(preset),
                    selected = selected,
                    onClick = { viewModel.onFeeChange(if (selected) "" else preset.toString()) },
                )
            }
        }
    }
    if (viewModel.error == FormError.SameWallet) {
        ErrorText(R.string.error_two_wallets)
    } else if (from != null && amount > 0 && viewModel.balanceAfter(from, amount) < 0) {
        Text(
            stringResource(R.string.transfer_negative_warning, from.wallet.name),
            style = SakuinTheme.typography.caption,
            color = SakuinTheme.colors.warning,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    @StringRes label: Int,
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
        Text(stringResource(label), style = SakuinTheme.typography.labelMedium, color = SakuinTheme.colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
            categories.forEach { category ->
                SakuinChip(
                    label = category.name,
                    selected = category.id == selectedId,
                    onClick = { onSelect(category.id) },
                    icon = categoryIcon(category.iconKey),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.NeedWallets(tab: TransactionType, onAddWallet: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(SakuinTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(if (tab == TransactionType.TRANSFER) R.string.transfer_need_two_wallets else R.string.need_wallet),
            style = SakuinTheme.typography.bodyMedium,
            color = SakuinTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        SakuinButton(text = stringResource(R.string.wallet_add), onClick = onAddWallet, icon = R.drawable.ic_plus)
    }
}

@Composable
private fun WalletSelectRow(@StringRes label: Int, item: WalletWithBalance?, onClick: () -> Unit) {
    val colors = SakuinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SakuinTheme.radius.lg))
            .background(colors.bgApp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item != null) WalletIconBox(item.wallet.type)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(label), style = SakuinTheme.typography.caption, color = colors.textSecondary)
            Text(
                item?.wallet?.name ?: stringResource(R.string.transfer_choose_wallet),
                style = SakuinTheme.typography.bodyStrong,
                color = colors.textPrimary,
            )
        }
        if (item != null) {
            Text(formatRupiah(item.balance), style = SakuinTheme.typography.labelMedium, color = colors.textSecondary)
        }
        Icon(painterResource(R.drawable.ic_chevron_down), null, Modifier.size(20.dp), tint = colors.textSecondary)
    }
}

/** The photo a scanned expense came from, opened full screen on tap. */
@Composable
private fun ReceiptPhotoRow(file: File, onClick: () -> Unit) {
    val colors = SakuinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SakuinTheme.radius.lg))
            .background(colors.bgApp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(SakuinTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReceiptThumbnail(file = file, onClick = null, modifier = Modifier.size(width = 40.dp, height = 48.dp))
        Text(
            stringResource(R.string.receipt_photo),
            modifier = Modifier.weight(1f),
            style = SakuinTheme.typography.bodyStrong,
            color = colors.textPrimary,
        )
        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp), tint = colors.textSecondary)
    }
}

@Composable
private fun ErrorText(@StringRes text: Int, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        stringResource(text),
        modifier = modifier,
        style = SakuinTheme.typography.caption,
        color = SakuinTheme.colors.expense,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
    )
}
