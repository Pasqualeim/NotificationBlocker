package com.pasquale.notificationblocker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.ui.AppInfo
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme

@Composable
fun AppItemRow(
    app: AppInfo,
    onToggleBlocked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppItemRow(
        appName = app.name,
        packageName = app.packageName,
        isBlocked = app.isBlocked,
        onToggleBlocked = onToggleBlocked,
        modifier = modifier,
    )
}

@Composable
fun AppItemRow(
    appName: String,
    packageName: String,
    isBlocked: Boolean,
    onToggleBlocked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedContainerColor by animateColorAsState(
        targetValue = if (isBlocked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(durationMillis = 200),
        label = "AppRowBgColor",
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isBlocked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "AppRowBorderColor",
    )

    val cdToggle = stringResource(R.string.cd_toggle_app_blocking, appName)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Switch,
                onClickLabel = cdToggle,
            ) {
                onToggleBlocked(!isBlocked)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor,
        ),
        border = BorderStroke(1.dp, animatedBorderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconImage(
                packageName = packageName,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = isBlocked,
                onCheckedChange = onToggleBlocked,
                modifier = Modifier.semantics {
                    contentDescription = cdToggle
                },
            )
        }
    }
}

@Preview(showBackground = true, name = "App Item Row - Blocked")
@Composable
fun AppItemRowBlockedPreview() {
    NotificationBlockerTheme {
        AppItemRow(
            appName = "Slack",
            packageName = "com.Slack",
            isBlocked = true,
            onToggleBlocked = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "App Item Row - Not Blocked")
@Composable
fun AppItemRowUnblockedPreview() {
    NotificationBlockerTheme {
        AppItemRow(
            appName = "Microsoft Teams",
            packageName = "com.microsoft.teams",
            isBlocked = false,
            onToggleBlocked = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
