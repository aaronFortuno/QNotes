package com.aaronfortuno.studio.qnotes.data.repository

import com.aaronfortuno.studio.qnotes.data.local.VaultItemDao
import com.aaronfortuno.studio.qnotes.data.model.VaultItem
import com.aaronfortuno.studio.qnotes.data.storage.FileStorage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val vaultItemDao: VaultItemDao,
    private val fileStorage: FileStorage
) {
    fun getAll(): Flow<List<VaultItem>> = vaultItemDao.getAll()

    suspend fun getById(id: Long): VaultItem? = vaultItemDao.getById(id)

    fun getByCategory(categoryId: Long): Flow<List<VaultItem>> =
        vaultItemDao.getByCategory(categoryId)

    fun search(query: String): Flow<List<VaultItem>> = vaultItemDao.search(query)

    suspend fun save(item: VaultItem, imageBytes: ByteArray? = null): Long {
        val finalItem = if (imageBytes != null) {
            val fileName = "img_${System.currentTimeMillis()}.png"
            fileStorage.saveImage(fileName, imageBytes)
            item.copy(imagePath = fileName)
        } else {
            item
        }
        return vaultItemDao.upsert(finalItem)
    }

    suspend fun delete(item: VaultItem) {
        item.imagePath?.let { fileStorage.deleteImage(it) }
        vaultItemDao.delete(item)
    }

    suspend fun deleteById(id: Long) {
        val item = vaultItemDao.getById(id)
        if (item != null) {
            delete(item)
        }
    }
}
