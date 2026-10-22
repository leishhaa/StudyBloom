package com.studybloom.app

import com.studybloom.app.services.AIService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIServiceTest {

    @Test
    fun testDefinitionsExtractedProperly() {
        val notes = """
            DNS: Domain Name System translates hostnames into IP addresses.
            Latency refers to the delay in transmitting packets across the network.
            TCP is connection-oriented and provides reliable data transmission.
        """.trimIndent()

        val set = AIService.generateLocalFlashcards(
            title = "Computer Networks",
            subject = "Networking",
            content = notes,
            userId = "user_test"
        )

        assertEquals("Computer Networks", set.title)
        assertEquals(3, set.cards.size)

        // Ensure no generic placeholders like "Key concept" exist
        set.cards.forEach { card ->
            assertFalse(card.question.contains("Key concept", ignoreCase = true))
            assertFalse(card.question.contains("core topic", ignoreCase = true))
            assertTrue(card.answer.isNotBlank())
        }

        // Verify specific extracted questions
        assertTrue(set.cards.any { it.question.contains("DNS", ignoreCase = true) })
        assertTrue(set.cards.any { it.question.contains("Latency", ignoreCase = true) })
        assertTrue(set.cards.any { it.question.contains("TCP", ignoreCase = true) })
    }

    @Test
    fun testFewConceptsGeneratesFewerCardsWithoutPadding() {
        val notes = "Mitochondria: The powerhouse of the cell responsible for generating ATP."

        val set = AIService.generateLocalFlashcards(
            title = "Biology",
            subject = "Science",
            content = notes,
            userId = "user_test"
        )

        // Only 1 definition present -> only 1 card generated (no filler padding)
        assertEquals(1, set.cards.size)
        assertTrue(set.cards[0].question.contains("Mitochondria"))
        assertEquals("The powerhouse of the cell responsible for generating ATP.", set.cards[0].answer)
    }

    @Test
    fun testQuizGenerationGroundedInCards() {
        val notes = """
            TCP: Transmission Control Protocol ensuring reliable transmission.
            UDP: User Datagram Protocol providing lightweight transmission without acknowledgments.
        """.trimIndent()

        val quiz = AIService.generateLocalQuiz(
            title = "Transport Layer",
            subject = "Networking",
            content = notes,
            userId = "user_test"
        )

        assertEquals(2, quiz.questions.size)
        quiz.questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue(q.correctAnswer in 0..3)
            val correct = q.options[q.correctAnswer]
            assertTrue(correct.isNotBlank())
        }
    }
}
