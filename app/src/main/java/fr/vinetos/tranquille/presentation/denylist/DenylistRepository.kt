package fr.vinetos.tranquille.presentation.denylist

import fr.vinetos.tranquille.App
import fr.vinetos.tranquille.data.DenylistImporterExporter
import fr.vinetos.tranquille.data.DenylistImporterExporter.ImportResult
import fr.vinetos.tranquille.data.DenylistItem
import fr.vinetos.tranquille.data.YacbHolder
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream

/**
 * What the denylist screens need from the data layer. Blocking-related
 * calls go through the existing DenylistService and DenylistDao unchanged.
 * All functions block and must be called off the main thread.
 */
interface DenylistRepository {
    fun getAll(): List<DenylistItem>
    fun findById(id: Long): DenylistItem?
    fun findByNameAndPattern(name: String, pattern: String): DenylistItem?
    fun insert(item: DenylistItem)
    fun update(item: DenylistItem)
    fun delete(ids: Collection<Long>)
    fun importFrom(input: InputStream): ImportResult?
    fun exportTo(file: File): Boolean
    var blockDenylisted: Boolean
}

/** Production implementation backed by YacbHolder and the app settings. */
object AppDenylistRepository : DenylistRepository {
    private val dao get() = YacbHolder.getDenylistDao()
    private val service get() = YacbHolder.getDenylistService()

    override fun getAll(): List<DenylistItem> = dao.getAll()

    override fun findById(id: Long): DenylistItem? =
        kotlinx.coroutines.runBlocking { dao.findById(id) }

    override fun findByNameAndPattern(name: String, pattern: String): DenylistItem? =
        kotlinx.coroutines.runBlocking { dao.findByNameAndPattern(name, pattern) }

    override fun insert(item: DenylistItem) {
        service.insert(item)
    }

    override fun update(item: DenylistItem) {
        service.update(item)
    }

    override fun delete(ids: Collection<Long>) {
        service.delete(ids)
    }

    override fun importFrom(input: InputStream): ImportResult? =
        DenylistImporterExporter().importDenylist(dao, service, input)

    override fun exportTo(file: File): Boolean = try {
        FileWriter(file).use { DenylistImporterExporter().writeBackup(dao.getAll(), it) }
    } catch (e: IOException) {
        false
    }

    override var blockDenylisted: Boolean
        get() = App.getSettings().blockDenylisted
        set(value) {
            App.getSettings().blockDenylisted = value
        }
}
