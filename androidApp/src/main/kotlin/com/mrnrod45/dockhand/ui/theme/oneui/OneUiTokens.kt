package com.mrnrod45.dockhand.ui.theme.oneui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One UI's corner radii, which are considerably rounder than Material's at every size — Samsung
 * treats the squircle as the primary shape cue of the system.
 */
internal val OneUiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

internal val MaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * One UI's type scale: headline weights are heavier, tracking is tighter than Material's, and body
 * text runs a touch larger. `FontFamily.Default` already resolves to SamsungOne / One UI Sans on a
 * Galaxy device, so the family is deliberately left alone.
 */
internal fun oneUiTypography(base: Typography): Typography = base.copy(
    displaySmall = base.displaySmall.oneUi(FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.oneUi(FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.oneUi(FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineSmall = base.headlineSmall.oneUi(FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = base.titleLarge.oneUi(FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = base.titleMedium.oneUi(FontWeight.SemiBold, letterSpacing = 0.sp),
    titleSmall = base.titleSmall.oneUi(FontWeight.SemiBold, letterSpacing = 0.sp),
    bodyLarge = base.bodyLarge.oneUi(FontWeight.Normal, letterSpacing = 0.sp, fontSize = 16.sp),
    bodyMedium = base.bodyMedium.oneUi(FontWeight.Normal, letterSpacing = 0.sp, fontSize = 15.sp),
    labelLarge = base.labelLarge.oneUi(FontWeight.SemiBold, letterSpacing = 0.sp)
)

private fun TextStyle.oneUi(
    weight: FontWeight,
    letterSpacing: androidx.compose.ui.unit.TextUnit,
    fontSize: androidx.compose.ui.unit.TextUnit = this.fontSize
) = copy(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    letterSpacing = letterSpacing,
    fontSize = fontSize
)
