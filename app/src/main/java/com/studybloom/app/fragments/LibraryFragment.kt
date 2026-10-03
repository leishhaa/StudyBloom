package com.studybloom.app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.studybloom.app.R
import com.studybloom.app.activities.FlashcardActivity
import com.studybloom.app.activities.NotesActivity
import com.studybloom.app.activities.QuizActivity
import com.studybloom.app.adapters.LibraryAdapter
import com.studybloom.app.databinding.FragmentLibraryBinding
import com.studybloom.app.models.LibraryItem
import com.studybloom.app.models.LibraryItemType
import com.studybloom.app.services.FirebaseService
import kotlinx.coroutines.launch

/**
 * LibraryFragment:
 * Displays all saved study materials (Notes, Flashcard Sets, and Quizzes)
 * with chip filters for easy categorization.
 * Demonstrates:
 * - RecyclerView setup and LinearLayoutManager
 * - Custom Adapter with click listeners
 * - Explicit Intents passing Serializable data models via Intent extras
 * - Firestore data retrieval and aggregation
 */
class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var libraryAdapter: LibraryAdapter
    private val allItems = mutableListOf<LibraryItem>()
    private var currentFilter: LibraryItemType? = null // null means ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupChipFilters()
    }

    override fun onResume() {
        super.onResume()
        loadLibraryData()
    }

    private fun setupRecyclerView() {
        libraryAdapter = LibraryAdapter(emptyList()) { selectedItem ->
            handleItemClick(selectedItem)
        }

        binding.rvLibrary.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = libraryAdapter
        }
    }

    private fun setupChipFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipNotes) -> LibraryItemType.NOTE
                checkedIds.contains(R.id.chipFlashcards) -> LibraryItemType.FLASHCARDS
                checkedIds.contains(R.id.chipQuizzes) -> LibraryItemType.QUIZ
                else -> null
            }
            filterAndDisplay()
        }
    }

    private fun loadLibraryData() {
        binding.progressBarLibrary.visibility = View.VISIBLE
        binding.llEmptyState.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notes = FirebaseService.getNotes()
                val flashcardSets = FirebaseService.getFlashcardSets()
                val quizzes = FirebaseService.getQuizzes()

                allItems.clear()

                // Map Notes into LibraryItem
                notes.forEach { note ->
                    allItems.add(
                        LibraryItem(
                            id = note.id,
                            title = note.title,
                            subject = note.subject,
                            type = LibraryItemType.NOTE,
                            timestamp = note.createdAt,
                            note = note
                        )
                    )
                }

                // Map Flashcards into LibraryItem
                flashcardSets.forEach { set ->
                    allItems.add(
                        LibraryItem(
                            id = set.id,
                            title = set.title,
                            subject = set.subject,
                            type = LibraryItemType.FLASHCARDS,
                            timestamp = set.createdAt,
                            flashcardSet = set
                        )
                    )
                }

                // Map Quizzes into LibraryItem
                quizzes.forEach { quiz ->
                    allItems.add(
                        LibraryItem(
                            id = quiz.id,
                            title = quiz.title,
                            subject = quiz.subject,
                            type = LibraryItemType.QUIZ,
                            timestamp = quiz.createdAt,
                            quiz = quiz
                        )
                    )
                }

                // Sort by most recent first
                allItems.sortByDescending { it.timestamp }

                filterAndDisplay()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                binding.progressBarLibrary.visibility = View.GONE
            }
        }
    }

    private fun filterAndDisplay() {
        val filtered = if (currentFilter == null) {
            allItems
        } else {
            allItems.filter { it.type == currentFilter }
        }

        libraryAdapter.updateList(filtered)
        binding.llEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun handleItemClick(item: LibraryItem) {
        when (item.type) {
            LibraryItemType.NOTE -> {
                val intent = Intent(requireContext(), NotesActivity::class.java).apply {
                    putExtra(NotesActivity.EXTRA_EXISTING_NOTE, item.note)
                }
                startActivity(intent)
            }
            LibraryItemType.FLASHCARDS -> {
                val intent = Intent(requireContext(), FlashcardActivity::class.java).apply {
                    putExtra(FlashcardActivity.EXTRA_FLASHCARD_SET, item.flashcardSet)
                }
                startActivity(intent)
            }
            LibraryItemType.QUIZ -> {
                val intent = Intent(requireContext(), QuizActivity::class.java).apply {
                    putExtra(QuizActivity.EXTRA_QUIZ, item.quiz)
                }
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
