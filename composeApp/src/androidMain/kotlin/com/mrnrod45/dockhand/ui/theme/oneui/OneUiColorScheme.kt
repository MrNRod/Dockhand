package com.mrnrod45.dockhand.ui.theme.oneui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Re-layers a system colour scheme the way One UI stacks its surfaces, without touching a hue.
 *
 * This is the whole of the One UI colour treatment. On Android 12+ the palette itself already comes
 * from the system — on a Galaxy that is Samsung's adaptive palette — so there is nothing to
 * re-derive. What differs is *structure*: Material paints content on a tinted surface, while One UI
 * floats cards above a distinctly different page (light grey page with white cards, black page with
 * lifted near-black cards). Both layers already exist in the scheme as `surfaceContainerLowest` and
 * `surfaceContainer`, so this only reassigns which role goes where.
 */
internal fun ColorScheme.withOneUiSurfaceLayering(dark: Boolean): ColorScheme =
    if (dark) {
        copy(
            background = surfaceContainerLowest,
            surface = surfaceContainer,
            surfaceContainerLow = surfaceContainer
        )
    } else {
        copy(
            background = surfaceContainerLow,
            surface = surfaceContainerLowest,
            surfaceContainer = surfaceContainerLowest
        )
    }

/**
 * One of One UI's tonal ramps.
 *
 * Only used for the bundled fallback palette below Android 12, where the platform publishes no
 * colours at all. Indices follow the AOSP `system_*_<index>` naming, which runs lightest first:
 * index 0 is white, index 1000 is black, and Material's tone N maps to index `(100 - N) * 10`.
 */
internal class TonalRamp(private val tones: Map<Int, Color>) {
    operator fun get(index: Int): Color = tones[index] ?: Color.Unspecified

    companion object {
        val INDICES = intArrayOf(0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000)
    }
}

internal class OneUiTonalPalette(
    val accent1: TonalRamp,
    val accent2: TonalRamp,
    val accent3: TonalRamp,
    val neutral1: TonalRamp,
    val neutral2: TonalRamp
)

internal fun oneUiLightColorScheme(palette: OneUiTonalPalette): ColorScheme {
    val a1 = palette.accent1
    val a2 = palette.accent2
    val a3 = palette.accent3
    val n1 = palette.neutral1
    val n2 = palette.neutral2
    return lightColorScheme(
        primary = a1[600],
        onPrimary = a1[0],
        primaryContainer = a1[100],
        onPrimaryContainer = a1[900],
        inversePrimary = a1[200],
        secondary = a2[600],
        onSecondary = a2[0],
        secondaryContainer = a2[100],
        onSecondaryContainer = a2[900],
        tertiary = a3[600],
        onTertiary = a3[0],
        tertiaryContainer = a3[100],
        onTertiaryContainer = a3[900],
        background = n1[50],
        onBackground = n1[900],
        surface = n1[0],
        onSurface = n1[900],
        surfaceVariant = n2[100],
        onSurfaceVariant = n2[700],
        surfaceTint = a1[600],
        inverseSurface = n1[800],
        inverseOnSurface = n1[50],
        surfaceContainerLowest = n1[0],
        surfaceContainerLow = n1[10],
        surfaceContainer = n1[0],
        surfaceContainerHigh = n1[50],
        surfaceContainerHighest = n1[100],
        surfaceBright = n1[0],
        surfaceDim = n1[100],
        outline = n2[500],
        outlineVariant = n2[200],
        scrim = Color.Black
    )
}

internal fun oneUiDarkColorScheme(palette: OneUiTonalPalette): ColorScheme {
    val a1 = palette.accent1
    val a2 = palette.accent2
    val a3 = palette.accent3
    val n1 = palette.neutral1
    val n2 = palette.neutral2
    return darkColorScheme(
        primary = a1[200],
        onPrimary = a1[800],
        primaryContainer = a1[700],
        onPrimaryContainer = a1[100],
        inversePrimary = a1[600],
        secondary = a2[200],
        onSecondary = a2[800],
        secondaryContainer = a2[700],
        onSecondaryContainer = a2[100],
        tertiary = a3[200],
        onTertiary = a3[800],
        tertiaryContainer = a3[700],
        onTertiaryContainer = a3[100],
        // One UI's dark page is black, with cards lifted just above it.
        background = n1[1000],
        onBackground = n1[50],
        surface = n1[900],
        onSurface = n1[50],
        surfaceVariant = n2[800],
        onSurfaceVariant = n2[200],
        surfaceTint = a1[200],
        inverseSurface = n1[50],
        inverseOnSurface = n1[800],
        surfaceContainerLowest = n1[1000],
        surfaceContainerLow = n1[900],
        surfaceContainer = n1[900],
        surfaceContainerHigh = n1[800],
        surfaceContainerHighest = n1[700],
        surfaceBright = n1[700],
        surfaceDim = n1[1000],
        outline = n2[400],
        outlineVariant = n2[700],
        scrim = Color.Black
    )
}

/**
 * Samsung Blue ramps, used only below Android 12 where there is no system palette to follow.
 */
internal val BundledOneUiPalette = OneUiTonalPalette(
    accent1 = ramp(
        0xFFFFFFFF, 0xFFFDFCFF, 0xFFEDF1FF, 0xFFD7E3FF, 0xFFABC7FF, 0xFF6FA8FF,
        0xFF2C8CFF, 0xFF0079EE, 0xFF0060C8, 0xFF004A9C, 0xFF003572, 0xFF00214A, 0xFF000000
    ),
    accent2 = ramp(
        0xFFFFFFFF, 0xFFFBFCFF, 0xFFECF0FA, 0xFFDCE2F0, 0xFFC0C7D6, 0xFFA5ABBA,
        0xFF8A909F, 0xFF6F7684, 0xFF575E6B, 0xFF404653, 0xFF2A303C, 0xFF151B26, 0xFF000000
    ),
    accent3 = ramp(
        0xFFFFFFFF, 0xFFFCFCFF, 0xFFE7F5F8, 0xFFCFEAF0, 0xFFA6D6E0, 0xFF7FC0CD,
        0xFF56A6B6, 0xFF328C9C, 0xFF00707F, 0xFF005662, 0xFF003C46, 0xFF00242B, 0xFF000000
    ),
    neutral1 = ramp(
        0xFFFFFFFF, 0xFFFCFCFF, 0xFFF2F4F7, 0xFFE9EBEF, 0xFFDCDEE3, 0xFFC0C3C8,
        0xFFA5A8AD, 0xFF8A8D92, 0xFF6F7378, 0xFF575A5F, 0xFF2B2D31, 0xFF1B1D20, 0xFF000000
    ),
    neutral2 = ramp(
        0xFFFFFFFF, 0xFFFBFCFF, 0xFFEFF1F7, 0xFFE5E8EF, 0xFFD5D8DF, 0xFFB9BCC4,
        0xFF9EA1A9, 0xFF83868E, 0xFF6A6D75, 0xFF52555C, 0xFF2A2D33, 0xFF1A1D22, 0xFF000000
    )
)

private fun ramp(vararg argb: Long): TonalRamp =
    TonalRamp(TonalRamp.INDICES.mapIndexed { i, index -> index to Color(argb[i]) }.toMap())
