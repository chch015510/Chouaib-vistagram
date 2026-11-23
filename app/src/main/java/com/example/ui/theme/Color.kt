package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Vistagram Brand & Surface Palette
val IgMagenta = Color(0xFFDD2A7B)
val IgAmber = Color(0xFFF58529)
val IgViolet = Color(0xFF8134AF)
val IgIndigo = Color(0xFF515BD4)
val IgBlue = Color(0xFF0095F6)
val IgHeartRed = Color(0xFFFF3040)
val IgOnlineGreen = Color(0xFF22C55E)

val IgStoryGradient =
  Brush.linearGradient(
    colors = listOf(IgAmber, IgMagenta, IgViolet, IgIndigo)
  )

val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFFAFAFA)
val LightSurfaceVariant = Color(0xFFF1F2F6)
val LightOnBackground = Color(0xFF0F1115)
val LightOnSurfaceVariant = Color(0xFF646B78)
val LightOutline = Color(0xFFE5E7EB)

val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF101015)
val DarkSurfaceVariant = Color(0xFF1C1C24)
val DarkOnBackground = Color(0xFFFAFAFA)
val DarkOnSurfaceVariant = Color(0xFFA1A7B3)
val DarkOutline = Color(0xFF272733)
