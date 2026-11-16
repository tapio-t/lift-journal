package com.example.liftjournal.ui.common

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.core.text.htmlEncode
import com.example.liftjournal.ui.theme.LocalPalette

/**
 * A string resource with light markup, as the prototype's readouts use: `<b>` marks an emphasised value
 * (bold, full colour) and `<i>` a muted aside. Arguments are escaped, so exercise names can't inject markup.
 */
@Composable
@ReadOnlyComposable
fun styledStringResource(@StringRes id: Int, vararg args: Any): AnnotatedString {
    val palette = LocalPalette.current
    return LocalResources.current.styledString(
        id,
        *args,
    ).withMarkupColors(emphasis = palette.foreground, aside = palette.muted)
}

/** Formats [id] with HTML-escaped [args] and parses its `<b>` and `<i>` markup. */
fun Resources.styledString(@StringRes id: Int, vararg args: Any): AnnotatedString {
    val escaped = args.map { if (it is String) it.htmlEncode() else it }.toTypedArray()
    return AnnotatedString.fromHtml(getString(id, *escaped))
}

/** Replaces the HTML styles with the app's: bold stays bold and gets [emphasis]; italics become plain [aside]. */
private fun AnnotatedString.withMarkupColors(emphasis: Color, aside: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    spanStyles.forEach { range ->
        when {
            range.item.fontWeight == FontWeight.Bold ->
                addStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = emphasis), range.start, range.end)
            range.item.fontStyle == FontStyle.Italic ->
                addStyle(SpanStyle(color = aside), range.start, range.end)
        }
    }
}
