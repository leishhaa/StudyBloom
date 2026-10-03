package com.studybloom.app.models

import java.io.Serializable

/**
 * Represents an individual flashcard with a question and answer.
 * Implements Serializable so it can be passed across Intent extras.
 */
data class Flashcard(
    val question: String = "",
    val answer: String = "",
    var isMastered: Boolean = false
) : Serializable
