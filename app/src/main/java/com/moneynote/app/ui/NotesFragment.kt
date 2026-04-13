package com.moneynote.app.ui

import android.graphics.Typeface
import android.content.Context
import android.os.Bundle
import android.text.TextPaint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.moneynote.app.R
import com.moneynote.app.data.DataChangeTracker
import com.moneynote.app.databinding.FragmentNotesBinding
import com.moneynote.app.ui.common.styleAppDialog
import com.moneynote.app.ui.notes.NoteItem
import com.moneynote.app.ui.notes.NotesAdapter
import com.moneynote.app.ui.notes.NotesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesFragment : Fragment(), TabRefreshable {
    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesStore: NotesStore
    private lateinit var notesAdapter: NotesAdapter
    private val notes = mutableListOf<NoteItem>()
    private var editingNoteId: Long? = null
    private var lastNotesVersion = -1L
    private var loadJob: Job? = null
    private val minTextScale = 0.8f
    private val maxTextScale = 1.4f
    private val textScaleStep = 0.1f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notesStore = NotesStore(requireContext())
        notesAdapter = NotesAdapter(
            onClick = { openEditor(it) },
            onLongClick = { showNoteActions(it) }
        )
        binding.rvNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotes.adapter = notesAdapter
        binding.rvNotes.itemAnimator = null

        binding.fabAddNote.setOnClickListener { openEditor(null) }
        binding.btnBackToList.setOnClickListener { showListMode() }
        binding.btnSaveNote.setOnClickListener { saveCurrentNote() }

        binding.btnBold.setOnClickListener { toggleStyleSpan(Typeface.BOLD) }
        binding.btnItalic.setOnClickListener { toggleStyleSpan(Typeface.ITALIC) }
        binding.btnUnderline.setOnClickListener { toggleUnderlineSpan() }
        binding.btnStrike.setOnClickListener { toggleStrikeSpan() }
        binding.btnTextLarger.setOnClickListener { applySizeSpan(1.25f) }
        binding.btnTextSmaller.setOnClickListener { applySizeSpan(0.85f) }

        setupToolbarButtonLabels()
        loadNotes()
        showListMode()
    }

    override fun refreshTab() {
        if (_binding != null) {
            val currentVersion = DataChangeTracker.currentNotesVersion()
            if (currentVersion != lastNotesVersion) {
                loadNotes()
            }
        }
    }

    private fun loadNotes() {
        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) { notesStore.load() }
            val ui = _binding ?: return@launch
            notes.clear()
            notes.addAll(loaded)
            notesAdapter.submit(loaded)
            ui.tvEmptyNotes.isVisible = loaded.isEmpty()
            lastNotesVersion = DataChangeTracker.currentNotesVersion()
        }
    }

    private fun openEditor(note: NoteItem?) {
        editingNoteId = note?.id
        binding.editorContainer.isVisible = true
        binding.listContainer.isVisible = false
        binding.fabAddNote.isVisible = false
        binding.etNoteTitle.setText(note?.title.orEmpty())
        val content = note?.contentHtml?.takeIf { it.isNotBlank() }?.let {
            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
        } ?: SpannableStringBuilder("")
        binding.etNoteContent.text = SpannableStringBuilder(content)
        binding.etNoteContent.requestFocus()
    }

    private fun showListMode() {
        hideKeyboard()
        binding.editorContainer.isVisible = false
        binding.listContainer.isVisible = true
        binding.fabAddNote.isVisible = true
        binding.etNoteTitle.clearFocus()
        binding.etNoteContent.clearFocus()
        editingNoteId = null
        binding.etNoteTitle.setText("")
        binding.etNoteContent.text = SpannableStringBuilder("")
    }

    private fun saveCurrentNote() {
        val title = binding.etNoteTitle.text?.toString()?.trim().orEmpty()
        val content = binding.etNoteContent.text ?: SpannableStringBuilder("")
        val plainContent = content.toString().trim()
        if (title.isBlank() && plainContent.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.notes_empty_error), Toast.LENGTH_SHORT).show()
            return
        }

        val note = NoteItem(
            id = editingNoteId ?: System.currentTimeMillis(),
            title = title,
            contentHtml = HtmlCompat.toHtml(SpannableStringBuilder(content), HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE),
            updatedAt = System.currentTimeMillis()
        )
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { notesStore.upsert(note) }
            loadNotes()
            showListMode()
            Toast.makeText(requireContext(), getString(R.string.saved_success), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNoteActions(note: NoteItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(note.title.ifBlank { getString(R.string.notes_untitled) })
            .setItems(arrayOf(getString(R.string.action_update), getString(R.string.action_delete))) { _, which ->
                when (which) {
                    0 -> openEditor(note)
                    1 -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            withContext(Dispatchers.IO) { notesStore.delete(note.id) }
                            loadNotes()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
            .also {
                it.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_panel)
                styleAppDialog(it, requireContext())
            }
    }

    private fun setupToolbarButtonLabels() {
        binding.btnBold.setTypeface(binding.btnBold.typeface, Typeface.BOLD)
        binding.btnItalic.setTypeface(binding.btnItalic.typeface, Typeface.ITALIC)
        binding.btnUnderline.paintFlags = binding.btnUnderline.paintFlags or TextPaint.UNDERLINE_TEXT_FLAG
        binding.btnStrike.paintFlags = binding.btnStrike.paintFlags or TextPaint.STRIKE_THRU_TEXT_FLAG
    }

    private fun toggleStyleSpan(style: Int) {
        val editable = binding.etNoteContent.text ?: return
        val start = binding.etNoteContent.selectionStart
        val end = binding.etNoteContent.selectionEnd
        if (start < 0 || end <= start) return
        val spans = editable.getSpans(start, end, StyleSpan::class.java)
            .filter { editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start && it.style == style }
        val fullyApplied = spans.any {
            editable.getSpanStart(it) <= start && editable.getSpanEnd(it) >= end
        }
        spans.forEach { span ->
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)
            val spanFlags = editable.getSpanFlags(span)
            editable.removeSpan(span)
            if (spanStart < start) {
                editable.setSpan(StyleSpan(style), spanStart, start, spanFlags)
            }
            if (spanEnd > end) {
                editable.setSpan(StyleSpan(style), end, spanEnd, spanFlags)
            }
        }
        if (!fullyApplied) {
            editable.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun toggleUnderlineSpan() {
        val editable = binding.etNoteContent.text ?: return
        val start = binding.etNoteContent.selectionStart
        val end = binding.etNoteContent.selectionEnd
        if (start < 0 || end <= start) return
        val spans = editable.getSpans(start, end, UnderlineSpan::class.java)
            .filter { editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start }
        val fullyApplied = spans.any {
            editable.getSpanStart(it) <= start && editable.getSpanEnd(it) >= end
        }
        spans.forEach { span ->
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)
            val spanFlags = editable.getSpanFlags(span)
            editable.removeSpan(span)
            if (spanStart < start) {
                editable.setSpan(UnderlineSpan(), spanStart, start, spanFlags)
            }
            if (spanEnd > end) {
                editable.setSpan(UnderlineSpan(), end, spanEnd, spanFlags)
            }
        }
        if (!fullyApplied) {
            editable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun toggleStrikeSpan() {
        val editable = binding.etNoteContent.text ?: return
        val start = binding.etNoteContent.selectionStart
        val end = binding.etNoteContent.selectionEnd
        if (start < 0 || end <= start) return
        val spans = editable.getSpans(start, end, StrikethroughSpan::class.java)
            .filter { editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start }
        val fullyApplied = spans.any {
            editable.getSpanStart(it) <= start && editable.getSpanEnd(it) >= end
        }
        spans.forEach { span ->
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)
            val spanFlags = editable.getSpanFlags(span)
            editable.removeSpan(span)
            if (spanStart < start) {
                editable.setSpan(StrikethroughSpan(), spanStart, start, spanFlags)
            }
            if (spanEnd > end) {
                editable.setSpan(StrikethroughSpan(), end, spanEnd, spanFlags)
            }
        }
        if (!fullyApplied) {
            editable.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applySizeSpan(multiplier: Float) {
        val editable = binding.etNoteContent.text ?: return
        val start = binding.etNoteContent.selectionStart
        val end = binding.etNoteContent.selectionEnd
        if (start < 0 || end <= start) return
        val existing = editable.getSpans(start, end, RelativeSizeSpan::class.java)
            .filter { editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start }
        val currentScale = existing
            .firstOrNull { editable.getSpanStart(it) <= start && editable.getSpanEnd(it) >= end }
            ?.sizeChange ?: 1f
        existing.forEach { span ->
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)
            val spanFlags = editable.getSpanFlags(span)
            val spanScale = span.sizeChange
            editable.removeSpan(span)
            if (spanStart < start) {
                editable.setSpan(RelativeSizeSpan(spanScale), spanStart, start, spanFlags)
            }
            if (spanEnd > end) {
                editable.setSpan(RelativeSizeSpan(spanScale), end, spanEnd, spanFlags)
            }
        }

        val nextScale = if (multiplier > 1f) {
            (currentScale + textScaleStep).coerceAtMost(maxTextScale)
        } else {
            (currentScale - textScaleStep).coerceAtLeast(minTextScale)
        }
        if (kotlin.math.abs(nextScale - 1f) > 0.01f) {
            editable.setSpan(RelativeSizeSpan(nextScale), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        val token = binding.etNoteContent.windowToken
            ?: binding.etNoteTitle.windowToken
            ?: binding.root.windowToken
        imm.hideSoftInputFromWindow(token, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadJob?.cancel()
        _binding = null
    }
}
