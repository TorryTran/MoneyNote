package com.moneynote.app.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.moneynote.app.databinding.ItemNoteBinding
import com.moneynote.app.ui.common.DateUtils

class NotesAdapter(
    private val onClick: (NoteItem) -> Unit,
    private val onLongClick: (NoteItem) -> Unit
) : ListAdapter<NoteItem, NotesAdapter.NoteVH>(DIFF) {

    fun submit(list: List<NoteItem>) {
        submitList(list.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteVH {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteVH(binding)
    }

    override fun onBindViewHolder(holder: NoteVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteVH(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NoteItem) {
            binding.tvTitle.text = item.title.ifBlank { "Ghi chú không tiêu đề" }
            binding.tvUpdatedAt.text = DateUtils.formatDate(item.updatedAt) + "  " + DateUtils.formatTime(item.updatedAt)
            binding.tvPreview.text = item.previewText
            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NoteItem>() {
            override fun areItemsTheSame(oldItem: NoteItem, newItem: NoteItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: NoteItem, newItem: NoteItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
