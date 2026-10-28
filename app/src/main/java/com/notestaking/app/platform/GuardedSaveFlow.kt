package com.notestaking.app.platform

/** The only permitted outcomes of trying to leave the visible document. */
internal enum class SaveNavigationDecision { BLOCK, FLUSH, PROMPT, CONTINUE }

/** Saving takes precedence over the transient dirty flag; it may be false mid-write. */
internal fun saveNavigationDecision(
    writing: Boolean,
    hasAutosaveDestination: Boolean,
    hasUnsavedChanges: Boolean,
): SaveNavigationDecision = when {
    writing -> SaveNavigationDecision.BLOCK
    hasAutosaveDestination -> SaveNavigationDecision.FLUSH
    hasUnsavedChanges -> SaveNavigationDecision.PROMPT
    else -> SaveNavigationDecision.CONTINUE
}

/**
 * One-shot continuation for navigation gated behind the system Save As picker.
 * Taking the callback clears it immediately, including on picker cancellation.
 * A failed save never triggers navigation, even if a callback is repeated later.
 */
internal class GuardedSaveFlow {
    private var pending: (() -> Unit)? = null

    fun queue(action: () -> Unit) {
        pending = action
    }

    fun consume(): (Boolean) -> Unit {
        val action = pending
        pending = null
        var resolved = false
        return { saved ->
            if (!resolved) {
                resolved = true
                if (saved) action?.invoke()
            }
        }
    }
}
