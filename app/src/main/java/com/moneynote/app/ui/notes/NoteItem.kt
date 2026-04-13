package com.moneynote.app.ui.notes

data class NoteItem(
    val id: Long,
    val title: String,
    val contentHtml: String,
    val updatedAt: Long,
    val previewText: String = ""
)
