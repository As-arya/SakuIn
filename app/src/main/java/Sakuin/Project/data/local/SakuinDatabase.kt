package Sakuin.Project.data.local

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import Sakuin.Project.data.local.dao.CategoryDao
import Sakuin.Project.data.local.dao.TransactionDao
import Sakuin.Project.data.local.dao.WalletDao
import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.TransactionEntity
import Sakuin.Project.data.local.entity.WalletEntity

@Database(
    entities = [WalletEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 2,
    exportSchema = true,
    // 2: transfer admin fee (transactions.fee)
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class SakuinDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val NAME = "sakuin.db"

        fun build(context: Context): SakuinDatabase =
            Room.databaseBuilder<SakuinDatabase>(context.applicationContext, NAME)
                .setDriver(BundledSQLiteDriver())
                .build()
    }
}
