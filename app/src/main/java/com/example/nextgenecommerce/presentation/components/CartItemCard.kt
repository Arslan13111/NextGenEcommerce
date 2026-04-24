package com.example.nextgenecommerce.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.CartItem

private fun colorNameToColor(name: String): Color = when (name.lowercase().trim()) {
    "red", "coral red"           -> Color(0xFFEF5350)
    "blue", "navy", "navy blue"  -> Color(0xFF1565C0)
    "green"                      -> Color(0xFF43A047)
    "black"                      -> Color(0xFF111111)
    "white"                      -> Color(0xFFEEEEEE)
    "yellow", "lemon yellow"     -> Color(0xFFFDD835)
    "pink"                       -> Color(0xFFEC407A)
    "purple"                     -> Color(0xFF8E24AA)
    "orange"                     -> Color(0xFFFB8C00)
    "gray", "grey"               -> Color(0xFF757575)
    "brown"                      -> Color(0xFF6D4C41)
    "beige"                      -> Color(0xFFF5F5DC)
    "lilac"                      -> Color(0xFFCE93D8)
    "sand"                       -> Color(0xFFC2B280)
    else                         -> Color(0xFFBDBDBD)
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isFavourite: Boolean,
    onFavouriteToggle: () -> Unit,
    onQuantityIncrease: (CartItem) -> Unit,
    onQuantityDecrease: (CartItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val circleColor by animateColorAsState(
        targetValue = if (isChecked) Color(0xFF111111) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "checkCircle"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isChecked) Color(0xFF111111) else Color(0xFFBDBDBD),
        animationSpec = tween(durationMillis = 200),
        label = "checkBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Animated circular checkbox ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(circleColor)
                .border(1.5.dp, borderColor, CircleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onCheckedChange(!isChecked) },
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // ── Product image card ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = cartItem.productImage,
                contentDescription = cartItem.productName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // "New" badge — top left
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
                    .align(Alignment.TopStart)
            ) {
                Text("New", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            }

            // Heart icon — top right (toggles wishlist)
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .align(Alignment.TopEnd)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onFavouriteToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = if (isFavourite) Color(0xFFEF5350) else Color.Black,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Pagination dots — bottom center
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == 0) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == 0) Color.Black else Color.Black.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }

        // ── Product details ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = cartItem.productName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                fontSize = 14.sp
            )

            if (cartItem.storeName.isNotBlank() && cartItem.storeName != "Default Store") {
                Text(
                    text = cartItem.storeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(colorNameToColor(cartItem.selectedColor))
                        .border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                )
                Text(
                    text = cartItem.selectedColor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Size:  ${cartItem.selectedSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onQuantityDecrease(cartItem) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("−", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = cartItem.quantity.toString(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onQuantityIncrease(cartItem) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Text(
                text = "Rs ${cartItem.price.toInt()}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
        }
    }
}
