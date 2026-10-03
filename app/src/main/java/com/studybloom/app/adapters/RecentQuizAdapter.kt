package com.studybloom.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.studybloom.app.databinding.ItemRecentQuizBinding
import com.studybloom.app.models.QuizResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter to show recent quiz scores in the Progress dashboard.
 */
class RecentQuizAdapter(
    private var results: List<QuizResult>,
    private val onItemClick: ((QuizResult) -> Unit)? = null
) : RecyclerView.Adapter<RecentQuizAdapter.RecentQuizViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    inner class RecentQuizViewHolder(val binding: ItemRecentQuizBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentQuizViewHolder {
        val binding = ItemRecentQuizBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentQuizViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentQuizViewHolder, position: Int) {
        val item = results[position]
        with(holder.binding) {
            tvRecentQuizTitle.text = item.quizTitle
            tvRecentQuizDate.text = "${dateFormat.format(Date(item.timestamp))} • ${item.correctCount}/${item.totalQuestions} Correct"
            tvRecentQuizScore.text = "${item.percentage}%"

            root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    override fun getItemCount(): Int = results.size

    fun updateResults(newResults: List<QuizResult>) {
        this.results = newResults
        notifyDataSetChanged()
    }
}
