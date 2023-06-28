package com.guillermonegrete.tts.data.source

import androidx.annotation.VisibleForTesting
import com.guillermonegrete.tts.db.BookFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.LinkedHashMap

class FakeFileRepository: FileRepository {

    var filesServiceData: LinkedHashMap<Int, BookFile> = LinkedHashMap()

    var files = filesServiceData.values

    override suspend fun getFiles(): List<BookFile> {
        return filesServiceData.values.toList()
    }

    override fun getRecentFiles(): Flow<List<BookFile>> {
        return flowOf(filesServiceData.values.toList().sortedByDescending { it.lastRead })
    }

    override suspend fun getFile(id: Int): BookFile? {
        return filesServiceData[id]
    }

    override suspend fun getFile(uri: String): BookFile? {
        return filesServiceData.filter { it.value.uri == uri }.values.firstOrNull()
    }

    override suspend fun saveFile(file: BookFile): Long {
        filesServiceData[file.id] = file
        return file.id.toLong()
    }

    override suspend fun deleteFile(file: BookFile) {
        filesServiceData.remove(file.id)
    }

    @VisibleForTesting
    fun addFiles(vararg files: BookFile) {
        for (file in files) {
            filesServiceData[file.id] = file
        }
    }
}