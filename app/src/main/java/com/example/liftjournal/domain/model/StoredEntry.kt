package com.example.liftjournal.domain.model

import java.time.LocalDate

/** A journal entry's date and text as stored, without parsing. Used for backups and Undo. */
data class StoredEntry(val date: LocalDate, val text: String)
