package com.studybloom.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studybloom.app.R
import com.studybloom.app.databinding.ActivityNotesBinding
import com.studybloom.app.models.Note
import com.studybloom.app.services.AIService
import com.studybloom.app.services.FirebaseService
import com.studybloom.app.utils.SharedPrefsHelper
import kotlinx.coroutines.launch

/**
 * NotesActivity:
 * Allows students to write or paste notes, save them to Cloud Firestore,
 * and generate Flashcards or Quizzes.
 * Demonstrates:
 * - Cloud Firestore document creation & updates
 * - Calling AI / Study Generation Service with Kotlin Coroutines
 * - Input validation & progressive UI states
 * - Explicit Intents
 */
class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private lateinit var prefs: SharedPrefsHelper

    private var existingNoteId: String = ""

    companion object {
        const val EXTRA_INITIAL_ACTION = "extra_initial_action"
        const val ACTION_GENERATE_FLASHCARDS = "action_flashcards"
        const val ACTION_GENERATE_QUIZ = "action_quiz"
        const val EXTRA_EXISTING_NOTE = "extra_existing_note"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPrefsHelper(this)

        // Set default subject from SharedPreferences
        binding.etSubject.setText(prefs.lastSubject)

        setupListeners()
        checkForExistingNote()
        configureScreenMode()

        // Focus notes input box if arrived via quick action
        if (intent.hasExtra(EXTRA_INITIAL_ACTION)) {
            binding.etNotesContent.requestFocus()
        }
    }

    private fun configureScreenMode() {
        val action = intent.getStringExtra(EXTRA_INITIAL_ACTION)
        val isQuizMode = action.equals(ACTION_GENERATE_QUIZ, ignoreCase = true) ||
                action.equals("quiz", ignoreCase = true)

        if (isQuizMode) {
            binding.btnGenerateFlashcards.visibility = View.GONE
            binding.btnGenerateQuiz.visibility = View.VISIBLE
            binding.btnGenerateQuiz.text = getString(R.string.btn_start)
        } else {
            // Default to Flashcards mode
            binding.btnGenerateQuiz.visibility = View.GONE
            binding.btnGenerateFlashcards.visibility = View.VISIBLE
            binding.btnGenerateFlashcards.text = getString(R.string.btn_generate)
        }
        binding.btnSaveNotes.visibility = View.GONE
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSaveNotes.setOnClickListener {
            saveNotesToFirestore(navigateAway = true)
        }

        binding.btnGenerateFlashcards.setOnClickListener {
            generateFlashcardsFlow()
        }

        binding.btnGenerateQuiz.setOnClickListener {
            generateQuizFlow()
        }
    }

    private fun checkForExistingNote() {
        @Suppress("DEPRECATION")
        val existingNote = intent.getSerializableExtra(EXTRA_EXISTING_NOTE) as? Note
        if (existingNote != null) {
            existingNoteId = existingNote.id
            binding.etNoteTitle.setText(existingNote.title)
            binding.etSubject.setText(existingNote.subject)
            binding.etNotesContent.setText(existingNote.content)
        }
    }

    private fun validateInputs(): Boolean {
        val content = binding.etNotesContent.text.toString().trim()

        if (content.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_empty_notes), Toast.LENGTH_SHORT).show()
            binding.etNotesContent.requestFocus()
            return false
        }

        var title = binding.etNoteTitle.text.toString().trim()
        if (title.isEmpty()) {
            title = content.lines()
                .firstOrNull { it.isNotBlank() }
                ?.replace(Regex("^[#*•\\-\\d.\\s]+"), "")
                ?.take(30)
                ?.trim()
                ?.ifBlank { "Study Notes" }
                ?: "Study Notes"
            binding.etNoteTitle.setText(title)
        }

        var subject = binding.etSubject.text.toString().trim()
        if (subject.isEmpty()) {
            subject = prefs.lastSubject.ifBlank { "General" }
            binding.etSubject.setText(subject)
        } else {
            prefs.lastSubject = subject
        }

        return true
    }

    private fun saveNotesToFirestore(navigateAway: Boolean, onSaved: ((Note) -> Unit)? = null) {
        if (!validateInputs()) return

        val title = binding.etNoteTitle.text.toString().trim()
        val subject = binding.etSubject.text.toString().trim()
        val content = binding.etNotesContent.text.toString().trim()

        setLoading(true, getString(R.string.loading_saving))

        lifecycleScope.launch {
            try {
                val note = Note(
                    id = existingNoteId,
                    userId = FirebaseService.getCurrentUserId(),
                    title = title,
                    subject = subject,
                    content = content,
                    createdAt = System.currentTimeMillis()
                )

                val savedId = FirebaseService.saveNote(note)
                val finalNote = note.copy(id = savedId)

                Toast.makeText(this@NotesActivity, "Notes saved successfully.", Toast.LENGTH_SHORT).show()

                onSaved?.invoke(finalNote)

                if (navigateAway) {
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotesActivity, "Error saving notes: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun generateFlashcardsFlow() {
        if (!validateInputs()) return

        val title = binding.etNoteTitle.text.toString().trim()
        val subject = binding.etSubject.text.toString().trim()
        val content = binding.etNotesContent.text.toString().trim()

        setLoading(true, getString(R.string.loading_generating_flashcards))

        lifecycleScope.launch {
            try {
                // Also ensure note is saved in background
                val note = Note(
                    id = existingNoteId,
                    userId = FirebaseService.getCurrentUserId(),
                    title = title,
                    subject = subject,
                    content = content,
                    createdAt = System.currentTimeMillis()
                )
                FirebaseService.saveNote(note)

                // Call AI / Study Service
                val generatedSet = AIService.generateFlashcards(
                    noteTitle = title,
                    subject = subject,
                    noteContent = content,
                    userId = FirebaseService.getCurrentUserId()
                )

                // Save generated flashcards to Firestore
                val savedId = FirebaseService.saveFlashcardSet(generatedSet)
                val finalSet = generatedSet.copy(id = savedId)

                // Open FlashcardActivity via Explicit Intent
                val intent = Intent(this@NotesActivity, FlashcardActivity::class.java).apply {
                    putExtra(FlashcardActivity.EXTRA_FLASHCARD_SET, finalSet)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this@NotesActivity,
                    "Generation failed: ${e.localizedMessage}. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun generateQuizFlow() {
        if (!validateInputs()) return

        val title = binding.etNoteTitle.text.toString().trim()
        val subject = binding.etSubject.text.toString().trim()
        val content = binding.etNotesContent.text.toString().trim()

        setLoading(true, getString(R.string.loading_generating_quiz))

        lifecycleScope.launch {
            try {
                // Save note
                val note = Note(
                    id = existingNoteId,
                    userId = FirebaseService.getCurrentUserId(),
                    title = title,
                    subject = subject,
                    content = content,
                    createdAt = System.currentTimeMillis()
                )
                FirebaseService.saveNote(note)

                // Call AI / Study Service
                val generatedQuiz = AIService.generateQuiz(
                    noteTitle = title,
                    subject = subject,
                    noteContent = content,
                    userId = FirebaseService.getCurrentUserId()
                )

                // Save generated quiz to Firestore
                val savedId = FirebaseService.saveQuiz(generatedQuiz)
                val finalQuiz = generatedQuiz.copy(id = savedId)

                // Open QuizActivity via Explicit Intent
                val intent = Intent(this@NotesActivity, QuizActivity::class.java).apply {
                    putExtra(QuizActivity.EXTRA_QUIZ, finalQuiz)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this@NotesActivity,
                    "Generation failed: ${e.localizedMessage}. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean, message: String = "") {
        binding.progressBarNotes.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvLoadingStatus.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvLoadingStatus.text = message
        binding.btnGenerateFlashcards.isEnabled = !isLoading
        binding.btnGenerateQuiz.isEnabled = !isLoading
        binding.btnSaveNotes.isEnabled = !isLoading
    }
}
