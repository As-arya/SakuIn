package Sakuin.Project.data.local

import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.CategoryKind

/** Categories inserted on first launch; users can add, rename or archive them later. */
object DefaultCategories {
    private val expense = listOf(
        "Food" to "utensils",
        "Transport" to "bus",
        "Education" to "graduation",
        "Shopping" to "bag",
        "Fun" to "gamepad",
        "Health" to "health",
        "Bills" to "bill",
        "Others" to "more",
    )

    private val income = listOf(
        "Salary" to "briefcase",
        "Allowance" to "coins",
        "THR" to "gift",
        "Bonus" to "sparkles",
        "Gift" to "gift",
        "Others" to "more",
    )

    val all: List<CategoryEntity> =
        expense.toEntities(CategoryKind.EXPENSE) + income.toEntities(CategoryKind.INCOME)

    private fun List<Pair<String, String>>.toEntities(kind: CategoryKind) =
        mapIndexed { index, (name, icon) ->
            CategoryEntity(name = name, kind = kind, iconKey = icon, isDefault = true, sortOrder = index)
        }
}
