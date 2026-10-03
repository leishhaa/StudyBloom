package com.studybloom.app.models

import java.io.Serializable

enum class LibraryItemType {
    NOTE,
    FLASHCARDS,
    QUIZ
}

/**
 * Unified representation for items shown in the LibraryFragment RecyclerView.
 * Holds common fields and a reference to the specific payload.
 */
data class LibraryItem(
    val id: String,
    val title: String,
    val subject: String,
    val type: LibraryItemType,
    val timestamp: Long,
    val note: Note? = null,
    val flashcardSet: FlashcardSet? = null,
    val quiz: Quiz? = null
) : Serializable
