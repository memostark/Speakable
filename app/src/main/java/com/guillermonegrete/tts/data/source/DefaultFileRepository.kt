package com.guillermonegrete.tts.data.source

import com.guillermonegrete.tts.db.BookFile
import com.guillermonegrete.tts.db.FileDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFileRepository @Inject constructor(private val fileDAO: FileDAO): FileRepository{

    override suspend fun getFiles(): List<BookFile> {
        return withContext(Dispatchers.IO){
            return@withContext fileDAO.getFiles()
        }
    }

    override fun getRecentFiles(): Flow<List<BookFile>> = fileDAO.getRecentFiles()

    override suspend fun getFile(id: Int): BookFile? {
        return withContext(Dispatchers.IO){
            return@withContext fileDAO.getFile(id)
        }
    }

    override suspend fun getFile(uri: String): BookFile? {
        return withContext(Dispatchers.IO){
            return@withContext fileDAO.getFile(uri)
        }
    }

    override suspend fun saveFile(file: BookFile): Long {
        return withContext(Dispatchers.IO){
            fileDAO.upsert(file)
        }
    }

    override suspend fun deleteFile(file: BookFile) {
        withContext(Dispatchers.IO){
            fileDAO.delete(file)
        }
    }
}