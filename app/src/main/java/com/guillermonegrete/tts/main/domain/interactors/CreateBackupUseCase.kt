package com.guillermonegrete.tts.main.domain.interactors

import android.content.Context
import com.guillermonegrete.tts.AbstractInteractor
import com.guillermonegrete.tts.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class CreateBackupUseCase @Inject constructor(
    executor: ExecutorService,
    mainThread: MainThread,
    @param:ApplicationContext private val context: Context,
) : AbstractInteractor(executor, mainThread){

    var callback: Callback? = null

    operator fun invoke(callback: Callback) {
        this.callback = callback
        execute()
    }

    override fun run() {
        val zipFilePath = "backup_data.zip"
        val databases = listOf("words.db", "files.db")
        val file = zipFiles(databases, zipFilePath)
        mMainThread.post {
            callback?.onBackupSuccess(file)
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

    fun restoreDatabase(onSuccess: () -> Unit, onError: (t: Throwable) -> Unit) {
        executorService.execute {
            try {
                val zipFilePath = "backup_data.zip"
                val backupFile = File(
                    context.getExternalFilesDir(null),
                    zipFilePath
                )
                restoreDatabase(backupFile)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun restoreDatabase(backupFile: File) {

        val databases = listOf("words.db", "files.db")

        ZipFile(backupFile).use { zip ->
            // Iterate through all entries
            zip.entries().asSequence().forEach { entry ->
                println("Entry name: ${entry.name}, Size: ${entry.size}")

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

    interface Callback{
        fun onBackupSuccess(outputFile: File)

        fun onError(t: Throwable)
    }
}
