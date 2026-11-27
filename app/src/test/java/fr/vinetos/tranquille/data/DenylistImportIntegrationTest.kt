package fr.vinetos.tranquille.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import fr.vinetos.tranquille.data.DenylistImporterExporter.ImportResult
import fr.vinetos.tranquille.data.datasource.DateColumnAdapter
import fr.vinetos.tranquille.data.datasource.DenylistDao
import fr.vinetos.tranquille.domain.service.DenylistService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.ConscryptMode
import java.util.Date

/** Import of a YACB backup through the real DAO and DenylistService (fictional +999 numbers). */
@ConscryptMode(ConscryptMode.Mode.OFF)
@RunWith(AndroidJUnit4::class)
class DenylistImportIntegrationTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var dao: DenylistDao
    private lateinit var service: DenylistService
    private val importer = DenylistImporterExporter()

    private val backup = """
        ID,name,pattern,creationTimestamp,numberOfCalls,lastCallTimestamp
        3,Spam one,+99900000001,1600000000000,2,1600000100000
        7,,+999000*,1600000200000,0,
        9,Single digit,+9990000000#,1600000300000,,
    """.trimIndent() + "\n"

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        dao = DenylistDao(Database(driver, DenylistItem.Adapter(DateColumnAdapter, DateColumnAdapter)))
        service = DenylistService({ }, dao)
    }

    @After
    fun tearDown() = driver.close()

    private fun import() = importer.importDenylist(dao, service, backup.byteInputStream())

    @Test
    fun importingTwiceAddsNothingTheSecondTime() {
        assertEquals(ImportResult(added = 3, updated = 0, skipped = 0), import())
        assertEquals(ImportResult(added = 0, updated = 0, skipped = 3), import())
        assertEquals(3, dao.countAll())
    }

    @Test
    fun existingItemWithSameIdIsNotOverwritten() {
        // An unrelated item that happens to get id 3, like "Spam one" in the backup.
        service.insert("Mine 1", "+99911111111")
        service.insert("Mine 2", "+99922222222")
        service.insert("Mine 3", "+99933333333")
        assertEquals("Mine 3", runBlocking { dao.findById(3) }?.name)

        import()

        assertEquals(6, dao.countAll())
        assertEquals("+99933333333", runBlocking { dao.findByPattern("+99933333333") }?.pattern)
        assertEquals(2L, runBlocking { dao.findByPattern("+99900000001") }?.numberOfCalls)
    }

    @Test
    fun existingItemGetsMissingNameAndNewerStats() {
        service.insert(DenylistItem(-1, "", "+99900000001", Date(1), false, 1, Date(1)))

        val result = import()

        assertEquals(ImportResult(added = 2, updated = 1, skipped = 0), result)
        val merged = runBlocking { dao.findByPattern("+99900000001") }!!
        assertEquals("Spam one", merged.name)
        assertEquals(2L, merged.numberOfCalls)
        assertEquals(Date(1600000100000), merged.lastCallDate)
    }
}
