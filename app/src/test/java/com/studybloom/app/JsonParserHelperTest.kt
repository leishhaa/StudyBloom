package com.studybloom.app

import com.studybloom.app.utils.JsonParserHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for JsonParserHelper to demonstrate JUnit testing in Android.
 * Verifies that structured JSON strings from Gemini AI are parsed correctly.
 */
class JsonParserHelperTest {

    @Test
    fun testParseFlashcardsValidJson() {
        val sampleJson = """
            {
              "title": "Photosynthesis",
              "cards": [
                {
                  "question": "What pigment absorbs light in plant cells?",
                  "answer": "Chlorophyll"
                },
                {
                  "question": "Where does the Calvin cycle take place?",
                  "answer": "In the stroma of chloroplasts"
                }
              ]
            }
        """.trimIndent()

        val set = JsonParserHelper.parseFlashcards(
            rawJson = sampleJson,
            userId = "test_user_1",
            defaultTitle = "Fallback Title",
            subject = "Biology"
        )

        assertEquals("Photosynthesis", set.title)
        assertEquals("Biology", set.subject)
        assertEquals(2, set.cards.size)
        assertEquals("What pigment absorbs light in plant cells?", set.cards[0].question)
        assertEquals("Chlorophyll", set.cards[0].answer)
    }

    @Test
    fun testParseQuizValidJson() {
        val sampleJson = """
            {
              "title": "Cell Biology Quiz",
              "questions": [
                {
                  "question": "Which organelle is the powerhouse of the cell?",
                  "options": [
                    "Nucleus",
                    "Mitochondria",
                    "Ribosome",
                    "Golgi apparatus"
                  ],
                  "correctAnswer": 1
                }
              ]
            }
        """.trimIndent()

        val quiz = JsonParserHelper.parseQuiz(
            rawJson = sampleJson,
            userId = "test_user_1",
            defaultTitle = "Default Quiz",
            subject = "Biology"
        )

        assertEquals("Cell Biology Quiz", quiz.title)
        assertEquals(1, quiz.questions.size)
        val q1 = quiz.questions[0]
        assertEquals("Which organelle is the powerhouse of the cell?", q1.question)
        assertEquals(4, q1.options.size)
        assertEquals(1, q1.correctAnswer)
        assertEquals("Mitochondria", q1.options[q1.correctAnswer])
    }

    @Test
    fun testCleanJsonResponseWithMarkdownBackticks() {
        val raw = "```json\n{\"title\": \"Markdown Test\", \"cards\": []}\n```"
        val cleaned = JsonParserHelper.cleanJsonResponse(raw)
        assertEquals("{\"title\": \"Markdown Test\", \"cards\": []}", cleaned)
    }
}
