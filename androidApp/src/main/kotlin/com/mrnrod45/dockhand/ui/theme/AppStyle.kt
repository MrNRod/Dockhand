package com.mrnrod45.dockhand.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Which design language the UI is currently dressed in. */
enum class UiStyle {
    MATERIAL,
    ONE_UI
}

/**
 * Layout and behaviour tokens that differ between Material and One UI.
 *
 * `MaterialTheme` already carries colour, shape and type, so this only holds the decisions Material
 * has no slot for: whether to group rows into cards, whether the title collapses, how much air to
 * leave around content, and which control Samsung would use for a boolean.
 */
data class AppStyle(
    val uiStyle: UiStyle,
    /** One UI opens screens with an oversized title that collapses as you scroll. */
    val collapsingLargeTitle: Boolean,
    /** One UI wraps related rows in a rounded card instead of separating them with dividers. */
    val groupedCards: Boolean,
    /** One UI uses switches for every boolean; Material uses checkboxes for multi-select lists. */
    val switchesForBooleans: Boolean,
    /** One UI centres actions in a full-width pill; Material uses a wrap-content button. */
    val fullWidthPillButtons: Boolean,
    val screenPadding: Dp,
    val groupSpacing: Dp,
    val rowSpacing: Dp,
    val cardCorner: Dp
) {
    val isOneUi: Boolean get() = uiStyle == UiStyle.ONE_UI

    companion object {
        val Material = AppStyle(
            uiStyle = UiStyle.MATERIAL,
            collapsingLargeTitle = false,
            groupedCards = false,
            switchesForBooleans = false,
            fullWidthPillButtons = false,
            screenPadding = 16.dp,
            groupSpacing = 12.dp,
            rowSpacing = 4.dp,
            cardCorner = 12.dp
        )

        val OneUi = AppStyle(
            uiStyle = UiStyle.ONE_UI,
            collapsingLargeTitle = true,
            groupedCards = true,
            switchesForBooleans = true,
            fullWidthPillButtons = true,
            screenPadding = 20.dp,
            groupSpacing = 20.dp,
            rowSpacing = 8.dp,
            cardCorner = 26.dp
        )
    }
}

val LocalAppStyle = staticCompositionLocalOf { AppStyle.Material }
