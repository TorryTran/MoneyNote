package com.moneynote.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.moneynote.app.R
import com.moneynote.app.databinding.ActivityMainBinding
import com.moneynote.app.ui.calendar.CalendarFragment
import com.moneynote.app.ui.entry.EntryHostFragment

class MainActivity : AppCompatActivity(), EntryHostFragment.EntryHostActions {
    private lateinit var binding: ActivityMainBinding
    private var currentTag: String = "entry"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.mainContainer, EntryHostFragment(), "entry")
                .commit()
            currentTag = "entry"
        } else {
            currentTag = savedInstanceState.getString(KEY_CURRENT_TAG, "entry")
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_entry -> showEntry()
                R.id.nav_calendar -> showCalendar()
                R.id.nav_report -> showReport()
                R.id.nav_wallet -> showWallet()
                R.id.nav_settings -> showSettings()
                else -> false
            }
        }

        binding.bottomNav.selectedItemId = when (currentTag) {
            "calendar" -> R.id.nav_calendar
            "report" -> R.id.nav_report
            "wallet" -> R.id.nav_wallet
            "settings" -> R.id.nav_settings
            else -> R.id.nav_entry
        }
    }

    private fun showEntry(): Boolean {
        switchTo("entry")
        return true
    }

    override fun openCalendarTab() {
        binding.bottomNav.selectedItemId = R.id.nav_calendar
    }

    private fun showCalendar(): Boolean {
        switchTo("calendar")
        return true
    }

    private fun showReport(): Boolean {
        switchTo("report")
        return true
    }

    private fun showWallet(): Boolean {
        switchTo("wallet")
        return true
    }

    private fun showSettings(): Boolean {
        switchTo("settings")
        return true
    }

    private fun switchTo(tag: String) {
        val fm = supportFragmentManager
        if (tag == currentTag) {
            (fm.findFragmentByTag(tag) as? TabRefreshable)?.refreshTab()
            return
        }
        val current = fm.findFragmentByTag(currentTag)
        val target = fm.findFragmentByTag(tag) ?: createFragment(tag)
        (target as? TabRefreshable)?.refreshTab()

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .apply {
                if (current != null) hide(current)
                if (target.isAdded) show(target) else add(R.id.mainContainer, target, tag)
            }
            .commit()
        currentTag = tag
    }

    private fun createFragment(tag: String): Fragment = when (tag) {
        "entry" -> EntryHostFragment()
        "calendar" -> CalendarFragment()
        "report" -> ReportFragment()
        "wallet" -> WalletFragment()
        else -> SettingsFragment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_CURRENT_TAG, currentTag)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_CURRENT_TAG = "key_current_tab_tag"
    }
}
