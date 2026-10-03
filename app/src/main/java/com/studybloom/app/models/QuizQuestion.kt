package com.studybloom.app.models

import java.io.Serializable

/**
 * Represents a single multiple-choice question in a quiz.
 * correctAnswer is the 0-based index of the correct option (0, 1, 2, or 3).
 */
data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: Int = 0
) : Serializable
