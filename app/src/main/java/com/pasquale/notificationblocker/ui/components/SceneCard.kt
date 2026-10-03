package com.pasquale.notificationblocker.ui.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import com.pasquale.notificationblocker.ui.zen.ZenState

/**
 * The room scene on Home, framed as a card that fills the size it is given: at work the desk, off
 * work the calm scene. On Home that size is whatever the page leaves free (see [HomeColumn]); the
 * scene is drawn to cover it and crops from the bottom, so any proportion works.
 */
@Composable
fun SceneCard(
    isOffWork: Boolean,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    environment: ZenState? = null,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        ZenScene(isOffWork = isOffWork, animate = animate, environment = environment)
    }
}

@Preview(showBackground = true, name = "Scene card - off work")
@Composable
fun SceneCardOffWorkPreview() {
    NotificationBlockerTheme {
        SceneCard(isOffWork = true, modifier = Modifier.padding(16.dp).aspectRatio(2f))
    }
}

@Preview(showBackground = true, name = "Scene card - at work")
@Composable
fun SceneCardAtWorkPreview() {
    NotificationBlockerTheme {
        SceneCard(isOffWork = false, modifier = Modifier.padding(16.dp).aspectRatio(2f))
    }
}

@Preview(showBackground = true, name = "Scene card - short (3:1)")
@Composable
fun SceneCardShortPreview() {
    NotificationBlockerTheme {
        SceneCard(isOffWork = true, modifier = Modifier.padding(16.dp).aspectRatio(3f))
    }
}
