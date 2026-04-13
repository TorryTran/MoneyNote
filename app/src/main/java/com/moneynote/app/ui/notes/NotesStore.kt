package com.moneynote.app.ui.notes

import android.content.Context
import androidx.core.text.HtmlCompat
import com.moneynote.app.data.DataChangeTracker
import com.moneynote.app.data.MoneyNoteDatabase
import org.json.JSONArray
import org.json.JSONObject

class NotesStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val db = MoneyNoteDatabase.getInstance(context)
    private var legacyMigrated = false

    fun load(): MutableList<NoteItem> {
        migrateLegacyPrefsIfNeeded()
        return db.getAllNotes().map { it.withPreview() }.toMutableList()
    }

    fun save(items: List<NoteItem>) {
        items.forEach { db.upsertNote(it.withPreview()) }
        val currentIds = items.map { it.id }.toSet()
        db.getAllNotes()
            .filterNot { it.id in currentIds }
            .forEach { db.deleteNote(it.id) }
        DataChangeTracker.bumpNotes()
    }

    fun upsert(item: NoteItem) {
        migrateLegacyPrefsIfNeeded()
        db.upsertNote(item.withPreview())
        DataChangeTracker.bumpNotes()
    }

    fun delete(id: Long) {
        migrateLegacyPrefsIfNeeded()
        db.deleteNote(id)
        DataChangeTracker.bumpNotes()
    }

    private fun migrateLegacyPrefsIfNeeded() {
        if (legacyMigrated) return
        legacyMigrated = true
        if (db.getAllNotes().isNotEmpty()) return
        val raw = prefs.getString(KEY_NOTES, null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                db.upsertNote(
                    NoteItem(
                        id = obj.optLong("id"),
                        title = obj.optString("title", ""),
                        contentHtml = obj.optString("contentHtml", ""),
                        updatedAt = obj.optLong("updatedAt")
                    ).withPreview()
                )
            }
        }
    }

    private fun NoteItem.withPreview(): NoteItem {
        if (previewText.isNotBlank()) return this
        val preview = HtmlCompat.fromHtml(contentHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace('\n', ' ')
            .trim()
            .let { text -> if (text.length > 90) text.take(90).trimEnd() + "..." else text }
        return copy(previewText = preview)
    }

    companion object {
        private const val PREFS_NAME = "notes_store"
        private const val KEY_NOTES = "notes_json"
    }
}
