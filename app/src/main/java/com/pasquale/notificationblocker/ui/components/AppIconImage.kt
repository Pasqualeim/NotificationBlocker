package com.pasquale.notificationblocker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pasquale.notificationblocker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** App icons already rasterized at the size they are shown, by package and size in px. */
private object AppIconCache {
    val bitmaps = ConcurrentHashMap<String, ImageBitmap>()
}

/**
 * The launcher icon of [packageName], [size] square. Rasterized once, off the main thread, at the
 * size it is shown: redrawing the full adaptive icon (284 px on a 420 dpi phone) each time a row
 * scrolled back into view made the app list stutter.
 */
@Composable
fun AppIconImage(
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val key = "$packageName@$px"

    val icon by produceState(initialValue = AppIconCache.bitmaps[key], key) {
        if (value == null && packageName.isNotBlank()) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getApplicationIcon(packageName).toBitmap(px, px).asImageBitmap()
                        .also { it.prepareToDraw() }
                }.getOrNull()
            }?.also { AppIconCache.bitmaps[key] = it }
        }
    }

    val bitmap = icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier.size(size),
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.ic_default_app),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
        )
    }
}
