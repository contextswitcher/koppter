package io.github.koppor.koppter.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.koppor.koppter.storage.ColorTheme

/** The colors of the light or the dark variant of a published palette, named by their use in the application. */
private class Palette(
    val dark: Boolean,
    val background: Color,
    /** Cards and panels. */
    val raised: Color,
    /** Labels without a meaning of their own and marked areas. */
    val variant: Color,
    val text: Color,
    val subtle: Color,
    /** Buttons, selected controls and a task that is due today. */
    val accent: Color,
    /** Errors, an overdue task and priority A. */
    val red: Color,
    /** Priority B. */
    val orange: Color,
    /** Priority C. */
    val yellow: Color,
    val project: Color,
    val context: Color,
)

// Nord: https://www.nordtheme.com/docs/colors-and-palettes
private val NORD_LIGHT = Palette(
    dark = false,
    background = Color(0xFFECEFF4), // nord6
    raised = Color(0xFFE5E9F0), // nord5
    variant = Color(0xFFD8DEE9), // nord4
    text = Color(0xFF2E3440), // nord0
    subtle = Color(0xFF4C566A), // nord3
    accent = Color(0xFF5E81AC), // nord10
    red = Color(0xFFBF616A), // nord11
    orange = Color(0xFFD08770), // nord12
    yellow = Color(0xFFEBCB8B), // nord13
    project = Color(0xFFA3BE8C), // nord14
    context = Color(0xFFB48EAD), // nord15
)
private val NORD_DARK = Palette(
    dark = true,
    background = Color(0xFF2E3440), // nord0
    raised = Color(0xFF3B4252), // nord1
    variant = Color(0xFF434C5E), // nord2
    text = Color(0xFFECEFF4), // nord6
    subtle = Color(0xFFD8DEE9), // nord4
    accent = Color(0xFF88C0D0), // nord8
    red = Color(0xFFBF616A),
    orange = Color(0xFFD08770),
    yellow = Color(0xFFEBCB8B),
    project = Color(0xFFA3BE8C),
    context = Color(0xFFB48EAD),
)

// Everforest with medium contrast: https://github.com/sainnhe/everforest/blob/master/palette.md
private val EVERFOREST_LIGHT = Palette(
    dark = false,
    background = Color(0xFFFDF6E3), // bg0
    raised = Color(0xFFF4F0D9), // bg1
    variant = Color(0xFFE6E2CC), // bg3
    text = Color(0xFF5C6A72), // fg
    // The greys of the light variant are too faint for text.
    subtle = Color(0xFF5C6A72), // fg
    accent = Color(0xFF8DA101), // green
    red = Color(0xFFF85552),
    orange = Color(0xFFF57D26),
    yellow = Color(0xFFDFA000),
    project = Color(0xFF3A94C5), // blue
    context = Color(0xFFDF69BA), // purple
)
private val EVERFOREST_DARK = Palette(
    dark = true,
    background = Color(0xFF2D353B), // bg0
    raised = Color(0xFF343F44), // bg1
    variant = Color(0xFF475258), // bg3
    text = Color(0xFFD3C6AA), // fg
    subtle = Color(0xFF9DA9A0), // grey2
    accent = Color(0xFFA7C080), // green
    red = Color(0xFFE67E80),
    orange = Color(0xFFE69875),
    yellow = Color(0xFFDBBC7F),
    project = Color(0xFF7FBBB3), // blue
    context = Color(0xFFD699B6), // purple
)

// The themes of the task manager tuxedo, which have one variant each: https://github.com/webstonehq/tuxedo/blob/main/src/theme.rs
// Its color dim is meant for completed tasks and is too faint for labels; subtle is its status_fg instead.
// Priorities A, B and C take pri_a, pri_b and pri_c, whatever their hue.
private val MUTED_SLATE = Palette(
    dark = true,
    background = Color(0xFF1A1D23), // bg
    raised = Color(0xFF1F232B), // panel
    variant = Color(0xFF2A2F38), // border
    text = Color(0xFFC8CCD4), // fg
    subtle = Color(0xFFA8B0BC), // status_fg
    accent = Color(0xFF8AA9C9),
    red = Color(0xFFE07A7A), // pri_a
    orange = Color(0xFFD4B06A), // pri_b
    yellow = Color(0xFF7AA67A), // pri_c
    project = Color(0xFF7FB3A8),
    context = Color(0xFFC89A6E),
)
private val DAWN = Palette(
    dark = false,
    background = Color(0xFFFAF6F0),
    raised = Color(0xFFF3EDE2),
    variant = Color(0xFFE0D6C4),
    text = Color(0xFF3D3528),
    subtle = Color(0xFF5A4F3D),
    accent = Color(0xFFA35D3A),
    red = Color(0xFFB8483A),
    orange = Color(0xFFA3722A),
    yellow = Color(0xFF5A7A3A),
    project = Color(0xFF3A7A6A),
    context = Color(0xFFA35D3A),
)
private val MATRIX = Palette(
    dark = true,
    background = Color(0xFF0A120A),
    raised = Color(0xFF0F1A0F),
    variant = Color(0xFF1A2A1A),
    text = Color(0xFF7FCC7F),
    subtle = Color(0xFF7FCC7F),
    accent = Color(0xFF9FFF9F),
    red = Color(0xFFFF8C8C),
    orange = Color(0xFFFFD66E),
    yellow = Color(0xFF9FFF9F),
    project = Color(0xFF9FFF9F),
    context = Color(0xFFFFB56E),
)

