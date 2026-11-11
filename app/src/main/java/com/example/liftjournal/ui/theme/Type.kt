package com.example.liftjournal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Tabular figures keep numbers aligned, like the prototype's font-variant-numeric: tabular-nums. */
private const val TABULAR_NUMBERS = "tnum"

private val BaseText = TextStyle(fontFeatureSettings = TABULAR_NUMBERS)

/** Text styles with the prototype's sizes. */
object AppText {
    val body = BaseText.copy(fontSize = 15.sp, lineHeight = 22.sp)
    val entryText = BaseText.copy(fontSize = 17.sp, lineHeight = 26.sp)
    val entryDate = BaseText.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.04.em)
    val tabLabel = BaseText.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.12.em)
    val section = BaseText.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.16.em)
    val pageTitle = BaseText.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    val hint = BaseText.copy(fontSize = 12.sp, lineHeight = 17.sp)
    val chip = BaseText.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    val button = BaseText.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
    val small = BaseText.copy(fontSize = 13.sp, lineHeight = 19.sp)
    val statNumber = BaseText.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    val statLabel = BaseText.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em)
    val chartAxis = BaseText.copy(fontSize = 10.sp)
    val chartValue = BaseText.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val goalPercent = BaseText.copy(fontSize = 88.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 92.sp)
    val mono = BaseText.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace)
}

val LiftJournalTypography = Typography(
    bodyLarge = AppText.body,
    bodyMedium = AppText.body,
    bodySmall = AppText.hint,
    labelLarge = AppText.button,
)
