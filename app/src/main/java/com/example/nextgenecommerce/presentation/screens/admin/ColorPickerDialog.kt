package com.example.nextgenecommerce.presentation.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PresetColor(val name: String, val color: Color)

val FASHION_PRESET_COLORS = listOf(
    PresetColor("Black",     Color(0xFF111111)),
    PresetColor("White",     Color(0xFFEEEEEE)),
    PresetColor("Red",       Color(0xFFE53935)),
    PresetColor("Blue",      Color(0xFF1565C0)),
    PresetColor("Green",     Color(0xFF2E7D32)),
    PresetColor("Yellow",    Color(0xFFFDD835)),
    PresetColor("Pink",      Color(0xFFEC407A)),
    PresetColor("Purple",    Color(0xFF7B1FA2)),
    PresetColor("Orange",    Color(0xFFFB8C00)),
    PresetColor("Gray",      Color(0xFF757575)),
    PresetColor("Brown",     Color(0xFF5D4037)),
    PresetColor("Beige",     Color(0xFFD7CCC8)),
    PresetColor("Navy Blue", Color(0xFF1A237E)),
    PresetColor("Coral Red", Color(0xFFFF6B6B)),
    PresetColor("Olive",     Color(0xFF827717)),
    PresetColor("Maroon",    Color(0xFF7F0000)),
    PresetColor("Teal",      Color(0xFF004D40)),
    PresetColor("Lavender",  Color(0xFFB39DDB)),
    PresetColor("Mint",      Color(0xFF80CBC4)),
    PresetColor("Rose",      Color(0xFFF48FB1)),
    PresetColor("Khaki",     Color(0xFFC8B878)),
    PresetColor("Burgundy",  Color(0xFF880E4F)),
    PresetColor("Lilac",     Color(0xFFCE93D8)),
    PresetColor("Sand",      Color(0xFFC2B280)),
    PresetColor("Charcoal",  Color(0xFF455A64)),
    PresetColor("Mustard",   Color(0xFFFFB300)),
    PresetColor("Turquoise", Color(0xFF00ACC1)),
    PresetColor("Salmon",    Color(0xFFFF8A65)),
    PresetColor("Ivory",     Color(0xFFFFF8E1)),
    PresetColor("Cream",     Color(0xFFFFFDE7)),
)

fun colorNameToPreview(name: String): Color {
    if (name.startsWith("#")) {
        return try { Color(android.graphics.Color.parseColor(name)) } catch (_: Exception) { Color(0xFFBDBDBD) }
    }
    return FASHION_PRESET_COLORS.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }?.color
        ?: when (name.lowercase().trim()) {
            "red", "coral red"           -> Color(0xFFE53935)
            "blue", "navy", "navy blue"  -> Color(0xFF1565C0)
            "green"                      -> Color(0xFF2E7D32)
            "black"                      -> Color(0xFF111111)
            "white"                      -> Color(0xFFEEEEEE)
            "yellow", "lemon yellow"     -> Color(0xFFFDD835)
            "pink"                       -> Color(0xFFEC407A)
            "purple"                     -> Color(0xFF7B1FA2)
            "orange"                     -> Color(0xFFFB8C00)
            "gray", "grey"               -> Color(0xFF757575)
            "brown"                      -> Color(0xFF5D4037)
            "beige"                      -> Color(0xFFD7CCC8)
            "lilac"                      -> Color(0xFFCE93D8)
            "sand"                       -> Color(0xFFC2B280)
            else                         -> Color(0xFFBDBDBD)
        }
}

@Composable
fun ColorPickerDialog(
    currentColorName: String,
    onDismiss: () -> Unit,
    onColorSelected: (colorName: String) -> Unit
) {
    var selectedName by remember { mutableStateOf(currentColorName) }
    var hexInput by remember { mutableStateOf(if (currentColorName.startsWith("#")) currentColorName else "") }
    var useCustomHex by remember { mutableStateOf(currentColorName.startsWith("#")) }

    val previewColorValue: Color = when {
        useCustomHex && hexInput.isNotBlank() -> {
            try {
                val h = if (hexInput.startsWith("#")) hexInput else "#$hexInput"
                Color(android.graphics.Color.parseColor(h))
            } catch (_: Exception) { Color(0xFFBDBDBD) }
        }
        else -> colorNameToPreview(selectedName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Color", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Current selection preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(previewColorValue)
                            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                    )
                    Column {
                        Text(
                            when {
                                useCustomHex && hexInput.isNotBlank() -> hexInput
                                selectedName.isNotBlank() -> selectedName
                                else -> "None selected"
                            },
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Selected color",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Text(
                    "Preset Colors",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 260.dp)
                ) {
                    items(FASHION_PRESET_COLORS) { preset ->
                        val isSelected = !useCustomHex && selectedName.equals(preset.name, ignoreCase = true)
                        // brightness check for icon tint
                        val lum = 0.299f * preset.color.red + 0.587f * preset.color.green + 0.114f * preset.color.blue
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.clickable {
                                selectedName = preset.name
                                hexInput = ""
                                useCustomHex = false
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(preset.color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check, null,
                                        tint = if (lum > 0.5f) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                preset.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                maxLines = 1,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Text(
                    "Custom Hex Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (useCustomHex && hexInput.isNotBlank()) previewColorValue
                                else Color(0xFFBDBDBD)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    )
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input.take(9)
                            useCustomHex = input.isNotBlank()
                            if (input.isNotBlank()) selectedName = ""
                        },
                        label = { Text("e.g. #FF5733") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val name = if (useCustomHex && hexInput.isNotBlank()) {
                    if (hexInput.startsWith("#")) hexInput else "#$hexInput"
                } else selectedName
                if (name.isNotBlank()) onColorSelected(name)
            }) { Text("Apply") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
