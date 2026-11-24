package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand Core Palette
val FeelkarCoral = Color(0xFFFF3366)
val FeelkarAmber = Color(0xFFFF7A33)
val FeelkarViolet = Color(0xFF7B3FE4)
val FeelkarCyan = Color(0xFF00C2FF)
val FeelkarEmerald = Color(0xFF00C48C)
val FeelkarLiveRed = Color(0xFFFF2D55)

// Light Theme Tokens (Daylight Editorial)
val LightBackground = Color(0xFFFAF9F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF2EFEA)
val LightOnBackground = Color(0xFF121116)
val LightOnSurface = Color(0xFF16151C)
val LightOnSurfaceVariant = Color(0xFF646170)
val LightOutline = Color(0xFFE5E0D8)

// Dark Theme Tokens (Midnight Studio)
val DarkBackground = Color(0xFF0C0B10)
val DarkSurface = Color(0xFF15141C)
val DarkSurfaceVariant = Color(0xFF211F2B)
val DarkOnBackground = Color(0xFFF6F5F8)
val DarkOnSurface = Color(0xFFF1EFF5)
val DarkOnSurfaceVariant = Color(0xFFA29EB2)
val DarkOutline = Color(0xFF2E2B3B)

val FeelkarBrandGradient = Brush.linearGradient(
    colors = listOf(FeelkarCoral, FeelkarAmber, FeelkarViolet)
)

val FeelkarStoryRingBrush = Brush.sweepGradient(
    colors = listOf(FeelkarCoral, FeelkarAmber, FeelkarViolet, FeelkarCyan, FeelkarCoral)
)
