package com.notestaking.app.ui

import android.animation.ValueAnimator
import android.graphics.drawable.AnimatedVectorDrawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.notestaking.app.R

/**
 * Frame-paced vector ink reveal on the navy launch stage. The stroke animates continuously
 * instead of swapping 24 PNGs at 67 ms intervals; the system's reduced-motion setting
 * shows the finished brand mark, and the animation stops when the loader leaves the UI.
 */
@Composable
fun NotesTakingLoader(modifier: Modifier = Modifier) {
    val cfg = LocalConfiguration.current
    // Scale with the short edge but cap it, so phones render big without tablets ballooning.
    val shortEdge = minOf(cfg.screenWidthDp, cfg.screenHeightDp)
    val side = minOf(shortEdge * 0.55f, 288f).dp
    Box(
        modifier.fillMaxSize().background(colorResource(R.color.loader_bg)),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    setImageResource(
                        if (ValueAnimator.areAnimatorsEnabled()) R.drawable.notestaking_loader
                        else R.drawable.ic_brand_mark
                    )
                }
            },
            modifier = Modifier.size(side),
            onReset = null,
            onRelease = { view -> (view.drawable as? AnimatedVectorDrawable)?.stop() },
            update = { view ->
                (view.drawable as? AnimatedVectorDrawable)?.let { if (!it.isRunning) it.start() }
            },
        )
    }
}
