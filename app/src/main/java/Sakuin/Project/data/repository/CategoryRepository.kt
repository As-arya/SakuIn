package Sakuin.Project.data.repository

import Sakuin.Project.data.local.DefaultCategories
import Sakuin.Project.data.local.dao.CategoryDao
import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.CategoryKind
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun observeCategories(kind: CategoryKind): Flow<List<CategoryEntity>> = categoryDao.observeByKind(kind)

    /** Seeds the default categories the first time the app runs. Safe to call repeatedly. */
    suspend fun ensureDefaults() {
        if (categoryDao.count() == 0) categoryDao.insertAll(DefaultCategories.all)
    }

    suspend fun addCategory(name: String, kind: CategoryKind, iconKey: String): Long {
        require(name.isNotBlank()) { "Category name is required" }
        return categoryDao.insert(
            CategoryEntity(
                name = name.trim(),
                kind = kind,
                iconKey = iconKey,
                sortOrder = categoryDao.maxSortOrder(kind) + 1,
            )
        )
    }

    suspend fun updateCategory(id: Long, name: String, iconKey: String) {
        require(name.isNotBlank()) { "Category name is required" }
        val current = requireNotNull(categoryDao.getById(id)) { "Category $id not found" }
        categoryDao.update(current.copy(name = name.trim(), iconKey = iconKey))
    }

    /** Hides the category from pickers; existing transactions keep pointing at it. */
    suspend fun archiveCategory(id: Long) {
        val current = categoryDao.getById(id) ?: return
        categoryDao.update(current.copy(isArchived = true))
    }
}
