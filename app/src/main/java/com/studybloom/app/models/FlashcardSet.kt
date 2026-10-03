package com.studybloom.app.models

import java.io.Serializable

/**
 * Represents a collection of flashcards generated for a specific note/topic.
 * Stored in Firestore under the 'flashcardSets' collection.
 */
data class FlashcardSet(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "",
    val cards: List<Flashcard> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    var masteredCount: Int = 0,
    var reviewCount: Int = 0
) : Serializable
