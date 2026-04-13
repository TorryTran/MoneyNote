package com.moneynote.app.ui.entry

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.moneynote.app.data.TransactionType
import com.moneynote.app.ui.WalletFragment

class EntryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> EntryFormFragment.newInstance(TransactionType.EXPENSE)
            1 -> EntryFormFragment.newInstance(TransactionType.INCOME)
            else -> WalletFragment()
        }
    }
}
