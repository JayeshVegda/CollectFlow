package com.jayesh.cashcollect.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextSecondary

/**
 * Typographic primitives.
 *
 * These exist so screens never hardcode a font size again. Previously the app contained
 * roughly twenty different inline `.sp` values (10, 11, 12, 15, 16, 17, 18, 32 …) which is
 * the single biggest reason the UI felt inconsistent rather than designed. Every size here
 * comes from the token scale in `theme/Type.kt`.
 */

/**
 * An ALL-CAPS monospace section header — the "instrument panel" label.
 *
 * @param count optional trailing count, e.g. `PENDING (3)`, dimmed relative to the title.
 */
@Composable
fun AppSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    color: Color = TextSecondary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = AppType.labelMonoLarge,
            color = color
        )
        if (count != null) {
            Text(
                text = "  ($count)",
                style = AppType.labelMono,
                color = TextSecondary
            )
        }
    }
}

/**
 * A right-aligned monospace amount. Tabular figures keep columns of money aligned, which
 * is a Notion rule for financial tables and was previously broken here.
 *
 * Colour is applied to the VALUE only — never to its label.
 */
@Composable
fun AmountText(
    amountPaise: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = AppType.amount,
    color: Color = TextDisplay,
    strikethrough: Boolean = false,
    align: TextAlign = TextAlign.End
) {
    Text(
        text = Paise(amountPaise).toFormattedRupees(),
        modifier = modifier,
        style = style,
        color = color,
        textAlign = align,
        textDecoration = if (strikethrough) TextDecoration.LineThrough else null,
        maxLines = 1
    )
}

/**
 * A two-line stack: a title and a dimmed meta line. The app's most common row body, so it
 * lives here rather than being re-implemented per screen.
 */
@Composable
fun AppRowText(
    title: String,
    meta: String?,
    modifier: Modifier = Modifier,
    titleColor: Color = TextDisplay,
    strikethrough: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = AppType.subheading,
            color = titleColor,
            textDecoration = if (strikethrough) TextDecoration.LineThrough else null,
            maxLines = 1
        )
        if (!meta.isNullOrBlank()) {
            Text(
                text = meta,
                style = AppType.bodySm,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

/** A page title. One per screen, per the three-layer hierarchy rule. */
@Composable
fun AppTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = AppType.heading,
        color = TextDisplay
    )
}