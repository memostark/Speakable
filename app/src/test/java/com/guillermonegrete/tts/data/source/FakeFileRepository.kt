package com.guillermonegrete.tts.data.source

import androidx.annotation.VisibleForTesting
import com.guillermonegrete.tts.db.BookFile
import java.util.LinkedHashMap

class FakeFileRepository: FileRepository {

    var filesServiceData: LinkedHashMap<Int, BookFile> = LinkedHashMap()

    override suspend fun getRecentFiles(): List<BookFile> {
        return filesServiceData.values.toList()
    }

    override suspend fun getFile(id: Int): BookFile? {
        return filesServiceData[id]
    }

    override suspend fun saveFile(file: BookFile) {
        filesServiceData[file.id] = file
    }

    @VisibleForTesting
    fun addTasks(vararg files: BookFile) {
        for (file in files) {
            filesServiceData[file.id] = file
        }
    }
}