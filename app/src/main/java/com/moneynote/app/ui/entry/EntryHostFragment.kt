package com.moneynote.app.ui.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.moneynote.app.R
import com.moneynote.app.ui.TabRefreshable
import com.moneynote.app.databinding.FragmentEntryHostBinding

class EntryHostFragment : Fragment(), TabRefreshable {
    private var _binding: FragmentEntryHostBinding? = null
    private val binding get() = _binding!!
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            refreshCurrentChild()
        }
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
            tab.text = when (position) {
                0 -> getString(R.string.tab_expense)
                1 -> getString(R.string.tab_income)
                else -> getString(R.string.tab_transfer)
            }
        }.attach()
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)
    }

    override fun refreshTab() {
        refreshCurrentChild()
    }

    private fun refreshCurrentChild() {
        val ui = _binding ?: return
        val tag = "f${ui.viewPager.currentItem}"
        (childFragmentManager.findFragmentByTag(tag) as? TabRefreshable)?.refreshTab()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
    }
}
