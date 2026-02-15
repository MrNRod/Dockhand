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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrnrod45.nstk.ui.theme.SystemBlueLight
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.focus.onFocusChanged

// -----------------------------------------------------------------------------
// Mac Button (Standard "Push Button")
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
    
    // Apple-like styling
    val backgroundColor = if (primary) {
        if (isPressed) SystemBlueLight.copy(alpha = 0.8f) else SystemBlueLight
    } else {
        if (isPressed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (primary) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (primary) Color.Transparent else MaterialTheme.colorScheme.outlineVariant // Light gray border for standard button
    
    val elevation = if (isPressed || !enabled) 0.dp else 1.dp

    Box(
        modifier = modifier
            .height(28.dp) // Standard macOS button height approx
            .shadow(elevation, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom visual feedback above
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                // Default TextStyle for buttons
                ProvideTextStyle(
                    value = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 13.sp // Ensure no extra vertical padding for perfect centering
                    )
                ) {
                    content()
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Mac Text Field
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
    
    // Focus Ring Color (Halo)
    val borderColor = if (isFocused) SystemBlueLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
    val borderThickness = if (isFocused) 2.dp else 1.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
            .border(borderThickness, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
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
// Mac Label (Secondary Help Text)
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
// Mac Card / Group Box
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp)) // Adaptive background
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Mac Switch (Approximation)
// -----------------------------------------------------------------------------
@Composable
fun MacSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val thumbRadius = 10.dp
    val trackWidth = 36.dp
    val trackHeight = 20.dp
    
    val interactionSource = remember { MutableInteractionSource() }
    
    // Animate position
    val thumbOffset by androidx.compose.animation.core.animateDpAsState(
        if (checked) trackWidth - thumbRadius * 2 - 2.dp else 2.dp
    )
    
    val trackColor = if (checked) SystemBlueLight else MaterialTheme.colorScheme.surfaceVariant
    val thumbColor = Color.White
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
        contentAlignment = Alignment.CenterStart // Ensure thumb aligns correctly
    ) {
        Box(
            modifier = Modifier
                .size(thumbRadius * 2 - 4.dp) // Slightly smaller than height for padding
                .offset(x = thumbOffset)
                .shadow(1.dp, androidx.compose.foundation.shape.CircleShape)
                .background(thumbColor, androidx.compose.foundation.shape.CircleShape)
        )
    }
}

// -----------------------------------------------------------------------------
// Mac Checkbox
// -----------------------------------------------------------------------------
@Composable
fun MacCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val size = 14.dp
    val interactionSource = remember { MutableInteractionSource() }
    
    val backgroundColor = if (checked) SystemBlueLight else MaterialTheme.colorScheme.surface
    val borderColor = if (checked) SystemBlueLight else MaterialTheme.colorScheme.outlineVariant
    val checkmarkColor = Color.White

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp)) // Standard macOS checkbox radius
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
             androidx.compose.material.icons.Icons.Default.Check.let { icon ->
                 androidx.compose.material3.Icon(
                 imageVector = icon,
                 contentDescription = null,
                 tint = checkmarkColor,
                 modifier = Modifier.size(10.dp)
                 )
             }
        }
    }
}

// -----------------------------------------------------------------------------
// Mac Radio Button
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
    val dotColor = SystemBlueLight

    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
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
                    .background(dotColor, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Mac Divider
// -----------------------------------------------------------------------------
@Composable
fun MacDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

// -----------------------------------------------------------------------------
// Mac Icon Button
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
            .clip(RoundedCornerShape(4.dp))
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
// Mac Dropdown
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
    
    Box(modifier = modifier) {
        MacButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedItem)
                
                // Arrows (Up/Down)
                Column(verticalArrangement = Arrangement.Center) {
                   // Simple arrows using canvas or icons. Font awesome has them.
                   // Or just "v" text for now, or canvas drawing triangles.
                   // Native macOS has a double arrow for popups.
                   Text("v", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        if (expanded) {
            Popup(
                onDismissRequest = { expanded = false },
                alignment = Alignment.TopStart
            ) {
                 Box(
                    modifier = Modifier
                        .width(200.dp) // Min width or adaptive?
                        //.width(IntrinsicSize.Max) // Needs core-layout
                        .padding(top = 28.dp) // Offset below button
                        .shadow(4.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        .zIndex(10f)
                 ) {
                     Column {
                         items.forEach { item ->
                             val isSelected = item == selectedItem
                             val itemBg = if (isSelected) SystemBlueLight else Color.Transparent
                             val itemColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                             
                             Box(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .background(itemBg)
                                     .clickable { 
                                         onItemSelected(item)
                                         expanded = false
                                     }
                                     .padding(horizontal = 12.dp, vertical = 6.dp)
                             ) {
                                 Text(
                                     text = item,
                                     style = TextStyle(fontSize = 13.sp, color = itemColor)
                                 )
                             }
                         }
                     }
                 }
            }
        }
    }
}
