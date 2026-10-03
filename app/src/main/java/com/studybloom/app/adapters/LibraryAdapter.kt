package com.studybloom.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.studybloom.app.R
import com.studybloom.app.databinding.ItemLibraryBinding
import com.studybloom.app.models.LibraryItem
import com.studybloom.app.models.LibraryItemType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView Adapter for displaying saved Notes, Flashcard Sets, and Quizzes in LibraryFragment.
 * Demonstrates standard ViewBinding and event listeners.
 */
class LibraryAdapter(
    private var items: List<LibraryItem>,
    private val onItemClick: (LibraryItem) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    inner class LibraryViewHolder(val binding: ItemLibraryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val binding = ItemLibraryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LibraryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvItemTitle.text = item.title
            tvItemSubject.text = "📚 ${item.subject}"
            tvItemDate.text = dateFormat.format(Date(item.timestamp))

            when (item.type) {
                LibraryItemType.NOTE -> {
                    tvItemTypeBadge.text = "Note 📝"
                    tvItemTypeBadge.setTextColor(ContextCompat.getColor(root.context, R.color.bloom_pink_dark))
                }
                LibraryItemType.FLASHCARDS -> {
                    tvItemTypeBadge.text = "Flashcards 🎴"
                    tvItemTypeBadge.setTextColor(ContextCompat.getColor(root.context, R.color.bloom_lavender_dark))
                }
                LibraryItemType.QUIZ -> {
                    tvItemTypeBadge.text = "Quiz 🎯"
                    tvItemTypeBadge.setTextColor(ContextCompat.getColor(root.context, R.color.bloom_pink_primary))
                }
            }

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<LibraryItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
