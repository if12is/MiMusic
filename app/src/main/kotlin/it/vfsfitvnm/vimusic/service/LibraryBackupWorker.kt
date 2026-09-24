package it.vfsfitvnm.vimusic.service

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.internal
import it.vfsfitvnm.vimusic.path
import it.vfsfitvnm.vimusic.utils.PlaybackLogStore
import it.vfsfitvnm.vimusic.utils.backupTreeUriKey
import it.vfsfitvnm.vimusic.utils.newestBackupName
import it.vfsfitvnm.vimusic.utils.preferences
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class LibraryBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val tree = applicationContext.preferences.getString(backupTreeUriKey, null)?.toUri()
            ?: return Result.failure()
        return if (writeBackup(applicationContext, tree)) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "mimusic-library-backup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LibraryBackupWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

fun writeBackup(context: Context, treeUri: Uri): Boolean {
    return try {
        Database.checkpoint()
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val root = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        val stamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val created = DocumentsContract.createDocument(
            resolver,
            root,
            "application/vnd.sqlite3",
            "mimusic-$stamp.db"
        ) ?: return false
        val databaseFile = Database.internal.path ?: return false
        val copied = resolver.openOutputStream(created)?.use { output ->
            FileInputStream(databaseFile).use { input -> input.copyTo(output) }
        }
        if (copied == null) return false
        trimOldBackups(context, treeUri)
        true
    } catch (e: java.io.IOException) {
        PlaybackLogStore.append("backup failed ${e.message}")
        false
    } catch (e: SecurityException) {
        PlaybackLogStore.append("backup failed ${e.message}")
        false
    } catch (e: IllegalArgumentException) {
        PlaybackLogStore.append("backup failed ${e.message}")
        false
    } catch (e: IllegalStateException) {
        PlaybackLogStore.append("backup failed ${e.message}")
        false
    } catch (e: android.database.sqlite.SQLiteException) {
        PlaybackLogStore.append("backup failed ${e.message}")
        false
    }
}

fun newestBackupUri(context: Context, treeUri: Uri): Uri? {
    return try {
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootId)
        val names = mutableListOf<Pair<String, String>>()
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex) ?: continue
                val id = cursor.getString(idIndex) ?: continue
                names += name to id
            }
        }
        val newest = newestBackupName(names.map { it.first }) ?: return null
        val id = names.first { it.first == newest }.second
        DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
    } catch (e: java.io.IOException) {
        PlaybackLogStore.append("backup list failed ${e.message}")
        null
    } catch (e: SecurityException) {
        PlaybackLogStore.append("backup list failed ${e.message}")
        null
    } catch (e: IllegalArgumentException) {
        PlaybackLogStore.append("backup list failed ${e.message}")
        null
    } catch (e: IllegalStateException) {
        PlaybackLogStore.append("backup list failed ${e.message}")
        null
    }
}

private fun trimOldBackups(context: Context, treeUri: Uri) {
    val resolver = context.contentResolver
    val rootId = DocumentsContract.getTreeDocumentId(treeUri)
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootId)
    val files = mutableListOf<Pair<String, String>>()
    resolver.query(
        children,
        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID),
        null,
        null,
        null
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        while (cursor.moveToNext()) {
            val name = cursor.getString(nameIndex) ?: continue
            val id = cursor.getString(idIndex) ?: continue
            if (name.startsWith("mimusic-") && name.endsWith(".db")) files += name to id
        }
    }
    files.sortedBy { it.first }.dropLast(5).forEach { (_, id) ->
        DocumentsContract.deleteDocument(
            resolver,
            DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
        )
    }
}
