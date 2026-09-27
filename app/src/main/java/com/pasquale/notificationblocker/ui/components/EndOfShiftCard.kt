package com.pasquale.notificationblocker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.pasquale.notificationblocker.R
import com.pasquale.notificationblocker.ui.theme.NotificationBlockerTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * "End of shift" celebration: work notifications swipe away, the sun sets, the moon rises.
 * Plays once, holds the last frame, then asks to be hidden via [onFinished].
 * With "Remove animations" enabled Lottie jumps straight to the last frame.
 */
@Composable
fun EndOfShiftCard(
    visible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EndOfShiftAnimation(
                    onFinished = onFinished,
                    modifier = Modifier.size(160.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.end_of_shift_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.end_of_shift_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EndOfShiftAnimation(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.end_of_shift))
    val animatable = rememberLottieAnimatable()
    val currentOnFinished by rememberUpdatedState(onFinished)
    val isPreview = LocalInspectionMode.current

    LaunchedEffect(composition) {
        val loaded = composition ?: return@LaunchedEffect
        animatable.animate(loaded)
        delay(HoldAfterEnd)
        currentOnFinished()
    }

    LottieAnimation(
        composition = composition,
        progress = { if (isPreview) 1f else animatable.progress },
        dynamicProperties = rememberEndOfShiftColors(),
        modifier = modifier,
    )
}

// Maps the group names in res/raw/end_of_shift.json (see tools/lottie/end_of_shift.py) to theme colors
@Composable
private fun rememberEndOfShiftColors() = with(MaterialTheme.colorScheme) {
    rememberLottieDynamicProperties(
        colorProperty("card_bg", secondaryContainer),
        colorProperty("card_dot", secondary),
        colorProperty("card_line", onSecondaryContainer),
        colorProperty("sun", secondary),
        colorProperty("horizon", onSurfaceVariant, LottieProperty.STROKE_COLOR),
        colorProperty("sky_night", primary),
        colorProperty("moon", primary),
        colorProperty("star", primary),
    )
}

@Composable
private fun colorProperty(group: String, color: Color, property: Int = LottieProperty.COLOR) =
    rememberLottieDynamicProperty(
        property = property,
        value = color.toArgb(),
        "**", group, "**",
    )

private val HoldAfterEnd = 2000.milliseconds

@Preview(showBackground = true, name = "End of shift - Light")
@Composable
fun EndOfShiftCardPreviewLight() {
    NotificationBlockerTheme(darkTheme = false) {
        EndOfShiftCard(visible = true, onFinished = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, name = "End of shift - Dark")
@Composable
fun EndOfShiftCardPreviewDark() {
    NotificationBlockerTheme(darkTheme = true) {
        EndOfShiftCard(visible = true, onFinished = {}, modifier = Modifier.padding(16.dp))
    }
}
