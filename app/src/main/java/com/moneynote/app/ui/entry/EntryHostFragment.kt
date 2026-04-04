package com.moneynote.app.ui.entry

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.moneynote.app.R
import com.moneynote.app.databinding.FragmentEntryHostBinding

class EntryHostFragment : Fragment() {
    interface EntryHostActions {
        fun openCalendarTab()
    }

    private var actions: EntryHostActions? = null
    private var _binding: FragmentEntryHostBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        actions = context as? EntryHostActions
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntryHostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pagerAdapter = EntryPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) getString(R.string.tab_expense) else getString(R.string.tab_income)
        }.attach()

        binding.btnQuickCalendar.setOnClickListener {
            actions?.openCalendarTab()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
