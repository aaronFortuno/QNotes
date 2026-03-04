package com.aaronfortuno.studio.qnotes.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaronfortuno.studio.qnotes.data.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var database: VaultDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val id = dao.insert(Category(name = "Work"))

        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals("Work", result!!.name)
    }

    @Test
    fun getAllReturnsSortedBySortOrderThenName() = runTest {
        dao.insert(Category(name = "Zebra", sortOrder = 0))
        dao.insert(Category(name = "Alpha", sortOrder = 0))
        dao.insert(Category(name = "Middle", sortOrder = 1))

        val results = dao.getAll().first()
        assertEquals(3, results.size)
        assertEquals("Alpha", results[0].name)
        assertEquals("Zebra", results[1].name)
        assertEquals("Middle", results[2].name)
    }

    @Test
    fun updateModifiesCategory() = runTest {
        val id = dao.insert(Category(name = "Old", color = null, sortOrder = 0))
        val category = dao.getById(id)!!
        dao.update(category.copy(name = "New", color = 0xFFFF0000.toInt(), sortOrder = 5))

        val result = dao.getById(id)!!
        assertEquals("New", result.name)
        assertEquals(0xFFFF0000.toInt(), result.color)
        assertEquals(5, result.sortOrder)
    }

    @Test
    fun deleteRemovesCategory() = runTest {
        val id = dao.insert(Category(name = "ToDelete"))
        val category = dao.getById(id)!!
        dao.delete(category)

        assertNull(dao.getById(id))
    }

    @Test
    fun colorIsNullableAndPersisted() = runTest {
        val idNoColor = dao.insert(Category(name = "NoColor"))
        val idWithColor = dao.insert(Category(name = "WithColor", color = 0xFF00FF00.toInt()))

        assertNull(dao.getById(idNoColor)!!.color)
        assertEquals(0xFF00FF00.toInt(), dao.getById(idWithColor)!!.color)
    }

    @Test
    fun sortOrderDefaultsToZero() = runTest {
        val id = dao.insert(Category(name = "Default"))

        val result = dao.getById(id)!!
        assertEquals(0, result.sortOrder)
    }
}
