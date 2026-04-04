package com.moneynote.app.ui.entry

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.moneynote.app.data.TransactionType

class EntryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            EntryFormFragment.newInstance(TransactionType.EXPENSE)
        } else {
            EntryFormFragment.newInstance(TransactionType.INCOME)
        }
    }
}
