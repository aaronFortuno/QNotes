package com.aaronfortuno.studio.qnotes.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaronfortuno.studio.qnotes.data.model.Category
import com.aaronfortuno.studio.qnotes.data.model.ItemType
import com.aaronfortuno.studio.qnotes.data.model.VaultItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultItemDaoTest {

    private lateinit var database: VaultDatabase
    private lateinit var dao: VaultItemDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.vaultItemDao()
        categoryDao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val item = VaultItem(title = "Test", content = "Content", type = ItemType.NOTE)
        val id = dao.insert(item)

        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals("Test", result!!.title)
        assertEquals("Content", result.content)
        assertEquals(ItemType.NOTE, result.type)
    }

    @Test
    fun getAllReturnsItemsOrderedByUpdatedAtDesc() = runTest {
        val older = VaultItem(title = "Old", content = "", type = ItemType.NOTE, updatedAt = 1000)
        val newer = VaultItem(title = "New", content = "", type = ItemType.NOTE, updatedAt = 2000)
        dao.insert(older)
        dao.insert(newer)

        val results = dao.getAll().first()
        assertEquals(2, results.size)
        assertEquals("New", results[0].title)
        assertEquals("Old", results[1].title)
    }

    @Test
    fun updateModifiesItem() = runTest {
        val id = dao.insert(VaultItem(title = "Original", content = "A", type = ItemType.NOTE))
        val item = dao.getById(id)!!
        dao.update(item.copy(title = "Updated", content = "B"))

        val result = dao.getById(id)!!
        assertEquals("Updated", result.title)
        assertEquals("B", result.content)
    }

    @Test
    fun deleteRemovesItem() = runTest {
        val id = dao.insert(VaultItem(title = "ToDelete", content = "", type = ItemType.NOTE))
        val item = dao.getById(id)!!
        dao.delete(item)

        assertNull(dao.getById(id))
    }

    @Test
    fun deleteByIdRemovesItem() = runTest {
        val id = dao.insert(VaultItem(title = "ToDelete", content = "", type = ItemType.NOTE))
        dao.deleteById(id)

        assertNull(dao.getById(id))
    }

    @Test
    fun upsertInsertsNewItem() = runTest {
        val id = dao.upsert(VaultItem(title = "New", content = "C", type = ItemType.CLIPBOARD))

        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals("New", result!!.title)
        assertEquals(ItemType.CLIPBOARD, result.type)
    }

    @Test
    fun upsertReplacesExistingItem() = runTest {
        val id = dao.insert(VaultItem(title = "V1", content = "A", type = ItemType.NOTE))
        dao.upsert(VaultItem(id = id, title = "V2", content = "B", type = ItemType.NOTE))

        val result = dao.getById(id)!!
        assertEquals("V2", result.title)
        assertEquals("B", result.content)
    }

    @Test
    fun getByCategoryFiltersCorrectly() = runTest {
        val catA = categoryDao.insert(Category(name = "Work"))
        val catB = categoryDao.insert(Category(name = "Personal"))
        dao.insert(VaultItem(title = "Cat1", content = "", type = ItemType.NOTE, categoryId = catA))
        dao.insert(VaultItem(title = "Cat2", content = "", type = ItemType.NOTE, categoryId = catB))
        dao.insert(VaultItem(title = "Cat1b", content = "", type = ItemType.NOTE, categoryId = catA))

        val results = dao.getByCategory(catA).first()
        assertEquals(2, results.size)
        assertTrue(results.all { it.categoryId == catA })
    }

    @Test
    fun searchFindsInTitle() = runTest {
        dao.insert(VaultItem(title = "Kotlin tips", content = "", type = ItemType.NOTE))
        dao.insert(VaultItem(title = "Java guide", content = "", type = ItemType.NOTE))

        val results = dao.search("Kotlin").first()
        assertEquals(1, results.size)
        assertEquals("Kotlin tips", results[0].title)
    }

    @Test
    fun searchFindsInContent() = runTest {
        dao.insert(VaultItem(title = "Note", content = "Learn Kotlin basics", type = ItemType.NOTE))
        dao.insert(VaultItem(title = "Note2", content = "Learn Java basics", type = ItemType.NOTE))

        val results = dao.search("Kotlin").first()
        assertEquals(1, results.size)
    }

    @Test
    fun searchFindsInTags() = runTest {
        dao.insert(VaultItem(title = "Note", content = "", type = ItemType.NOTE, tags = "kotlin,android"))
        dao.insert(VaultItem(title = "Note2", content = "", type = ItemType.NOTE, tags = "java,spring"))

        val results = dao.search("android").first()
        assertEquals(1, results.size)
    }

    @Test
    fun searchIsCaseInsensitive() = runTest {
        dao.insert(VaultItem(title = "Kotlin Tips", content = "", type = ItemType.NOTE))

        val results = dao.search("kotlin").first()
        assertEquals(1, results.size)
    }

    @Test
    fun searchReturnsEmptyForNoMatch() = runTest {
        dao.insert(VaultItem(title = "Note", content = "Content", type = ItemType.NOTE))

        val results = dao.search("nonexistent").first()
        assertTrue(results.isEmpty())
    }

    @Test
    fun imagePathIsStoredCorrectly() = runTest {
        val id = dao.insert(
            VaultItem(
                title = "Screenshot",
                content = "",
                type = ItemType.IMAGE,
                imagePath = "img_123.png"
            )
        )

        val result = dao.getById(id)!!
        assertEquals("img_123.png", result.imagePath)
    }

    @Test
    fun allItemTypesAreSupported() = runTest {
        for (type in ItemType.entries) {
            val id = dao.insert(VaultItem(title = type.name, content = "", type = type))
            val result = dao.getById(id)!!
            assertEquals(type, result.type)
        }
    }
}
