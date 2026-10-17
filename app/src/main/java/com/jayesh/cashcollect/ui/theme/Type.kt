package com.jayesh.cashcollect.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.R

/*
 * Typography
 * ----------
 * The Nothing design system specifies three typeface roles, all bundled offline so the
 * app never downloads a font (offline-first) and never depends on the system having one:
 *
 *   Space Grotesk — body, headings, UI. The workhorse.
 *   Space Mono    — data, numbers, ALL-CAPS instrument labels.
 *   Doto          — dot-matrix hero numerals ONLY (36sp+). Never body text.
 *
 * Both families are Colophon Foundry designs sharing DNA with Nothing's own typefaces;
 * Doto is the variable dot-matrix closest to Nothing's NDot 57.
 *
 * Font-discipline budget per screen (Nothing skill): 2 families, 3 sizes, 2 weights.
 * Every value below comes from the token table — do not introduce new sizes in screens.
 * If a screen needs more separation, add Space, not a font size.
 */

private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

private val SpaceMono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold)
)

/** Doto is a single bold instance; it is a display face, not a text face. */
private val Doto = FontFamily(
    Font(R.font.doto_bold, FontWeight.Bold)
)

/** Tabular numerals so amounts line up in columns (Notion rule for financial figures). */
private const val TABULAR_FIGURES = "tnum"

/**
 * Semantic type roles. Screens should use these names — e.g. `AppType.labelMono` —
 * instead of hardcoding `fontSize = 11.sp` inline, which is what previously produced
 * ~20 inconsistent sizes across the app.
 */
object AppType {

    /**
     * Doto dot-matrix numerals, 30sp. Restricted to WHOLE NUMBERS ONLY.
     */
    val displayHero = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.02).em
    )

    /**
     * The money hero — crisp, slim, Apple Wallet style.
     * Space Grotesk Medium at 28sp with tabular figures.
     */
    val displayMoney = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = TABULAR_FIGURES
    )

    /** Section hero: 22sp Medium with tabular figures. */
    val displaySection = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = TABULAR_FIGURES
    )

    /** Page titles: 20sp Medium. */
    val heading = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).em
    )

    /** Row titles, customer names: 15sp Medium. */
    val subheading = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    /** Body text: 14sp Regular. */
    val body = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    /** Secondary body, list meta lines: 13sp Regular. */
    val bodySm = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.01.em
    )

    /** Timestamps, footnotes: 11sp Regular. */
    val caption = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.02.em
    )

    /**
     * Clean, slim Apple-style label in Space Grotesk Medium. 12sp.
     */
    val label = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.02.em
    )

    /**
     * Compact Apple-style label in Space Grotesk Medium. 11sp.
     */
    val labelSm = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.02.em
    )

    /**
     * Monospace label for technical codes or serial indicators.
     */
    val labelMono = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.05.em
    )

    /** Section header monospace label. */
    val labelMonoLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04.em
    )

    /** Amount: 15sp Medium with tabular figures. */
    val amount = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        fontFeatureSettings = TABULAR_FIGURES
    )

    /** Emphasised amount: 17sp Medium with tabular figures. */
    val amountLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        fontFeatureSettings = TABULAR_FIGURES
    )
}

/**
 * Material3 slots mapped onto the slim scale so Material components (buttons, chips,
 * text fields) inherit the slim Apple aesthetic rather than chunky monospace defaults.
 */
val Typography = Typography(
    displayLarge = AppType.displayHero,
    displayMedium = AppType.displaySection,
    displaySmall = AppType.heading,

    headlineLarge = AppType.heading,
    headlineMedium = AppType.heading,
    headlineSmall = AppType.subheading,

    titleLarge = AppType.subheading,
    titleMedium = AppType.body.copy(fontWeight = FontWeight.Medium),
    titleSmall = AppType.label,

    bodyLarge = AppType.body,
    bodyMedium = AppType.bodySm,
    bodySmall = AppType.caption,

    labelLarge = AppType.label,
    labelMedium = AppType.label,
    labelSmall = AppType.labelSm
)

