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

// Material 3 Expressive Grouped List Card shape tokens
val ExpressiveGroupedOuterRadius = 24.dp
val ExpressiveGroupedInnerRadius = 4.dp

/**
 * Returns the Material 3 grouped list card shape for an item at [index] within a group of [count] items.
 * - Single item: fully rounded (24.dp) on all corners.
 * - First (top) item: rounded top corners (24.dp), small inner bottom corners (4.dp).
 * - Middle items: small inner corners (4.dp) on all sides.
 * - Last (bottom) item: small inner top corners (4.dp), rounded bottom corners (24.dp).
 */
fun groupedListCardShape(index: Int, count: Int): RoundedCornerShape {
    return when {
        count <= 1 -> RoundedCornerShape(ExpressiveGroupedOuterRadius)
        index == 0 -> RoundedCornerShape(
            topStart = ExpressiveGroupedOuterRadius,
            topEnd = ExpressiveGroupedOuterRadius,
            bottomStart = ExpressiveGroupedInnerRadius,
            bottomEnd = ExpressiveGroupedInnerRadius
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = ExpressiveGroupedInnerRadius,
            topEnd = ExpressiveGroupedInnerRadius,
            bottomStart = ExpressiveGroupedOuterRadius,
            bottomEnd = ExpressiveGroupedOuterRadius
        )
        else -> RoundedCornerShape(ExpressiveGroupedInnerRadius)
    }
}

