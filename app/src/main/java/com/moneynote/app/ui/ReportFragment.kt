package com.moneynote.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.moneynote.app.R
import com.moneynote.app.databinding.FragmentReportBinding

class ReportFragment : Fragment(), TabRefreshable {
    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            refreshAllChildren()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewPager.adapter = ReportPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) {
                getString(R.string.report_tab_weekly)
            } else {
                getString(R.string.report_tab_category)
            }
        }.attach()

        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)
        binding.viewPager.post { refreshAllChildren() }
    }

    override fun refreshTab() {
        if (_binding == null) return
        refreshAllChildren()
        binding.viewPager.post { refreshAllChildren() }
    }

    private fun refreshAllChildren() {
        childFragmentManager.fragments.forEach { fragment ->
            (fragment as? TabRefreshable)?.refreshTab()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
    }
}
