package Sakuin.Project.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.CategoryKind
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM categories WHERE kind = :kind")
    suspend fun maxSortOrder(kind: CategoryKind): Int

    @Query(
        """
        SELECT * FROM categories
        WHERE kind = :kind AND isArchived = 0
        ORDER BY sortOrder, name
        """
    )
    fun observeByKind(kind: CategoryKind): Flow<List<CategoryEntity>>
}
