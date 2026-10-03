package com.studybloom.app.models

import java.io.Serializable

/**
 * Stores the performance result of a completed quiz.
 * Stored in Firestore under the 'quizResults' collection.
 */
data class QuizResult(
    val id: String = "",
    val userId: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val subject: String = "",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val percentage: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
