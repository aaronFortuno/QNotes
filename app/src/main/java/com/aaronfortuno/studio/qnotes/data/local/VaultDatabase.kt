package com.aaronfortuno.studio.qnotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aaronfortuno.studio.qnotes.data.model.Category
import com.aaronfortuno.studio.qnotes.data.model.VaultItem

@Database(
    entities = [VaultItem::class, Category::class],
    version = 1,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun categoryDao(): CategoryDao
}
