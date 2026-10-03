package com.studybloom.app.activities

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studybloom.app.R
import com.studybloom.app.databinding.ActivityNotesBinding
import com.studybloom.app.models.Note
import com.studybloom.app.services.AIService
import com.studybloom.app.services.FirebaseService
import com.studybloom.app.utils.SharedPrefsHelper
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * NotesActivity:
 * Allows students to write or import notes, save them to Cloud Firestore & Firebase Storage,
 * and initiate AI generation for Flashcards or Quizzes.
 * Demonstrates:
 * - Activity Result API for file picking (Intent.ACTION_GET_CONTENT)
 * - Reading streams from content URIs
 * - Firebase Storage file uploads
 * - Cloud Firestore document creation
 * - Calling AI Service with Kotlin Coroutines
 * - Input validation & progressive UI states
 */
class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private lateinit var prefs: SharedPrefsHelper

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null
    private var existingNoteId: String = ""

    companion object {
        const val EXTRA_INITIAL_ACTION = "extra_initial_action"
        const val ACTION_GENERATE_FLASHCARDS = "action_flashcards"
        const val ACTION_GENERATE_QUIZ = "action_quiz"
        const val EXTRA_EXISTING_NOTE = "extra_existing_note"
    }

    // Register File Picker contract for .txt files
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedFile(uri)
        }
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
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSelectFile.setOnClickListener {
            // Launch Android system file picker for plain text files
            filePickerLauncher.launch("text/*")
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
            if (!existingNote.fileName.isNullOrBlank()) {
                binding.tvSelectedFile.text = "${getString(R.string.file_selected_prefix)}${existingNote.fileName}"
            }
        }
    }

    /**
     * Reads the selected .txt file from the device storage into the notes EditText.
     */
    private fun handleSelectedFile(uri: Uri) {
        selectedFileUri = uri
        selectedFileName = getFileNameFromUri(uri)
        binding.tvSelectedFile.text = "${getString(R.string.file_selected_prefix)}$selectedFileName"

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val content = reader.readText()
                    binding.etNotesContent.setText(content)
                    if (binding.etNoteTitle.text.isNullOrBlank() && selectedFileName != null) {
                        val baseName = selectedFileName!!.substringBeforeLast(".")
                        binding.etNoteTitle.setText(baseName)
                    }
                    Toast.makeText(this, "File content loaded! 🌸", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "notes.txt"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    private fun validateInputs(): Boolean {
        val title = binding.etNoteTitle.text.toString().trim()
        val subject = binding.etSubject.text.toString().trim()
        val content = binding.etNotesContent.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (subject.isEmpty()) {
            Toast.makeText(this, "Please enter a subject.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (content.isEmpty()) {
            Toast.makeText(this, "Please enter or import note content.", Toast.LENGTH_SHORT).show()
            return false
        }
        // Save subject to SharedPreferences for future use
        prefs.lastSubject = subject
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
                var fileUrl: String? = null
                // If a file was selected, upload to Firebase Cloud Storage
                if (selectedFileUri != null && selectedFileName != null) {
                    fileUrl = FirebaseService.uploadNoteFile(selectedFileUri!!, selectedFileName!!)
                }

                val note = Note(
                    id = existingNoteId,
                    userId = FirebaseService.getCurrentUserId(),
                    title = title,
                    subject = subject,
                    content = content,
                    fileUrl = fileUrl,
                    fileName = selectedFileName,
                    createdAt = System.currentTimeMillis()
                )

                val savedId = FirebaseService.saveNote(note)
                val finalNote = note.copy(id = savedId)

                Toast.makeText(this@NotesActivity, "Notes saved successfully! 💾🌸", Toast.LENGTH_SHORT).show()

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

        setLoading(true, getString(R.string.loading_generating_ai))

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

                // Call AI Service
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
                val intent = android.content.Intent(this@NotesActivity, FlashcardActivity::class.java).apply {
                    putExtra(FlashcardActivity.EXTRA_FLASHCARD_SET, finalSet)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this@NotesActivity,
                    "AI generation failed: ${e.localizedMessage}. Please try again.",
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

        setLoading(true, getString(R.string.loading_generating_ai))

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

                // Call AI Service
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
                val intent = android.content.Intent(this@NotesActivity, QuizActivity::class.java).apply {
                    putExtra(QuizActivity.EXTRA_QUIZ, finalQuiz)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this@NotesActivity,
                    "Quiz generation failed: ${e.localizedMessage}",
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
