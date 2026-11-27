package fr.vinetos.tranquille.data.datasource

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import fr.vinetos.tranquille.data.Database
import fr.vinetos.tranquille.data.DenylistItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.ConscryptMode
import java.util.Date

// Runs under Robolectric like DenylistImportIntegrationTest: the SQLite JDBC driver
// registered inside the Robolectric sandbox is not visible to plain JVM tests in the
// same process, so all database tests share the sandbox.
@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class DenylistDaoTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var dao: DenylistDao

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        dao = DenylistDao(
            Database(driver, DenylistItem.Adapter(DateColumnAdapter, DateColumnAdapter))
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun newItem(name: String, pattern: String) = DenylistItem(
        id = -1L,
        name = name,
        pattern = pattern,
        creationDate = Date(1_000L),
        invalid = false,
        numberOfCalls = 0,
        lastCallDate = null
    )

    @Test
    fun insertingTwoNewItemsKeepsBoth() = runBlocking {
        dao.insert(newItem("first", "+99900000001"))
        dao.insert(newItem("second", "+99900000002"))

        assertEquals(2, dao.countAll())
        assertNotNull(dao.findByPattern("+99900000001"))
        assertNotNull(dao.findByPattern("+99900000002"))
    }

    @Test
    fun insertedItemGetsPositiveId() = runBlocking {
        dao.insert(newItem("first", "+99900000001"))

        val stored = dao.findByPattern("+99900000001")
        assertNotNull(stored)
        assert(stored!!.id > 0) { "expected an autoincrement id, got ${stored.id}" }
    }

    @Test
    fun updateReplacesExistingRow() = runBlocking {
        dao.insert(newItem("first", "+99900000001"))
        val stored = dao.findByPattern("+99900000001")!!

        dao.update(stored.copy(name = "renamed"))

        assertEquals(1, dao.countAll())
        assertEquals("renamed", dao.findById(stored.id)!!.name)
    }

    @Test
    fun getFirstMatchUsesLikePatterns() = runBlocking {
        dao.insert(newItem("prefix", "+999000%"))

        assertNotNull(dao.getFirstMatch("+99900012345"))
        assertNull(dao.getFirstMatch("+99800012345"))
    }
}
