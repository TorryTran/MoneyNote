package com.moneynote.app.data

import java.util.concurrent.atomic.AtomicLong

object DataChangeTracker {
    private val transactionsVersion = AtomicLong(0L)
    private val walletsVersion = AtomicLong(0L)
    private val notesVersion = AtomicLong(0L)
    private val categoriesVersion = AtomicLong(0L)

    fun bumpTransactions() {
        transactionsVersion.incrementAndGet()
    }

    fun bumpWallets() {
        walletsVersion.incrementAndGet()
    }

    fun bumpNotes() {
        notesVersion.incrementAndGet()
    }

    fun bumpCategories() {
        categoriesVersion.incrementAndGet()
    }

    fun currentTransactionsVersion(): Long = transactionsVersion.get()

    fun currentWalletsVersion(): Long = walletsVersion.get()

    fun currentNotesVersion(): Long = notesVersion.get()

    fun currentCategoriesVersion(): Long = categoriesVersion.get()
}
