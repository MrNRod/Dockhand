package com.mrnrod45.dockhand.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mrnrod45.dockhand.ui.theme.LocalAppStyle

/**
 * Widgets that render Material or One UI depending on [LocalAppStyle].
 *
 * The One UI variants follow Samsung's three structural habits: an oversized title that collapses
 * as you scroll, related rows grouped into a rounded card, and switches (never checkboxes) for
 * booleans.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAdaptiveTopBarScrollBehavior(): TopAppBarScrollBehavior {
    val state = rememberTopAppBarState()
    return if (LocalAppStyle.current.collapsingLargeTitle) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state)
    } else {
        TopAppBarDefaults.pinnedScrollBehavior(state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val transparent = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent
    )
    if (LocalAppStyle.current.collapsingLargeTitle) {
        LargeTopAppBar(
            title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
            colors = transparent,
            scrollBehavior = scrollBehavior,
            actions = actions
        )
    } else {
        TopAppBar(
            title = { Text(title) },
            colors = transparent,
            scrollBehavior = scrollBehavior,
            actions = actions
        )
    }
}

/**
 * A titled group of settings rows. One UI draws the title as a small accent label above a rounded
 * card holding the rows; Material keeps the rows inline under a section heading.
 */
@Composable
fun SettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val style = LocalAppStyle.current
    Column(modifier = modifier.fillMaxWidth().padding(bottom = style.groupSpacing)) {
        if (title != null) {
            Text(
                text = title,
                style = if (style.isOneUi) MaterialTheme.typography.labelLarge
                else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    start = if (style.isOneUi) 8.dp else 0.dp,
                    bottom = 8.dp
                )
            )
        }
        if (style.groupedCards) {
            Surface(
                shape = RoundedCornerShape(style.cardCorner),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(style.rowSpacing),
                    content = content
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(style.rowSpacing),
                content = content
            )
        }
    }
}

/** Padding for a row inside a [SettingsGroup] — inset on One UI to clear the card corners. */
@Composable
private fun rowModifier(enabled: Boolean, onClick: (() -> Unit)?): Modifier {
    val style = LocalAppStyle.current
    val base = Modifier.fillMaxWidth()
    val clickable = if (onClick != null && enabled) base.clickable(onClick = onClick) else base
    return if (style.isOneUi) {
        clickable.padding(horizontal = 20.dp, vertical = 14.dp)
    } else {
        clickable.padding(vertical = 4.dp)
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    val style = LocalAppStyle.current
    Row(
        modifier = modifier.then(rowModifier(enabled) { onCheckedChange(!checked) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!style.switchesForBooleans) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (style.switchesForBooleans) {
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

/** One UI centres primary actions in a full-width pill; Material keeps a wrap-content button. */
@Composable
fun AdaptiveActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val style = LocalAppStyle.current
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = if (style.fullWidthPillButtons) RoundedCornerShape(percent = 50) else ButtonDefaults.shape,
        modifier = if (style.fullWidthPillButtons) modifier.fillMaxWidth().height(52.dp) else modifier
    ) {
        Text(text)
    }
}
