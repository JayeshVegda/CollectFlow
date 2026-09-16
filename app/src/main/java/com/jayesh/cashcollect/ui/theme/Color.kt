package com.jayesh.cashcollect.ui.theme

import androidx.compose.ui.graphics.Color

// Nothing OS Official Design Tokens (Monochromatic OLED Canvas)
val NothingBlack = Color(0xFF000000)          // Pure OLED Black
val NothingCard = Color(0xFF111111)           // Surface card
val NothingCardRaised = Color(0xFF1A1A1A)     // Surface raised
val NothingBorder = Color(0xFF222222)         // Subtle border / divider
val NothingBorderVisible = Color(0xFF333333)  // Visible wireframe border

// Accent (Signal light - urgent / interrupt only)
val NothingRed = Color(0xFFD71921)            // Nothing Red
val NothingRedBg = Color(0x26D71921)          // Accent subtle tint
val NothingRedBorder = Color(0x66D71921)

// Monochrome Text Hierarchy
val NothingWhite = Color(0xFFFFFFFF)          // Hero text / display (100%)
val NothingTextPrimary = Color(0xFFE8E8E8)    // Body text (90%)
val NothingGray = Color(0xFF999999)           // Labels / secondary (60%)
val NothingMuted = Color(0xFF666666)          // Disabled / hints (40%)

// Data Status Colors (applied to numeric values, not backgrounds)
val NothingAmber = Color(0xFFD4A843)          // Caution / unsent warning
val NothingAmberBg = Color(0x1AD4A843)
val NothingAmberBorder = Color(0x4DD4A843)

val NothingGreen = Color(0xFF4A9E5C)          // Confirmed success
val NothingGreenBg = Color(0x1A4A9E5C)
val NothingGreenBorder = Color(0x4D4A9E5C)

// Liquid Glass layer — translucent fills blended over the OLED black canvas.
// Kept at low alpha so the monochrome Nothing hierarchy still reads clearly.
val NothingGlass = Color(0x12FFFFFF)          // Base frosted fill (~7%)
val NothingGlassRaised = Color(0x1CFFFFFF)    // Raised frosted fill (~11%)
val NothingGlassBorder = Color(0x2EFFFFFF)    // 1px glass edge (~18%)
val NothingGlassHighlight = Color(0x24FFFFFF) // Top-left sheen (~14%)
val NothingGlassTint = Color(0x08FFFFFF)      // Ultra-subtle wash (~3%)
val NothingScrim = Color(0x99000000)          // Sheet backdrop dim
