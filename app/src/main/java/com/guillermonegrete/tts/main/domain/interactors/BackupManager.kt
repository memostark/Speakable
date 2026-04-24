package com.guillermonegrete.tts.main.domain.interactors

import android.content.Context
import com.guillermonegrete.tts.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class BackupManager @Inject constructor(
    private val executor: ExecutorService,
    private val mainThread: MainThread,
    @param:ApplicationContext private val context: Context,
) {

    fun createBackup(fileSuccessCallback: FileCallback, errorCallback: ErrorCallback) {
        executor.execute {
            try {
                val zipFilePath = "backup_data.zip"
                val file = zipFiles(FILES_TO_BACKUP, zipFilePath)
                mainThread.post {
                    fileSuccessCallback.onBackupSuccess(file)
                }
            } catch (e: Exception) {
                mainThread.post { errorCallback.onError(e) }
            }
        }
    }

    fun zipFiles(databaseNames: List<String>, zipFilePath: String): File {
        val backupFile = File(
            context.getExternalFilesDir(null),
            zipFilePath
        )

        ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
            databaseNames.forEach { databaseName ->
                val file = context.getDatabasePath(databaseName)
                if (file.exists()) {
                    // Add entry and copy data
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        return backupFile
    }

    fun restoreDatabase(successCallback: SuccessCallback, errorCallback: ErrorCallback) {
        executor.execute {
            try {
                val zipFilePath = "backup_data.zip"
                val backupFile = File(
                    context.getExternalFilesDir(null),
                    zipFilePath
                )
                restoreDatabase(backupFile)
                mainThread.post { successCallback.onSuccess() }
            } catch (e: Exception) {
                mainThread.post { errorCallback.onError(e) }
            }
        }
    }

    private fun restoreDatabase(backupFile: File) {

        val databases = FILES_TO_BACKUP

        ZipFile(backupFile).use { zip ->
            // Iterate through all entries
            zip.entries().asSequence().forEach { entry ->
                // Read content of a specific entry if needed
                if (!entry.isDirectory && databases.contains(entry.name)) {
                    zip.getInputStream(entry).use { input ->
                        val dbFile = context.getDatabasePath(entry.name)
                        FileOutputStream(dbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    companion object {
        val FILES_TO_BACKUP = listOf("words.db", "words.db-shm", "words.db-wal", "files.db", "files.db-shm", "files.db-wal")
    }

    fun interface FileCallback {
        fun onBackupSuccess(outputFile: File)
    }

    fun interface SuccessCallback {
        fun onSuccess()
    }

    fun interface ErrorCallback {
        fun onError(t: Throwable)
    }
}
