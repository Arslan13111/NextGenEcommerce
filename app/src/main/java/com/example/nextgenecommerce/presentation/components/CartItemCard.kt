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
import androidx.compose.material.icons.outlined.DeleteOutline
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
import com.example.nextgenecommerce.util.ColorUtils

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
                .width(110.dp)
                .height(140.dp)
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
        }

        // ── Product details ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = cartItem.productName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                IconButton(
                    onClick = onFavouriteToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Toggle wishlist",
                        tint = if (isFavourite) Color(0xFFEF5350) else Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(ColorUtils.parseColor(cartItem.selectedColor))
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    )
                    Text(
                        text = ColorUtils.getColorDisplayName(cartItem.selectedColor),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "Size: ${cartItem.selectedSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PKR ${cartItem.price.toInt()}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onQuantityDecrease(cartItem) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    
                    Text(
                        text = cartItem.quantity.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .clickable { onQuantityIncrease(cartItem) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
