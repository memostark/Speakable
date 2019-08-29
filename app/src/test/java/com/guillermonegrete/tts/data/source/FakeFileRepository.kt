package com.guillermonegrete.tts.data.source

import androidx.annotation.VisibleForTesting
import com.guillermonegrete.tts.db.BookFile
import java.util.LinkedHashMap

class FakeFileRepository: FileRepository {

    var tasksServiceData: LinkedHashMap<Int, BookFile> = LinkedHashMap()

    override suspend fun getRecentFiles(): List<BookFile> {
        return tasksServiceData.values.toList()
    }

    override suspend fun saveFile(file: BookFile) {

    }

    @VisibleForTesting
    fun addTasks(vararg files: BookFile) {
        for (file in files) {
            tasksServiceData[file.id] = file
        }
    }
}