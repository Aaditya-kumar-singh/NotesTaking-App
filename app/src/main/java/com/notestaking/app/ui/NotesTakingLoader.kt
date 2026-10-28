package com.notestaking.app.ui

import android.graphics.drawable.AnimationDrawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.notestaking.app.R

/**
 * Fullscreen launch animation: the exported frame-by-frame glitch loader
 * ([R.drawable.notestaking_loader] — a 24-frame ~1.6s [AnimationDrawable]) centred on a
 * deep navy stage that matches the new notebook artwork, so
 * there's no edge seam. Shown while the session restores, then faded out.
 */
@Composable
fun NotesTakingLoader(modifier: Modifier = Modifier) {
    val cfg = LocalConfiguration.current
    // Scale with the short edge but cap it, so phones render big without tablets ballooning.
    val shortEdge = minOf(cfg.screenWidthDp, cfg.screenHeightDp)
    val side = minOf(shortEdge * 0.55f, 288f).dp
    Box(
        modifier.fillMaxSize().background(Color(0xFF07142A)),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx -> ImageView(ctx).apply { setImageResource(R.drawable.notestaking_loader) } },
            update = { iv -> (iv.drawable as? AnimationDrawable)?.let { if (!it.isRunning) it.start() } },
            modifier = Modifier.size(side),
        )
    }
}
