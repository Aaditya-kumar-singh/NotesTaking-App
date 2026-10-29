package com.notestaking.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notestaking.app.R
import com.notestaking.app.ui.icons.NotesTakingIcons
import com.notestaking.app.ui.theme.LocalPalette
import com.notestaking.app.ui.theme.toComposeColor

/** NotesTaking App identity. No old project funding/repository links are promoted here.
 * MIT attribution remains visible and the full copyright notice remains in LICENSE. */
@Composable
fun AboutPane() {
    val palette = LocalPalette.current
    val ctx = LocalContext.current
    val version = remember {
        runCatching {
            val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode else pi.versionCode.toLong()
            "${pi.versionName} ($code)"
        }.getOrDefault("")
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(88.dp)
            Spacer(Modifier.height(16.dp))
            Text("NotesTaking App", color = palette.text.toComposeColor(), fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.app_tagline),
                color = palette.textDim.toComposeColor(), fontSize = 13.sp, textAlign = TextAlign.Center)
            if (version.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Tooltip(stringResource(R.string.copy_version)) {
                    Row(
                        Modifier.clickable { copyVersion(ctx, version) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.version_n, version),
                            color = palette.textDim.toComposeColor(), fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(NotesTakingIcons.copy, stringResource(R.string.copy_version),
                            tint = palette.textDim.toComposeColor(), modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
            Text("Open-source credits", color = palette.text.toComposeColor(),
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Text("Based on the MIT-licensed XNotes project by Shardul Vikram Singh.",
                color = palette.textDim.toComposeColor(), fontSize = 12.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.mit_license),
                color = palette.textDim.toComposeColor(), fontSize = 12.sp)
        }
    }
}
/** Display the shared launcher mark in About and navigation without bitmap decoding. */
@Composable
internal fun BrandMark(size: Dp) {
    Box(
        Modifier.size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(colorResource(R.color.ic_launcher_background)),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_brand_mark), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}

private fun copyVersion(ctx: Context, version: String) {
    runCatching {
        val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clip.setPrimaryClip(ClipData.newPlainText("NotesTaking App version", "NotesTaking App $version"))
        Toast.makeText(ctx, ctx.getString(R.string.version_copied), Toast.LENGTH_SHORT).show()
    }
}
