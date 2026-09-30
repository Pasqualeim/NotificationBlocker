package com.pasquale.notificationblocker.ui.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
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
 * The room scene on Home, framed as a card with a fixed 2.6:1 ratio so it keeps its size inside
 * the scrolling column: at work the desk, off work the calm scene.
 */
@Composable
fun SceneCard(
    isOffWork: Boolean,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    environment: ZenState? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(SCENE_ASPECT_RATIO),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        ZenScene(isOffWork = isOffWork, animate = animate, environment = environment)
    }
}

// Wide and short on purpose: with 2:1 the schedule card slipped under the bottom button on a 20:9 phone
private const val SCENE_ASPECT_RATIO = 2.6f

@Preview(showBackground = true, name = "Scene card - off work")
@Composable
fun SceneCardOffWorkPreview() {
    NotificationBlockerTheme {
        SceneCard(isOffWork = true, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, name = "Scene card - at work")
@Composable
fun SceneCardAtWorkPreview() {
    NotificationBlockerTheme {
        SceneCard(isOffWork = false, modifier = Modifier.padding(16.dp))
    }
}
