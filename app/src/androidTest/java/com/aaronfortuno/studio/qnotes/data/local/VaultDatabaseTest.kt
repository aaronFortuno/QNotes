package com.aaronfortuno.studio.qnotes.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaronfortuno.studio.qnotes.data.model.Category
import com.aaronfortuno.studio.qnotes.data.model.ItemType
import com.aaronfortuno.studio.qnotes.data.model.VaultItem
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultDatabaseTest {

    private lateinit var database: VaultDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deletingCategorySetsItemCategoryIdToNull() = runTest {
        val categoryId = database.categoryDao().insert(Category(name = "Work"))
        val itemId = database.vaultItemDao().insert(
            VaultItem(title = "Note", content = "", type = ItemType.NOTE, categoryId = categoryId)
        )

        val category = database.categoryDao().getById(categoryId)!!
        database.categoryDao().delete(category)

        val item = database.vaultItemDao().getById(itemId)!!
        assertNull(item.categoryId)
    }
}
