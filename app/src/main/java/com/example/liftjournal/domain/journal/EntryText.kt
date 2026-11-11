package com.example.liftjournal.domain.journal

/**
 * Puts [added] below [existing], used when merging entries on one date (change date, import, undo).
 * Blank sides are dropped so no empty lines pile up.
 */
fun appendEntryText(existing: String, added: String): String {
    val top = existing.trimEnd()
    val bottom = added.trim('\n', '\r', ' ', '\t')
    if (top.isBlank()) return bottom
    if (bottom.isBlank()) return top
    return "$top\n$bottom"
}