/** The contrast ratio of two colors as WCAG defines it, from 1 to 21. */
internal fun contrast(a: Color, b: Color): Float {
    val (low, high) = listOf(a.luminance(), b.luminance()).sorted()
    return (high + 0.05f) / (low + 0.05f)
}

/**
 * The Material color roles filled with the colors of a palette.
 * The palettes have no colors for containers, so a container is the color of its role mixed into the color of the cards.
 */
private fun palette(theme: ColorTheme, dark: Boolean) = when (theme) {
    ColorTheme.NORD -> if (dark) NORD_DARK else NORD_LIGHT
    ColorTheme.EVERFOREST -> if (dark) EVERFOREST_DARK else EVERFOREST_LIGHT
    ColorTheme.MUTED_SLATE -> MUTED_SLATE
    ColorTheme.DAWN -> DAWN
    ColorTheme.MATRIX -> MATRIX
}

/** The green of the palette, for what is fine; the Material color scheme has no role for it. */
fun fineColor(theme: ColorTheme, dark: Boolean): Color = palette(theme, dark).project

/** The color for a state that needs no attention, see [fineColor]. */
val LocalFineColor = staticCompositionLocalOf { Color.Unspecified }

// [impl->dsn~color-themes~1]
fun colorScheme(theme: ColorTheme, dark: Boolean): ColorScheme {
    return with(palette(theme, dark)) {
        fun container(color: Color) = color.copy(alpha = 0.3f).compositeOver(raised)

        // The text on a color of the palette is the lighter or the darker end of the palette, whichever reads better.
        fun on(color: Color) = if (contrast(background, color) > contrast(text, color)) background else text
        (if (dark) darkColorScheme() else lightColorScheme()).copy(
            primary = accent,
            onPrimary = on(accent),
            primaryContainer = container(accent),
            onPrimaryContainer = text,
            inversePrimary = accent,
            secondary = yellow,
            onSecondary = on(yellow),
            secondaryContainer = container(context),
            onSecondaryContainer = text,
            tertiary = orange,
            onTertiary = on(orange),
            tertiaryContainer = container(project),
            onTertiaryContainer = text,
            error = red,
            onError = on(red),
            errorContainer = container(red),
            onErrorContainer = text,
            background = background,
            onBackground = text,
            surface = background,
            onSurface = text,
            surfaceVariant = variant,
            onSurfaceVariant = subtle,
            surfaceTint = accent,
            inverseSurface = text,
            inverseOnSurface = background,
            outline = subtle.copy(alpha = 0.6f).compositeOver(background),
            outlineVariant = variant,
            surfaceDim = raised,
            surfaceBright = background,
            surfaceContainerLowest = background,
            surfaceContainerLow = raised,
            surfaceContainer = raised,
            surfaceContainerHigh = raised,
            surfaceContainerHighest = raised,
        )
    }
}

/**
 * The type scale of Material with two sizes instead of its nine: the medium body size for texts, fields, buttons and titles,
 * and the small body size for labels and remarks. A title differs from a text by its weight.
 * The many sizes of the scale are made for phones and look restless and coarse in a window on a desktop.
 * The medium size also keeps a field of one line at its minimum height.
 * Compose measures an empty line higher than a filled one, so a field of the default size shrinks with the first character typed.
 */
// [impl->dsn~main-screen~1]
val compactTypography: Typography = Typography().run {
    // The spacing of the letters belongs to the size: with that of its own, a style would still look like another size.
    fun TextStyle.sizedAs(other: TextStyle) = copy(fontSize = other.fontSize, lineHeight = other.lineHeight, letterSpacing = other.letterSpacing)
    fun TextStyle.text() = sizedAs(bodyMedium)
    fun TextStyle.small() = sizedAs(bodySmall)
    Typography(
        displayLarge.text(), displayMedium.text(), displaySmall.text(),
        headlineLarge.text(), headlineMedium.text(), headlineSmall.text(),
        // The large title has the weight of a text in the scale, which only its size told apart.
        titleLarge.text().copy(fontWeight = FontWeight.Bold), titleMedium.text(), titleSmall.text(),
        bodyLarge.text(), bodyMedium, bodySmall,
        labelLarge.text(), labelMedium.small(), labelSmall.small(),
    )
}
