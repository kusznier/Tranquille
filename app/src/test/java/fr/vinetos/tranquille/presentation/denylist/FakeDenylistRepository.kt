package fr.vinetos.tranquille.presentation.denylist

import fr.vinetos.tranquille.data.DenylistImporterExporter.ImportResult
import fr.vinetos.tranquille.data.DenylistItem
import java.io.File
import java.io.InputStream

class FakeDenylistRepository(initial: List<DenylistItem> = emptyList()) : DenylistRepository {
    val items = initial.toMutableList()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1
    var importResult: ImportResult? = ImportResult(0, 0, 0)
    var exportOk = true
    val updates = mutableListOf<DenylistItem>()

    override fun getAll() = items.toList()
    override fun findById(id: Long) = items.firstOrNull { it.id == id }
    override fun findByNameAndPattern(name: String, pattern: String) =
        items.firstOrNull { it.name == name && it.pattern == pattern }

    override fun insert(item: DenylistItem) {
        items += item.copy(id = nextId++)
    }

    override fun update(item: DenylistItem) {
        updates += item
        items.replaceAll { if (it.id == item.id) item else it }
    }

    override fun delete(ids: Collection<Long>) {
        items.removeAll { it.id in ids }
    }

    override fun importFrom(input: InputStream) = importResult
    override fun exportTo(file: File) = exportOk
    override var blockDenylisted = true
}
