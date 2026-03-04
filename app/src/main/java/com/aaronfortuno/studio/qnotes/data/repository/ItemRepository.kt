package com.aaronfortuno.studio.qnotes.data.repository

import com.aaronfortuno.studio.qnotes.data.local.VaultItemDao
import com.aaronfortuno.studio.qnotes.data.model.VaultItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val vaultItemDao: VaultItemDao
) {
    fun getAll(): Flow<List<VaultItem>> = vaultItemDao.getAll()

    suspend fun getById(id: Long): VaultItem? = vaultItemDao.getById(id)

    suspend fun insert(item: VaultItem): Long = vaultItemDao.insert(item)

    suspend fun update(item: VaultItem) = vaultItemDao.update(item)

    suspend fun delete(item: VaultItem) = vaultItemDao.delete(item)
}
