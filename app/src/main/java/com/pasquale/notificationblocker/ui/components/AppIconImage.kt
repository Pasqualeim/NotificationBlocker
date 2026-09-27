package com.pasquale.notificationblocker.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.drawable.toBitmap
import com.pasquale.notificationblocker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private object AppIconCache {
    val cache = ConcurrentHashMap<String, Drawable>()
}

@Composable
fun AppIconImage(
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val iconDrawableState = produceState<Drawable?>(
        initialValue = AppIconCache.cache[packageName],
        key1 = packageName
    ) {
        if (value == null && packageName.isNotBlank()) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    context.packageManager.getApplicationIcon(packageName)
                } catch (_: Exception) {
                    null
                }
            }
            if (loaded != null) {
                AppIconCache.cache[packageName] = loaded
                value = loaded
            }
        }
    }

    val drawable = iconDrawableState.value
    if (drawable != null) {
        val bitmap = remember(drawable) { drawable.toBitmap().asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.ic_default_app),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}
