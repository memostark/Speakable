package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.db.BookFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFileRepository @Inject constructor(): FileRepository{
    override fun getRecentFiles(): List<BookFile> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveFile(file: BookFile) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}