package com.guillermonegrete.tts.importtext

import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.provider.DocumentsContract
import java.io.File

/**
 * Validates android Uri for both file and content types. Based from: https://stackoverflow.com/a/50143855/10244759
 */
class UriValidator {

    fun isLoadable(context: Context, uri: Uri): Boolean {

        return when(uri.scheme) {
            "content" -> {
                if (DocumentsContract.isDocumentUri(context, uri))
                    documentUriExists(context, uri)
                else // Content URI is not from a document provider
                    contentUriExists(context, uri)
            }

            "file" -> {
                val path = uri.path
                path != null && File(path).exists()
            }

            // http, https, etc. No inexpensive way to test existence.
            else -> false
        }
    }

    // All DocumentProviders should support the COLUMN_DOCUMENT_ID column
    private fun documentUriExists(context: Context, uri: Uri): Boolean =
        resolveUri(context, uri, DocumentsContract.Document.COLUMN_DOCUMENT_ID)

    // All ContentProviders should support the BaseColumns._ID column
    private fun contentUriExists(context: Context, uri: Uri): Boolean =
        resolveUri(context, uri, BaseColumns._ID)

    private fun resolveUri(context: Context, uri: Uri, column: String): Boolean {

        val cursor = context.contentResolver.query(uri,
            arrayOf(column), // Empty projections are bad for performance
            null,
            null,
            null)

        val result = cursor?.moveToFirst() ?: false

        cursor?.close()

        return result
    }
}