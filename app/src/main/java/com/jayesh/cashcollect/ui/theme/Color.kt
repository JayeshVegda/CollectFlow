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

// ---------------------------------------------------------------------------
// Semantic layer
// ---------------------------------------------------------------------------
// Screens should reference these roles rather than raw hex, so the palette can be
// tuned in one place. Contrast figures below are against the #000000 canvas.

/** Page background. */
val SurfaceBase = NothingBlack
/** Default card / ledger row. */
val SurfaceCard = NothingCard
/** Raised card, pressed states, sheets. */
val SurfaceRaised = NothingCardRaised
/** Decorative divider only. Never the sole signal for a boundary. */
val SurfaceDivider = NothingBorder
/** Intentional edge — used sparingly, not on every card. */
val SurfaceEdge = NothingBorderVisible

/** Body text. #E8E8E8 on #000 = 16.5:1. */
val TextPrimary = NothingTextPrimary
/** Labels, captions, metadata. #999999 on #000 ≈ 7.4:1 — passes AA. */
val TextSecondary = NothingGray
/**
 * Tertiary content that must still be *readable* (empty states, helper lines).
 * #A1A1A1 on #000 ≈ 9:1.
 *
 * This exists because [NothingMuted] (#666666) measures ≈3.7:1 and therefore fails the
 * 4.5:1 WCAG AA floor for body text — yet it was being used for real content such as the
 * History empty state. Use this for anything a person has to read.
 */
val TextTertiary = Color(0xFFA1A1A1)
/** Hero numbers and headlines. #FFFFFF on #000 = 21:1. */
val TextDisplay = NothingWhite
/** Disabled / decorative ONLY (~3.7:1, fails AA). Never readable content. */
val TextDisabled = NothingMuted
