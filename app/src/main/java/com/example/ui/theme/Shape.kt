package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Shape Tokens
 * Defined according to https://m3.material.io/blog/building-with-m3-expressive
 * Features exaggerated radii, expressive squircles, and pill containers.
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// Expressive semantic component shapes
val ExpressivePillShape = CircleShape
val ExpressiveCardShape = RoundedCornerShape(24.dp)
val ExpressiveSquircleShape = RoundedCornerShape(22.dp)
val ExpressiveTileShape = RoundedCornerShape(18.dp)
val ExpressiveChipShape = RoundedCornerShape(14.dp)
val ExpressiveBadgeShape = RoundedCornerShape(10.dp)
val ExpressiveDockShape = RoundedCornerShape(32.dp)
val ExpressiveSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
val ExpressiveDialogShape = RoundedCornerShape(28.dp)
