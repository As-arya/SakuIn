package Sakuin.Project.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.CategoryKind
import Sakuin.Project.data.repository.CategoryRepository
import Sakuin.Project.ui.components.ButtonStyle
import Sakuin.Project.ui.components.CategoryIconKeys
import Sakuin.Project.ui.components.SakuinButton
import Sakuin.Project.ui.components.SakuinTextField
import Sakuin.Project.ui.components.SakuinTopBar
import Sakuin.Project.ui.components.SegmentedControl
import Sakuin.Project.ui.components.categoryIcon
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.theme.SakuinTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repository: CategoryRepository) : ViewModel() {
    val expense: StateFlow<List<CategoryEntity>> = repository.observeCategories(CategoryKind.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val income: StateFlow<List<CategoryEntity>> = repository.observeCategories(CategoryKind.INCOME)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(editing: CategoryEntity?, kind: CategoryKind, name: String, iconKey: String) {
        viewModelScope.launch {
            if (editing == null) repository.addCategory(name, kind, iconKey) else repository.updateCategory(editing.id, name, iconKey)
        }
    }

    fun archive(id: Long) {
        viewModelScope.launch { repository.archiveCategory(id) }
    }
}

/** Sheet target: a new category of a kind, or an existing one to edit. */
private data class CategoryEdit(val kind: CategoryKind, val category: CategoryEntity?)

/** Figma 12: expense categories and income sources, each editable or archivable. */
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val viewModel = sakuinViewModel { CategoriesViewModel(it.categoryRepository) }
    val expense by viewModel.expense.collectAsStateWithLifecycle()
    val income by viewModel.income.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(CategoryKind.EXPENSE) }
    var editing by remember { mutableStateOf<CategoryEdit?>(null) }
    val colors = SakuinTheme.colors
    val list = if (kind == CategoryKind.EXPENSE) expense else income

    Column(Modifier.fillMaxSize()) {
        SakuinTopBar(
            title = stringResource(R.string.title_categories),
            onBack = onBack,
            actions = {
                IconButton(onClick = { editing = CategoryEdit(kind, null) }) {
                    Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.category_add), tint = colors.textPrimary)
                }
            },
        )
        SegmentedControl(
            options = listOf(stringResource(R.string.tab_expense), stringResource(R.string.tab_income)),
            selectedIndex = if (kind == CategoryKind.EXPENSE) 0 else 1,
            onSelect = { kind = if (it == 0) CategoryKind.EXPENSE else CategoryKind.INCOME },
            modifier = Modifier.padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.sm),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
        ) {
            items(list, key = { it.id }) { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SakuinTheme.radius.lg))
                        .background(colors.bgSurface)
                        .clickable(role = Role.Button) { editing = CategoryEdit(category.kind, category) }
                        .padding(SakuinTheme.spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile(category.iconKey)
                    Text(category.name, modifier = Modifier.weight(1f), style = SakuinTheme.typography.bodyStrong, color = colors.textPrimary)
                    Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.action_edit), Modifier.size(18.dp), tint = colors.textSecondary)
                }
            }
        }
    }

    editing?.let { target ->
        CategorySheet(
            target = target,
            onSave = { name, icon ->
                viewModel.save(target.category, target.kind, name, icon)
                editing = null
            },
            onArchive = target.category?.let { category ->
                {
                    viewModel.archive(category.id)
                    editing = null
                }
            },
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategorySheet(
    target: CategoryEdit,
    onSave: (name: String, iconKey: String) -> Unit,
    onArchive: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = SakuinTheme.colors
    var name by remember { mutableStateOf(target.category?.name.orEmpty()) }
    var icon by remember { mutableStateOf(target.category?.iconKey ?: "tag") }
    var showError by remember { mutableStateOf(false) }
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
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg),
        ) {
            Text(
                stringResource(if (target.category == null) R.string.category_new else R.string.category_edit),
                style = SakuinTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            SakuinTextField(
                value = name,
                onValueChange = {
                    name = it.take(30)
                    showError = false
                },
                label = stringResource(R.string.field_name),
                leadingIcon = categoryIcon(icon),
                isError = showError,
                helper = if (showError) stringResource(R.string.error_name_required) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            )
            Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
                Text(stringResource(R.string.field_icon), style = SakuinTheme.typography.labelMedium, color = colors.textSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CategoryIconKeys.keys.forEach { key ->
                        val selected = key == icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(SakuinTheme.radius.md))
                                .background(if (selected) colors.primary else colors.bgSurfaceVariant)
                                .clickable(role = Role.RadioButton) { icon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(categoryIcon(key)),
                                contentDescription = key,
                                modifier = Modifier.size(22.dp),
                                tint = if (selected) colors.onPrimary else colors.textPrimary,
                            )
                        }
                    }
                }
            }
            SakuinButton(
                text = stringResource(R.string.category_save),
                onClick = { if (name.isBlank()) showError = true else onSave(name, icon) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (onArchive != null) {
                SakuinButton(
                    text = stringResource(R.string.category_archive),
                    onClick = onArchive,
                    style = ButtonStyle.Outline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun IconTile(iconKey: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(SakuinTheme.colors.bgSurfaceVariant, RoundedCornerShape(SakuinTheme.radius.md)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(categoryIcon(iconKey)), null, Modifier.size(20.dp), tint = SakuinTheme.colors.textPrimary)
    }
}
