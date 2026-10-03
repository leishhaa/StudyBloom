package com.studybloom.app.models

import java.io.Serializable

/**
 * Represents an AI-generated multiple choice quiz.
 * Stored in Firestore under the 'quizzes' collection.
 */
data class Quiz(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
