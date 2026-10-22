package com.studybloom.app.services

import com.google.ai.client.generativeai.GenerativeModel
import com.studybloom.app.models.Flashcard
import com.studybloom.app.models.FlashcardSet
import com.studybloom.app.models.Quiz
import com.studybloom.app.models.QuizQuestion
import com.studybloom.app.utils.JsonParserHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AIService {


    var geminiApiKey: String = ""

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
                    You are an academic study assistant.
                    Generate revision flashcards based STRICTLY and ONLY on the provided study notes.
                    Every question and answer MUST be directly supported by the text in the notes.
                    Do NOT invent facts, use generic placeholders (like "Key concept 1"), or add outside information.
                    Extract key definitions, facts, comparisons, and causal relationships.
                    If the notes only contain a few concepts, generate only that number of flashcards (do not pad with filler).
                    
                    Subject: $subject
                    Topic: $noteTitle
                    Notes:
                    $noteContent
                    
                    Return ONLY valid JSON matching this exact structure with NO markdown or explanation:
                    {
                      "title": "$noteTitle",
                      "cards": [
                        {
                          "question": "Specific question testing a fact or definition from the notes",
                          "answer": "Accurate answer directly extracted from the notes"
                        }
                      ]
                    }
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: ""
                if (responseText.isNotBlank()) {
                    val parsed = JsonParserHelper.parseFlashcards(
                        rawJson = responseText,
                        userId = userId,
                        defaultTitle = noteTitle,
                        subject = subject
                    )
                    if (parsed.cards.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Local fallback: extracts real definitions and facts directly from the notes
        return@withContext generateLocalFlashcards(noteTitle, subject, noteContent, userId)
    }

    /**
     * Generates multiple-choice quiz questions based strictly on the provided study notes.
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
                    You are an academic study assistant.
                    Generate multiple-choice quiz questions based STRICTLY and ONLY on the provided study notes.
                    Every question and correct answer MUST be directly supported by the text in the notes.
                    Do NOT invent facts, use generic placeholders, or add outside information.
                    If the notes only contain limited information, generate fewer questions (e.g. 2 to 4) instead of padding with filler.
                    
                    Subject: $subject
                    Topic: $noteTitle
                    Notes:
                    $noteContent
                    
                    Return ONLY valid JSON matching this exact structure with NO markdown or explanation:
                    {
                      "title": "$noteTitle Quiz",
                      "questions": [
                        {
                          "question": "Clear question testing a concept from the notes?",
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
                    Note: correctAnswer must be 0, 1, 2, or 3 corresponding to the correct option index in options.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: ""
                if (responseText.isNotBlank()) {
                    val parsed = JsonParserHelper.parseQuiz(
                        rawJson = responseText,
                        userId = userId,
                        defaultTitle = "$noteTitle Quiz",
                        subject = subject
                    )
                    if (parsed.questions.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Local fallback: constructs questions grounded in the extracted flashcard set
        return@withContext generateLocalQuiz(noteTitle, subject, noteContent, userId)
    }

    /**
     * Local deterministic extractor that identifies definitions, key facts,
     * bullet points, and relationships from ordinary paragraphs.
     * Generates fewer cards if the notes contain few concepts, and avoids generic placeholders.
     */
    fun generateLocalFlashcards(
        title: String,
        subject: String,
        content: String,
        userId: String
    ): FlashcardSet {
        val cards = mutableListOf<Flashcard>()
        val cleanContent = content.trim()

        if (cleanContent.isEmpty()) {
            return FlashcardSet(
                id = "",
                userId = userId,
                title = title,
                subject = subject,
                cards = emptyList(),
                createdAt = System.currentTimeMillis()
            )
        }

        // Extract individual candidate statements from lines, bullet points, and sentences
        val candidateStatements = mutableListOf<String>()

        cleanContent.lines().forEach { rawLine ->
            val line = rawLine.trim()
                .replace(Regex("^[•*\\-–—]+\\s*"), "") // strip bullet markers
                .replace(Regex("^\\d+[.)]\\s*"), "")   // strip numbered list markers
                .trim()

            if (line.isBlank()) return@forEach

            val colonIdx = line.indexOf(':')
            val dashIdx = when {
                line.contains(" - ") -> line.indexOf(" - ")
                line.contains(" — ") -> line.indexOf(" — ")
                line.contains(" – ") -> line.indexOf(" – ")
                else -> -1
            }

            if (colonIdx in 2..50 || dashIdx in 2..50) {
                candidateStatements.add(line)
            } else {
                val sentences = line.split(Regex("(?<=[.!?])\\s+"))
                    .map { it.trim() }
                    .filter { it.length >= 15 }
                if (sentences.isNotEmpty()) {
                    candidateStatements.addAll(sentences)
                } else if (line.length >= 10) {
                    candidateStatements.add(line)
                }
            }
        }

        // Parse candidate statements into high quality Q&A cards
        for (statement in candidateStatements) {
            if (cards.size >= 10) break
            if (statement.length < 12) continue

            val card = parseStatementToFlashcard(statement)
            if (card != null && cards.none { it.question.equals(card.question, ignoreCase = true) }) {
                cards.add(card)
            }
        }

        // If no specific pattern matched, extract factual cards from non-empty statements
        if (cards.isEmpty()) {
            val validStatements = candidateStatements.filter { it.isNotBlank() }
            for (line in validStatements.take(5)) {
                val cleanLine = line.trimEnd('.', '!', '?')
                val question = if (cleanLine.length > 50) {
                    val preview = cleanLine.take(45).substringBeforeLast(' ', "")
                        .ifBlank { cleanLine.take(40) }
                    "Key fact regarding $preview..."
                } else {
                    "What do the notes state about $cleanLine?"
                }
                cards.add(Flashcard(question = question, answer = line))
            }
        }

        // Final fallback: if content was a single short phrase
        if (cards.isEmpty() && cleanContent.isNotBlank()) {
            cards.add(
                Flashcard(
                    question = "Summary of $title in notes:",
                    answer = cleanContent
                )
            )
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

    private fun parseStatementToFlashcard(statement: String): Flashcard? {
        val trimmed = statement.trim()

        // 1. "Term : Definition"
        if (trimmed.contains(':')) {
            val parts = trimmed.split(':', limit = 2)
            val term = parts[0].trim().replace(Regex("^[•*\\-–—\\d.\\s]+"), "")
            val def = parts[1].trim()
            if (term.isNotBlank() && def.length >= 5 && term.split("\\s+".toRegex()).size <= 7) {
                val q = if (term.endsWith("s", ignoreCase = true) && !term.endsWith("ss", ignoreCase = true)) {
                    "What are $term?"
                } else {
                    "What is $term?"
                }
                return Flashcard(question = q, answer = def)
            }
        }

        // 2. "Term - Definition" or "Term — Definition" or "Term – Definition"
        val dashRegex = Regex("\\s+[-—–]\\s+")
        if (dashRegex.containsMatchIn(trimmed)) {
            val parts = trimmed.split(dashRegex, limit = 2)
            val term = parts[0].trim().replace(Regex("^[•*\\-–—\\d.\\s]+"), "")
            val def = parts[1].trim()
            if (term.isNotBlank() && def.length >= 5 && term.split("\\s+".toRegex()).size <= 7) {
                val q = if (term.endsWith("s", ignoreCase = true) && !term.endsWith("ss", ignoreCase = true)) {
                    "What are $term?"
                } else {
                    "What is $term?"
                }
                return Flashcard(question = q, answer = def)
            }
        }

        // 3. "X is defined as Y" / "X refers to Y" / "X means Y" / "X is described as Y"
        val defKeywords = Regex("^(.*?)\\s+(is defined as|refers to|means|is described as)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val defMatch = defKeywords.find(trimmed)
        if (defMatch != null) {
            val term = defMatch.groupValues[1].trim()
            val verb = defMatch.groupValues[2].trim()
            if (term.split("\\s+".toRegex()).size <= 7 && !isPronoun(term)) {
                val question = if (verb.contains("refers", ignoreCase = true)) {
                    "What does $term refer to?"
                } else if (verb.contains("means", ignoreCase = true)) {
                    "What does $term mean?"
                } else {
                    "How is $term defined?"
                }
                return Flashcard(question = question, answer = trimmed)
            }
        }

        // 4. "X is used to Y" / "X is used for Y" / "X serves to Y" / "X functions to Y" / "X is responsible for Y"
        val purposeRegex = Regex("^(.*?)\\s+(is used to|is used for|serves to|functions to|is responsible for)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val purposeMatch = purposeRegex.find(trimmed)
        if (purposeMatch != null) {
            val term = purposeMatch.groupValues[1].trim()
            if (term.split("\\s+".toRegex()).size <= 6 && !isPronoun(term)) {
                return Flashcard(question = "What is the purpose of $term?", answer = trimmed)
            }
        }

        // 5. Comparison: "differs from", "whereas", "in contrast to", "unlike", "compared to"
        val compKeywords = listOf("differs from", "whereas", "in contrast to", "unlike", "compared to", "while")
        for (comp in compKeywords) {
            if (trimmed.contains(comp, ignoreCase = true)) {
                return Flashcard(
                    question = "What comparison or distinction is noted in the text?",
                    answer = trimmed
                )
            }
        }

        // 6. Composition: "consists of", "is composed of", "contains", "comprises"
        val compRegex = Regex("^(.*?)\\s+(consists of|is composed of|contains|comprises)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val compMatch = compRegex.find(trimmed)
        if (compMatch != null) {
            val term = compMatch.groupValues[1].trim()
            val verb = compMatch.groupValues[2].trim()
            if (term.split("\\s+".toRegex()).size <= 6 && !isPronoun(term)) {
                return Flashcard(question = "What $verb $term?", answer = trimmed)
            }
        }

        // 7. General "X is Y" / "X are Y" (when X is a concise subject)
        val isAreRegex = Regex("^(.*?)\\s+(is an?|are|was|were)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val isAreMatch = isAreRegex.find(trimmed)
        if (isAreMatch != null) {
            val subject = isAreMatch.groupValues[1].trim()
            val verb = isAreMatch.groupValues[2].trim()
            val predicate = isAreMatch.groupValues[3].trim()
            if (subject.split("\\s+".toRegex()).size in 1..6 && !isPronoun(subject) && predicate.length >= 10) {
                val q = if (verb.contains("are", ignoreCase = true) || verb.contains("were", ignoreCase = true)) {
                    "What are $subject?"
                } else {
                    "What is $subject?"
                }
                return Flashcard(question = q, answer = trimmed)
            }
        }

        // 8. Informative sentence with identifiable subject (first 2-5 words before first verb/comma)
        val words = trimmed.split("\\s+".toRegex())
        if (words.size >= 5) {
            val subjectCandidate = words.take(4).joinToString(" ").trimEnd(',', ';')
            if (!isPronoun(subjectCandidate)) {
                return Flashcard(
                    question = "According to the notes, what is stated regarding $subjectCandidate?",
                    answer = trimmed
                )
            }
        }

        return null
    }

    private fun isPronoun(text: String): Boolean {
        val firstWord = text.split("\\s+".toRegex()).firstOrNull()?.lowercase() ?: return true
        return firstWord in setOf("it", "this", "these", "those", "there", "that", "he", "she", "they", "we", "you", "i")
    }

    /**
     * Local deterministic quiz generator that creates questions
     * based strictly on the extracted flashcards and uses answers
     * from other cards as realistic distractors.
     */
    fun generateLocalQuiz(
        title: String,
        subject: String,
        content: String,
        userId: String
    ): Quiz {
        val cards = generateLocalFlashcards(title, subject, content, userId).cards
        val questions = mutableListOf<QuizQuestion>()

        val targetCards = cards.take(5)

        for (card in targetCards) {
            val correctAnswer = card.answer

            // Distractors taken from other real notes cards
            val otherAnswers = cards.filter { it.answer != correctAnswer }
                .map { it.answer }
                .shuffled()
                .toMutableList()

            val options = mutableListOf<String>()
            options.add(correctAnswer)

            while (options.size < 4 && otherAnswers.isNotEmpty()) {
                val candidate = otherAnswers.removeAt(0)
                if (!options.contains(candidate)) {
                    options.add(candidate)
                }
            }

            val fallbackDistractors = listOf(
                "An inverse mechanism not described in the notes",
                "A secondary factor with no direct relationship mentioned",
                "Applies only under alternative conditions outside these notes",
                "Not supported by the provided study notes"
            ).shuffled()

            var fallbackIdx = 0
            while (options.size < 4 && fallbackIdx < fallbackDistractors.size) {
                val candidate = fallbackDistractors[fallbackIdx++]
                if (!options.contains(candidate)) {
                    options.add(candidate)
                }
            }

            options.shuffle()
            val correctIndex = options.indexOf(correctAnswer)

            questions.add(
                QuizQuestion(
                    question = card.question,
                    options = options,
                    correctAnswer = correctIndex
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
