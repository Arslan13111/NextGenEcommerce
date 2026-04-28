package com.example.nextgenecommerce.util

import androidx.compose.ui.graphics.Color

object ColorUtils {

    /**
     * Map of common hex codes to display names.
     */
    private val HEX_TO_NAME = mapOf(
        "#111111" to "Black",
        "#000000" to "Black",
        "#EEEEEE" to "White",
        "#FFFFFF" to "White",
        "#E53935" to "Red",
        "#FF0000" to "Red",
        "#1565C0" to "Blue",
        "#0000FF" to "Blue",
        "#2E7D32" to "Green",
        "#008000" to "Green",
        "#FDD835" to "Yellow",
        "#FFFF00" to "Yellow",
        "#EC407A" to "Pink",
        "#FFC0CB" to "Pink",
        "#7B1FA2" to "Purple",
        "#800080" to "Purple",
        "#FB8C00" to "Orange",
        "#FFA500" to "Orange",
        "#757575" to "Gray",
        "#808080" to "Gray",
        "#5D4037" to "Brown",
        "#A52A2A" to "Brown",
        "#D7CCC8" to "Beige",
        "#F5F5DC" to "Beige",
        "#1A237E" to "Navy Blue",
        "#FFFF6B6B" to "Coral Red",
        "#FF6B6B" to "Coral Red",
        "#827717" to "Olive",
        "#7F0000" to "Maroon",
        "#004D40" to "Teal",
        "#B39DDB" to "Lavender",
        "#80CBC4" to "Mint",
        "#F48FB1" to "Rose",
        "#C8B878" to "Khaki",
        "#880E4F" to "Burgundy",
        "#CE93D8" to "Lilac",
        "#C2B280" to "Sand",
        "#455A64" to "Charcoal",
        "#FFB300" to "Mustard",
        "#00ACC1" to "Turquoise",
        "#FF8A65" to "Salmon",
        "#FFF8E1" to "Ivory",
        "#FFFDE7" to "Cream"
    )

    /**
     * Returns a human-readable name for a given color input.
     * If input is a hex code (e.g., #FF0000), it returns the name (e.g., Red).
     * If input is already a name, it returns it as-is (capitalized).
     */
    fun getColorDisplayName(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("#")) {
            val upperHex = trimmed.uppercase()
            // Try direct match
            HEX_TO_NAME[upperHex]?.let { return it }
            
            // Try 6-char match if it's 8-char (remove alpha)
            if (upperHex.length == 9) {
                val sixChar = "#" + upperHex.substring(3)
                HEX_TO_NAME[sixChar]?.let { return it }
            }
            
            return upperHex // Fallback to the code if no name found
        }
        
        // Capitalize first letter of each word for beauty
        return trimmed.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Converts a color name or hex code to a Compose Color object.
     * Fallback to Light Gray if unknown.
     */
    fun parseColor(input: String): Color {
        val trimmed = input.trim()
        if (trimmed.startsWith("#")) {
            return try {
                Color(android.graphics.Color.parseColor(trimmed))
            } catch (e: Exception) {
                Color.LightGray
            }
        }
        
        // Match against some hardcoded names if it's not a hex
        return when (trimmed.lowercase()) {
            "black" -> Color(0xFF111111)
            "white" -> Color(0xFFEEEEEE)
            "red" -> Color(0xFFE53935)
            "blue" -> Color(0xFF1565C0)
            "green" -> Color(0xFF2E7D32)
            "yellow" -> Color(0xFFFDD835)
            "pink" -> Color(0xFFEC407A)
            "purple" -> Color(0xFF7B1FA2)
            "orange" -> Color(0xFFFB8C00)
            "gray", "grey" -> Color(0xFF757575)
            "brown" -> Color(0xFF5D4037)
            "beige" -> Color(0xFFD7CCC8)
            "navy blue", "navy" -> Color(0xFF1A237E)
            "coral red" -> Color(0xFFFF6B6B)
            "olive" -> Color(0xFF827717)
            "maroon" -> Color(0xFF7F0000)
            "teal" -> Color(0xFF004D40)
            "lavender" -> Color(0xFFB39DDB)
            "mint" -> Color(0xFF80CBC4)
            "rose" -> Color(0xFFF48FB1)
            "khaki" -> Color(0xFFC8B878)
            "burgundy" -> Color(0xFF880E4F)
            "lilac" -> Color(0xFFCE93D8)
            "sand" -> Color(0xFFC2B280)
            "charcoal" -> Color(0xFF455A64)
            "mustard" -> Color(0xFFFFB300)
            "turquoise" -> Color(0xFF00ACC1)
            "salmon" -> Color(0xFFFF8A65)
            "ivory" -> Color(0xFFFFF8E1)
            "cream" -> Color(0xFFFFFDE7)
            else -> Color.LightGray
        }
    }
}
