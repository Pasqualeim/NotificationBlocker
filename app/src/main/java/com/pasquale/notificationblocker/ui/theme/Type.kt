package com.pasquale.notificationblocker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Material 3 type scale in the phone's own font (Roboto on a Pixel, the maker's font elsewhere),
 * like the system apps: no downloaded font. Headlines and titles a step bolder than the defaults, as
 * in the Expressive apps, so a screen reads from its title down.
 */
private val Base = Typography()

val Typography = Base.copy(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Medium),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Medium),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Medium),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Medium),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Medium),
)

val TimeDisplayTextStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 64.sp,
    lineHeight = 64.sp,
    fontFeatureSettings = "tnum",
)
