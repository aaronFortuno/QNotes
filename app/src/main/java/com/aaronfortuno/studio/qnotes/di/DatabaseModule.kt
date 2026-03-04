package com.aaronfortuno.studio.qnotes.di

import android.content.Context
import androidx.room.Room
import com.aaronfortuno.studio.qnotes.data.local.CategoryDao
import com.aaronfortuno.studio.qnotes.data.local.VaultDatabase
import com.aaronfortuno.studio.qnotes.data.local.VaultItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VaultDatabase {
        return Room.databaseBuilder(
            context,
            VaultDatabase::class.java,
            "vault_database"
        ).build()
    }

    @Provides
    fun provideVaultItemDao(database: VaultDatabase): VaultItemDao {
        return database.vaultItemDao()
    }

    @Provides
    fun provideCategoryDao(database: VaultDatabase): CategoryDao {
        return database.categoryDao()
    }
}
