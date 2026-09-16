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

    /** The one hero number per screen. Doto dot-matrix, 48sp. Restricted to 36sp+. */
    val displayHero = TextStyle(
        fontFamily = Doto,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.02).em
    )

    /** Section hero: the big number on a card that is not THE hero. */
    val displaySection = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = TABULAR_FIGURES
    )

    /** Page titles. */
    val heading = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.01).em
    )

    /** Row titles, customer names. */
    val subheading = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 23.sp
    )

    /** Body text. */
    val body = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    /** Secondary body, list meta lines. */
    val bodySm = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.01.em
    )

    /** Timestamps, footnotes. */
    val caption = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.04.em
    )

    /**
     * The ALL-CAPS monospace "instrument panel" label. 11sp with wide tracking.
     * This is the app's most-used label style; it is deliberately small but never
     * smaller than this, and always paired with readable [TextSecondary] or brighter.
     */
    val labelMono = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.08.em
    )

    /** Slightly larger monospace label for section headers. */
    val labelMonoLarge = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.08.em
    )

    /** Monospace amount, standard size. Always right-aligned in columns. */
    val amount = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )

    /** Monospace amount, emphasised (ledger rows). */
    val amountLarge = TextStyle(
        fontFamily = SpaceMono,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 23.sp
    )
}

/**
 * Material3 slots mapped onto the same scale so Material components (buttons, chips,
 * text fields) inherit the system rather than falling back to stock Roboto sizing.
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
    titleSmall = AppType.bodySm.copy(fontWeight = FontWeight.Medium),

    bodyLarge = AppType.body,
    bodyMedium = AppType.bodySm,
    bodySmall = AppType.caption,

    labelLarge = AppType.labelMonoLarge,
    labelMedium = AppType.labelMono,
    labelSmall = AppType.labelMono
)

