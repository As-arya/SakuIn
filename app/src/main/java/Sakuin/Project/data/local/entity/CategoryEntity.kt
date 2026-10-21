package Sakuin.Project.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** [EXPENSE] categories are "what it was for"; [INCOME] categories are "where it came from". */
enum class CategoryKind { EXPENSE, INCOME }

@Entity(
    tableName = "categories",
    indices = [Index("kind")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    /** Key of the icon drawn for this category, e.g. "utensils" (matches the Figma icon names). */
    val iconKey: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
)
