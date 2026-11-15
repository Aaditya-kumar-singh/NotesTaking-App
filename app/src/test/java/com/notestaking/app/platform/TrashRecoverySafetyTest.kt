package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrashRecoverySafetyTest {
    private val info = Triple("info-id", "trash-info.json", false)
    private val note = Triple("note-id", "Meeting.xnote", false)
    private val folder = Triple("folder-id", "Project", true)

    @Test fun exactlyOneNoteOrFolderCanBeRestored() {
        assertEquals(note, TrashRecoverySafety.singlePayload(listOf(info, note), info.second))
        assertEquals(folder, TrashRecoverySafety.singlePayload(listOf(folder, info), info.second))
    }

    @Test fun missingOrUnreadableEntriesNeverAuthorizeDeletingWrapper() {
        assertNull(TrashRecoverySafety.singlePayload(emptyList(), info.second))
        assertNull(TrashRecoverySafety.singlePayload(listOf(info), info.second))
    }

    @Test fun ambiguousWrappersCannotRestoreOneItemAndDiscardOthers() {
        assertNull(TrashRecoverySafety.singlePayload(listOf(info, note, folder), info.second))
        assertNull(TrashRecoverySafety.singlePayload(listOf(note, folder), info.second))
    }

    @Test fun metadataNameOnADirectoryIsNotMistakenForTheSidecar() {
        val namedFolder = Triple("folder-id", info.second, true)
        assertEquals(namedFolder, TrashRecoverySafety.singlePayload(listOf(info, namedFolder), info.second))
    }
}
