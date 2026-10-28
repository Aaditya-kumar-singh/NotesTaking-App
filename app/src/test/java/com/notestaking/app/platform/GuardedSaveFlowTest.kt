package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class GuardedSaveFlowTest {
    @Test fun saveInProgressAlwaysBlocksNavigationEvenIfDirtyWasCleared() {
        assertEquals(
            SaveNavigationDecision.BLOCK,
            saveNavigationDecision(writing = true, hasAutosaveDestination = false, hasUnsavedChanges = false),
        )
        assertEquals(
            SaveNavigationDecision.BLOCK,
            saveNavigationDecision(writing = true, hasAutosaveDestination = true, hasUnsavedChanges = true),
        )
    }

    @Test fun autosavedFilesWaitForConfirmedFlush() {
        assertEquals(
            SaveNavigationDecision.FLUSH,
            saveNavigationDecision(writing = false, hasAutosaveDestination = true, hasUnsavedChanges = false),
        )
        assertEquals(
            SaveNavigationDecision.FLUSH,
            saveNavigationDecision(writing = false, hasAutosaveDestination = true, hasUnsavedChanges = true),
        )
    }

    @Test fun externalUnsavedFileRequiresSaveDiscardOrCancelPrompt() {
        assertEquals(
            SaveNavigationDecision.PROMPT,
            saveNavigationDecision(writing = false, hasAutosaveDestination = false, hasUnsavedChanges = true),
        )
        assertEquals(
            SaveNavigationDecision.CONTINUE,
            saveNavigationDecision(writing = false, hasAutosaveDestination = false, hasUnsavedChanges = false),
        )
    }


    @Test fun successfulSaveRunsGuardedNavigationExactlyOnce() {
        val flow = GuardedSaveFlow()
        var navigations = 0
        flow.queue { navigations++ }
        val resolve = flow.consume()
        resolve(true)
        resolve(true)
        assertEquals(1, navigations)
    }

    @Test fun failedWriteDoesNotRunNavigationOrLeaveAnOldAction() {
        val flow = GuardedSaveFlow()
        var navigations = 0
        flow.queue { navigations++ }
        flow.consume()(false)
        flow.consume()(true) // A later, unrelated Save As must not run the old action.
        assertEquals(0, navigations)
    }

    @Test fun cancelledPickerDropsContinuationBeforeANewSave() {
        val flow = GuardedSaveFlow()
        var navigations = 0
        flow.queue { navigations += 10 }
        flow.consume() // Picker returned null. No save callback will arrive.
        flow.queue { navigations++ }
        flow.consume()(true)
        assertEquals(1, navigations)
    }

    @Test fun duplicateOrLateFailedCallbackCannotFireAnotherAction() {
        val flow = GuardedSaveFlow()
        var navigations = 0
        flow.queue { navigations += 10 }
        val first = flow.consume()
        flow.queue { navigations++ }
        first(false)
        first(true)
        flow.consume()(true)
        assertEquals(1, navigations)
    }
}
