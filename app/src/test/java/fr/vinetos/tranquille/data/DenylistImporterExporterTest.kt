package fr.vinetos.tranquille.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Uses only fictional numbers (country code +999 is unassigned).
 */
class DenylistImporterExporterTest {

    private val importer = DenylistImporterExporter()

    // Same layout as a YACB "blacklist" CSV backup.
    private val yacbBackup = """
        ID,name,pattern,creationTimestamp,numberOfCalls,lastCallTimestamp
        3,Spam one,+99900000001,1600000000000,2,1600000100000
        7,,+999000*,1600000200000,0,
        9,Single digit,+9990000000#,1600000300000,,
    """.trimIndent() + "\n"

    @Test
    fun readsYacbBackup() {
        val items = importer.read(yacbBackup.byteInputStream())
        assertNotNull(items)
        assertEquals(3, items!!.size)

        val first = items[0]
        assertEquals(3L, first.id)
        assertEquals("Spam one", first.name)
        assertEquals("+99900000001", first.pattern)
        assertEquals(Date(1600000000000), first.creationDate)
        assertEquals(2L, first.numberOfCalls)
        assertEquals(Date(1600000100000), first.lastCallDate)
        assertFalse(first.invalid)

        // human-readable wildcards are converted to SQL LIKE wildcards
        assertEquals("+999000%", items[1].pattern)
        assertEquals("", items[1].name)
        assertNull(items[1].lastCallDate)
        assertEquals("+9990000000_", items[2].pattern)
        assertEquals(0L, items[2].numberOfCalls)
    }

    @Test
    fun readsYacbBackupWithoutHeader() {
        val noHeader = yacbBackup.lines().drop(1).joinToString("\n")
        val items = importer.read(noHeader.byteInputStream())
        assertEquals(3, items!!.size)
    }

    @Test
    fun exportThenImportRoundTrip() {
        val original = listOf(
            DenylistItem(5, "Name, with comma", "+999123%", Date(1000), false, 4, Date(2000)),
            DenylistItem(6, null, "+99912345678_", Date(3000), false, 0, null),
        )
        val out = StringBuilder()
        assertTrue(importer.writeBackup(original, out))

        val items = importer.read(out.toString().byteInputStream())!!
        assertEquals(2, items.size)
        assertEquals(original[0], items[0])
        assertEquals("+99912345678_", items[1].pattern)
        assertNull(items[1].lastCallDate)
    }

    @Test
    fun readsNoPhoneSpamBackup() {
        val backup = "+99900000001: Spam one\n+999000*: Prefix\n"
        val items = importer.read(backup.byteInputStream())
        assertNotNull(items)
        assertEquals(2, items!!.size)
        assertEquals("+999000%", items[1].pattern)
    }
}
