package com.studybloom.app.services

import com.google.ai.client.generativeai.GenerativeModel
import com.studybloom.app.models.Flashcard
import com.studybloom.app.models.FlashcardSet
import com.studybloom.app.models.Quiz
import com.studybloom.app.models.QuizQuestion
import com.studybloom.app.utils.JsonParserHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service to generate Flashcards and Quizzes from study notes using Gemini AI.
 * Includes both cloud AI invocation and a robust local fallback generator
 * to guarantee the app never crashes during university demonstrations.
 */
object AIService {

    // You can set your Gemini API key here or via Remote Config / BuildConfig
    // If empty or if network fails, the local smart study generator will be used seamlessly.
    var geminiApiKey: String = ""

    /**
     * Generates approximately 10 flashcards from the provided notes text.
     */
    suspend fun generateFlashcards(
        noteTitle: String,
        subject: String,
        noteContent: String,
        userId: String
    ): FlashcardSet = withContext(Dispatchers.IO) {
        if (geminiApiKey.isNotBlank()) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = geminiApiKey
                )

                val prompt = """
                    You are an academic study assistant for the app StudyBloom.
                    Read the following study notes and generate approximately 10 useful revision flashcards.
                    
                    Subject: $subject
                    Topic: $noteTitle
                    Notes:
                    $noteContent
                    
                    Return ONLY valid JSON matching this exact structure with NO markdown or explanation:
                    {
                      "title": "$noteTitle",
                      "cards": [
                        {
                          "question": "Clear question testing a key concept",
                          "answer": "Concise, accurate answer"
                        }
                      ]
                    }
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: ""
                if (responseText.isNotBlank()) {
                    return@withContext JsonParserHelper.parseFlashcards(
                        rawJson = responseText,
                        userId = userId,
                        defaultTitle = noteTitle,
                        subject = subject
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Graceful fallback if Gemini call fails
            }
        }

        // Deterministic local smart generator (Offline / Demo fallback)
        return@withContext generateLocalFlashcards(noteTitle, subject, noteContent, userId)
    }

    /**
     * Generates approximately 5 multiple choice questions from the provided notes text.
     */
    suspend fun generateQuiz(
        noteTitle: String,
        subject: String,
        noteContent: String,
        userId: String
    ): Quiz = withContext(Dispatchers.IO) {
        if (geminiApiKey.isNotBlank()) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = geminiApiKey
                )

                val prompt = """
                    You are an academic study assistant for the app StudyBloom.
                    Read the following study notes and generate approximately 5 multiple-choice quiz questions.
                    
                    Subject: $subject
                    Topic: $noteTitle
                    Notes:
                    $noteContent
                    
                    Return ONLY valid JSON matching this exact structure with NO markdown or explanation:
                    {
                      "title": "$noteTitle Quiz",
                      "questions": [
                        {
                          "question": "Question text here?",
                          "options": [
                            "Option A",
                            "Option B",
                            "Option C",
                            "Option D"
                          ],
                          "correctAnswer": 0
                        }
                      ]
                    }
                    Note: correctAnswer must be 0, 1, 2, or 3 corresponding to the correct option.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: ""
                if (responseText.isNotBlank()) {
                    return@withContext JsonParserHelper.parseQuiz(
                        rawJson = responseText,
                        userId = userId,
                        defaultTitle = "$noteTitle Quiz",
                        subject = subject
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Graceful fallback if Gemini call fails
            }
        }

        // Deterministic local smart generator (Offline / Demo fallback)
        return@withContext generateLocalQuiz(noteTitle, subject, noteContent, userId)
    }

    /**
     * Intelligent local fallback that extracts key points from notes text
     * and produces flashcards if network/API is unavailable.
     */
    private fun generateLocalFlashcards(
        title: String,
        subject: String,
        content: String,
        userId: String
    ): FlashcardSet {
        val sentences = content.split(Regex("[.\\n;]"))
            .map { it.trim() }
            .filter { it.length > 15 }

        val cards = mutableListOf<Flashcard>()

        for ((index, sentence) in sentences.withIndex()) {
            if (cards.size >= 10) break
            when {
                sentence.contains(" is ", ignoreCase = true) -> {
                    val parts = sentence.split(Regex(" is ", RegexOption.IGNORE_CASE), limit = 2)
                    cards.add(Flashcard(question = "What is ${parts[0].trim()}?", answer = parts[1].trim()))
                }
                sentence.contains(" are ", ignoreCase = true) -> {
                    val parts = sentence.split(Regex(" are ", RegexOption.IGNORE_CASE), limit = 2)
                    cards.add(Flashcard(question = "What are ${parts[0].trim()}?", answer = parts[1].trim()))
                }
                sentence.contains(":", ignoreCase = true) -> {
                    val parts = sentence.split(":", limit = 2)
                    cards.add(Flashcard(question = "Define ${parts[0].trim()}", answer = parts[1].trim()))
                }
                else -> {
                    cards.add(Flashcard(question = "Key concept ${index + 1} of $title", answer = sentence))
                }
            }
        }

        // If note was very brief, provide structured study cards based on the topic
        if (cards.isEmpty()) {
            cards.add(Flashcard("What is the core topic of $title?", "Understanding $title within $subject."))
            cards.add(Flashcard("What are the primary principles of $title?", "Key concepts and definitions outlined in $subject."))
            cards.add(Flashcard("Why is $title important?", "It builds fundamental knowledge required for examinations."))
            cards.add(Flashcard("How does $title apply in practice?", "By applying the learned rules to solve revision problems."))
            cards.add(Flashcard("Summary checkpoint for $title", content.ifBlank { "Core review notes." }))
        }

        return FlashcardSet(
            id = "",
            userId = userId,
            title = title,
            subject = subject,
            cards = cards,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Intelligent local fallback that constructs multiple-choice questions
     * from notes text if network/API is unavailable.
     */
    private fun generateLocalQuiz(
        title: String,
        subject: String,
        content: String,
        userId: String
    ): Quiz {
        val cards = generateLocalFlashcards(title, subject, content, userId).cards
        val questions = mutableListOf<QuizQuestion>()

        for ((i, card) in cards.take(5).withIndex()) {
            val correctAns = card.answer
            val options = mutableListOf(
                correctAns,
                "An unrelated process in another field",
                "The inverse of the primary mechanism",
                "A secondary factor with negligible impact"
            ).shuffled()

            val correctIdx = options.indexOf(correctAns)
            questions.add(
                QuizQuestion(
                    question = "Q${i + 1}: ${card.question}",
                    options = options,
                    correctAnswer = correctIdx
                )
            )
        }

        return Quiz(
            id = "",
            userId = userId,
            title = "$title Quiz",
            subject = subject,
            questions = questions,
            createdAt = System.currentTimeMillis()
        )
    }
}
