package com.mrnrod45.nstk.ui.components

import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrnrod45.nstk.ui.theme.SystemBlueLight
import androidx.compose.material3.ProvideTextStyle

// -----------------------------------------------------------------------------
// Mac Button — Aqua "Push Button" (22dp height, 5dp corner)
// -----------------------------------------------------------------------------
@Composable
fun MacButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Aqua button colors
    val backgroundColor = when {
        !enabled         -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        primary && isPressed -> SystemBlueLight.copy(alpha = 0.75f)
        primary          -> SystemBlueLight
        isPressed        -> MaterialTheme.colorScheme.surfaceVariant
        else             -> MaterialTheme.colorScheme.surface
    }

    val contentColor = if (primary) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        primary  -> Color.Transparent
        else     -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .height(22.dp) // Aqua push button height
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                ProvideTextStyle(
                    value = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
                        lineHeight = 13.sp
                    )
                ) {
                    content()
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Mac Text Field — Aqua round-rect text field (22dp height, 5dp corner)
// -----------------------------------------------------------------------------
@Composable
fun MacTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }

    // Focus ring — 3dp blue halo, matching Aqua appearance
    val borderColor = if (isFocused) SystemBlueLight.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
    val borderThickness = if (isFocused) 2.dp else 1.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .height(22.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(borderThickness, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        textStyle = TextStyle(
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 13.sp
        ),
        singleLine = singleLine,
        enabled = enabled,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                innerTextField()
            }
        }
    )
}

// -----------------------------------------------------------------------------
// Mac Label — secondary/tertiary label text
// -----------------------------------------------------------------------------
@Composable
fun MacLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = fontSize,
            color = color,
            fontWeight = FontWeight.Normal,
            lineHeight = fontSize
        )
    )
}

// -----------------------------------------------------------------------------
// Mac Group / Preferences pane section box (8dp corner)
// -----------------------------------------------------------------------------
@Composable
fun MacGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        if (title != null) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Mac Switch — Aqua on/off switch (26×15dp track, 13dp thumb)
// -----------------------------------------------------------------------------
@Composable
fun MacSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth  = 26.dp
    val trackHeight = 15.dp
    val thumbSize   = 13.dp  // slightly smaller than track height for 1dp padding

    val interactionSource = remember { MutableInteractionSource() }

    val thumbOffset by androidx.compose.animation.core.animateDpAsState(
        if (checked) trackWidth - thumbSize - 1.dp else 1.dp
    )

    val trackColor  = if (checked) SystemBlueLight else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (checked) SystemBlueLight else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(trackColor)
            .border(1.dp, borderColor, RoundedCornerShape(trackHeight / 2))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize)
                .offset(x = thumbOffset)
                .shadow(1.dp, androidx.compose.foundation.shape.CircleShape)
                .background(Color.White, androidx.compose.foundation.shape.CircleShape)
        )
    }
}

// -----------------------------------------------------------------------------
// Mac Checkbox — Aqua checkbox (13dp, 3dp corner)
// -----------------------------------------------------------------------------
@Composable
fun MacCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val size = 13.dp
    val interactionSource = remember { MutableInteractionSource() }

    val backgroundColor = if (checked) SystemBlueLight else MaterialTheme.colorScheme.surface
    val borderColor = if (checked) SystemBlueLight else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(5.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(5.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(9.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Mac Radio Button — Aqua (16dp circle, 8dp dot)
// -----------------------------------------------------------------------------
@Composable
fun MacRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val size = 16.dp
    val dotSize = 8.dp
    val interactionSource = remember { MutableInteractionSource() }

    val borderColor = if (selected) SystemBlueLight else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (selected) SystemBlueLight else MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, androidx.compose.foundation.shape.CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(Color.White, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Mac Segmented Control — NSSegmentedControl equivalent for exclusive choices
// -----------------------------------------------------------------------------
@Composable
fun MacSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSegmentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .height(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(2.dp)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .then(
                        if (selected)
                            Modifier
                                .shadow(1.dp, RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        else Modifier
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = { onSegmentSelected(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Mac Divider — NSColor.separatorColor equivalent
// -----------------------------------------------------------------------------
@Composable
fun MacDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

// -----------------------------------------------------------------------------
// Mac Icon Button — toolbar or inline icon action
// -----------------------------------------------------------------------------
@Composable
fun MacIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            content()
        }
    }
}

// -----------------------------------------------------------------------------
// Mac Dropdown — NSPopUpButton style with chevron indicator
// -----------------------------------------------------------------------------
@Composable
fun MacDropdown(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // NSPopUpButton "rounded" bezel: subtle vertical gradient, less rounded
    // than a pill-shaped push button, double-chevron affordance.
    val bezelBrush = when {
        !enabled  -> Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ))
        isPressed -> Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant
        ))
        else -> Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ))
    }

    Box(
        modifier = modifier.onGloballyPositioned { buttonWidthPx = it.size.width }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(bezelBrush)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 0.8f else 0.4f),
                    RoundedCornerShape(6.dp)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = { expanded = true }
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedItem,
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        if (expanded) {
            Popup(
                onDismissRequest = { expanded = false },
                alignment = Alignment.TopStart
            ) {
                // Pin popup to exactly the trigger button width — never wider
                val popupWidth = with(density) { buttonWidthPx.toDp() }
                Box(
                    modifier = Modifier
                        .width(popupWidth)
                        .padding(top = 22.dp) // offset below the button
                        .shadow(6.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .zIndex(10f)
                ) {
                    Column {
                        items.forEach { item ->
                            val isSelected = item == selectedItem
                            val itemBg = if (isSelected) SystemBlueLight else Color.Transparent
                            val itemColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(itemBg)
                                    .clickable {
                                        onItemSelected(item)
                                        expanded = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Spacer(modifier = Modifier.width(15.dp))
                                }
                                Text(
                                    text = item,
                                    style = TextStyle(fontSize = 13.sp, color = itemColor, lineHeight = 13.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
