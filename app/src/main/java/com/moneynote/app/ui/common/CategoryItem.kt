package com.moneynote.app.ui.common

data class CategoryItem(
    var name: String,
    val iconRes: Int,
    val colorHex: String,
    val isEditor: Boolean = false
)
