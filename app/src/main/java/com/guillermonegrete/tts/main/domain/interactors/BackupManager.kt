package com.guillermonegrete.tts.main.domain.interactors

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.guillermonegrete.tts.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class BackupManager @Inject constructor(
    private val executor: ExecutorService,
    private val mainThread: MainThread,
    @param:ApplicationContext private val context: Context,
) {

    fun createBackup(directoryUri: Uri, fileSuccessCallback: FileCallback, errorCallback: ErrorCallback) {
        executor.execute {
            try {
                val resolver = context.contentResolver
                val directory = DocumentFile.fromTreeUri(context, directoryUri)
                    ?: throw FileNotFoundException("Couldn't find folder for uri: $directoryUri")

                val zipFilePath = "backup_data.zip"
                val zipFile = directory.createFile("application/zip", zipFilePath)
                    ?: throw FileNotFoundException("Couldn't create file at folder: $directoryUri")
                resolver.openOutputStream(zipFile.uri)?.use { outputStream ->
                    zipFiles(FILES_TO_BACKUP, outputStream)
                    mainThread.post {
                        fileSuccessCallback.onBackupSuccess(zipFile.uri)
                    }
                }
            } catch (e: Exception) {
                mainThread.post { errorCallback.onError(e) }
            }
        }
    }

    fun zipFiles(databaseNames: List<String>, outputStream: OutputStream) {

        ZipOutputStream(outputStream).use { zos ->
            databaseNames.forEach { databaseName ->
                val file = context.getDatabasePath(databaseName)
                if (file.exists()) {
                    // Add entry and copy data
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            val prefName = context.packageName + "_preferences"
            val sharedPrefFile =
                File(context.filesDir?.parent + "/shared_prefs/" + prefName + ".xml")
            if (sharedPrefFile.exists()) {
                // Add entry and copy data
                zos.putNextEntry(ZipEntry(sharedPrefFile.name))
                sharedPrefFile.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    fun restoreDatabase(uri: Uri, successCallback: SuccessCallback, errorCallback: ErrorCallback) {
        executor.execute {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw FileNotFoundException("Couldn't open file ofr uri: $uri")
                restoreDatabase(inputStream)
                mainThread.post { successCallback.onSuccess() }
            } catch (e: Exception) {
                mainThread.post { errorCallback.onError(e) }
            }
        }
    }

    private fun restoreDatabase(inputStream: InputStream) {
        val databases = FILES_TO_BACKUP

        val zipInputStream = ZipInputStream(inputStream)
        var entry: ZipEntry? = zipInputStream.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && databases.contains(entry.name)) {
                val dbFile = context.getDatabasePath(entry.name)
                FileOutputStream(dbFile).use { fos ->
                    val buffer = ByteArray(4096)
                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                }
            }

            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
    }

    companion object {
        val FILES_TO_BACKUP = listOf("words.db", "words.db-shm", "words.db-wal", "files.db", "files.db-shm", "files.db-wal")
    }

    fun interface FileCallback {
        fun onBackupSuccess(outputUri: Uri)
    }

    fun interface SuccessCallback {
        fun onSuccess()
    }

    fun interface ErrorCallback {
        fun onError(t: Throwable)
    }
}
