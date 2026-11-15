package com.notestaking.app.platform

/** Keep only pages still present by identity; equal page content does not mean the same page. */
internal object PageSelectionSafety {
    fun <T : Any> prune(selected: MutableList<T>, current: List<T>) {
        selected.removeAll { picked -> current.none { it === picked } }
    }
}
