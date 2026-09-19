package com.jayesh.cashcollect.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.Radius
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.SurfaceBase
import com.jayesh.cashcollect.ui.theme.SurfaceDivider
import com.jayesh.cashcollect.ui.theme.SurfaceEdge
import com.jayesh.cashcollect.ui.theme.TextDisabled
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextSecondary

/*
 * Controls
 * --------
 * These exist because the same button, text field and screen title were re-specified by hand in
 * every screen — the pill radius, the container colour, the 11sp monospace label, the focus border.
 * Twenty-odd near-identical copies is exactly how a design system drifts: no single edit fixes them,
 * and each copy is a chance to differ by a dp or a shade.
 *
 * Android's own guidance on custom design systems is to wrap the Material component and expose the
 * themed values through it, rather than hardcoding those values at every call site. That is what
 * these are. Screens should use these instead of a raw `Button` / `OutlinedTextField`.
 */

/** The one top-bar title style. Every screen's app bar uses it, so the titles cannot drift apart. */
@Composable
fun AppScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = AppType.labelMonoLarge,
        color = TextDisplay
    )
}

/**
 * The primary action: a white pill with a black label.
 *
 * This is the only filled control in the system, which is what makes it read as *the* action on a
 * screen. Label and icon inherit `contentColor`, so the disabled state dims correctly instead of
 * staying high-contrast black.
 */
@Composable
fun AppPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(Space.touchTarget),
        enabled = enabled,
        shape = RoundedCornerShape(Radius.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = TextDisplay,
            contentColor = NothingBlack,
            disabledContainerColor = SurfaceDivider,
            disabledContentColor = TextDisabled
        ),
        contentPadding = PaddingValues(horizontal = Space.md, vertical = 0.dp)
    ) {
        ButtonGlyph(icon)
        Text(text = label, style = AppType.labelMono)
    }
}

/**
 * A secondary or destructive action: hairline outline, no fill.
 *
 * @param accent label and icon colour. Pass
 *   [com.jayesh.cashcollect.ui.theme.NothingRed] for a destructive action — the only role red is
 *   allowed to play.
 */
@Composable
fun AppOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = TextSecondary,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(Space.touchTarget),
        enabled = enabled,
        shape = RoundedCornerShape(Radius.pill),
        border = BorderStroke(1.dp, SurfaceEdge),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = accent,
            disabledContentColor = TextDisabled
        ),
        contentPadding = PaddingValues(horizontal = Space.md, vertical = 0.dp)
    ) {
        ButtonGlyph(icon)
        Text(text = label, style = AppType.labelMono)
    }
}

@Composable
private fun ButtonGlyph(icon: ImageVector?) {
    if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(Space.xs))
    }
}

/**
 * The app's text field.
 *
 * One place decides the focus colour, the label style, the corner radius and the keyboard type, so a
 * field in Settings looks and behaves like a field anywhere else.
 *
 * @param singleLine pass `false` for the message-template editor; keep `minLines` at 1 when
 *   `singleLine` is true, which is the only combination Compose accepts.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label.uppercase(), style = AppType.labelMono, color = TextSecondary) },
        placeholder = placeholder?.let { hint ->
            { Text(text = hint, style = AppType.body, color = TextDisabled) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(Radius.chip),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceBase,
            unfocusedContainerColor = SurfaceBase,
            focusedBorderColor = TextDisplay,
            unfocusedBorderColor = SurfaceEdge,
            focusedTextColor = TextDisplay,
            unfocusedTextColor = TextDisplay,
            cursorColor = TextDisplay,
            focusedLabelColor = TextDisplay,
            unfocusedLabelColor = TextSecondary
        )
    )
}