package com.notestaking.app.platform

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import com.notestaking.app.R
import java.io.File
import java.io.FileNotFoundException

/**
 * Exposes an app-owned directory as a SAF tree, so the in-app explorer can browse "Internal
 * storage" with the very same DocumentsContract calls it uses for a user-granted folder. Document
 * ids are paths relative to the root ("root", "root/sub", "root/sub/note.xnote"), so they stay
 * valid even if the backing directory ever moves. The framework requires a DocumentsProvider to
 * be exported and MANAGE_DOCUMENTS-protected; with no DOCUMENTS_PROVIDER intent filter it stays
 * out of the system file picker, so only xnotes reaches it. Its files go on app uninstall.
 */
class AppStorageDocumentsProvider : DocumentsProvider() {

    private val baseDir: File get() = rootDir(context!!)
    private val safeTree: SafeDocumentTree get() = SafeDocumentTree(baseDir)

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor =
        MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)

    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        includeFile(cursor, documentId, fileFor(documentId))
        return cursor
    }

    override fun queryChildDocuments(parentDocumentId: String, projection: Array<String>?, sortOrder: String?): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val tree = safeTree
        tree.children(parentDocumentId).forEach { child -> includeFile(cursor, tree.documentId(child), child) }
        return cursor
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean =
        safeTree.isChild(parentDocumentId, documentId)

    override fun findDocumentPath(parentDocumentId: String?, childDocumentId: String): DocumentsContract.Path {
        val top = parentDocumentId ?: ROOT_DOC_ID
        val tree = safeTree
        tree.resolve(top)
        tree.resolve(childDocumentId)
        if (top != childDocumentId && !tree.isChild(top, childDocumentId))
            throw FileNotFoundException("Not a descendant")
        val ids = com.notestaking.app.core.util.DocKeys.chain(top, childDocumentId)
            ?: throw FileNotFoundException("Document is not under requested parent")
        return DocumentsContract.Path(if (parentDocumentId == null) ROOT_DOC_ID else null, ids)
    }

    override fun getDocumentType(documentId: String): String = mimeOf(fileFor(documentId))

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        val file = fileFor(documentId)
        if (!file.isFile) throw FileNotFoundException("Not a regular file")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    @Synchronized
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String =
        safeTree.create(parentDocumentId, displayName, mimeType == Document.MIME_TYPE_DIR)

    @Synchronized
    override fun deleteDocument(documentId: String) = safeTree.delete(documentId)

    @Synchronized
    override fun renameDocument(documentId: String, displayName: String): String =
        safeTree.rename(documentId, displayName)

    @Synchronized
    override fun copyDocument(sourceDocumentId: String, targetParentDocumentId: String): String =
        safeTree.copy(sourceDocumentId, targetParentDocumentId)

    @Synchronized
    override fun moveDocument(sourceDocumentId: String, sourceParentDocumentId: String, targetParentDocumentId: String): String =
        safeTree.move(sourceDocumentId, sourceParentDocumentId, targetParentDocumentId)

    private fun fileFor(documentId: String): File = safeTree.resolve(documentId)

    private fun includeFile(cursor: MatrixCursor, documentId: String, file: File) {
        val isDir = file.isDirectory
        var flags = if (isDir) Document.FLAG_DIR_SUPPORTS_CREATE else Document.FLAG_SUPPORTS_WRITE
        if (documentId != ROOT_DOC_ID) {
            flags = flags or Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_RENAME or
                Document.FLAG_SUPPORTS_MOVE or Document.FLAG_SUPPORTS_COPY
        }
        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, documentId)
            add(Document.COLUMN_DISPLAY_NAME, if (documentId == ROOT_DOC_ID) context?.getString(R.string.app_storage).orEmpty() else file.name)
            add(Document.COLUMN_MIME_TYPE, if (isDir) Document.MIME_TYPE_DIR else mimeOf(file))
            add(Document.COLUMN_SIZE, file.length())
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(Document.COLUMN_FLAGS, flags)
        }
    }

    private fun mimeOf(file: File): String =
        if (file.isDirectory) Document.MIME_TYPE_DIR else "application/octet-stream"

    companion object {
        private const val ROOT_DOC_ID = "root"
        private const val DIR_NAME = "Notes"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_FLAGS, Root.COLUMN_TITLE, Root.COLUMN_ICON,
        )
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS,
        )

        private fun authority(context: Context): String = context.packageName + ".documents"

        /** The app's private notes directory (app-external, removed on uninstall); created on demand. */
        fun rootDir(context: Context): File {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            return File(base, DIR_NAME).apply { if (!exists()) mkdirs() }
        }

        /** The SAF tree uri for that directory, usable as an explorer browse root. */
        fun treeUri(context: Context): Uri {
            rootDir(context)
            return DocumentsContract.buildTreeDocumentUri(authority(context), ROOT_DOC_ID)
        }
    }
}
