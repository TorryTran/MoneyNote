package com.moneynote.app.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.Fragment
import com.moneynote.app.R
import com.moneynote.app.databinding.ActivityMainBinding
import com.moneynote.app.ui.calendar.CalendarFragment
import com.moneynote.app.ui.entry.EntryHostFragment
import com.moneynote.app.update.AppUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentTag: String = "entry"
    private var updateCheckJob: Job? = null
    private var updateDialog: AlertDialog? = null
    private val updateDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: return
            if (!AppUpdater.handleDownloadCompleted(this@MainActivity, id)) return
            Toast.makeText(this@MainActivity, getString(R.string.update_install_prompt), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerUpdateDownloadReceiver()

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

        maybeCheckForUpdates()
    }

    private fun showEntry(): Boolean {
        switchTo("entry")
        return true
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
                runOnCommit {
                    (supportFragmentManager.findFragmentByTag(tag) as? TabRefreshable)?.refreshTab()
                }
            }
            .commit()
        currentTag = tag
    }

    private fun createFragment(tag: String): Fragment = when (tag) {
        "entry" -> EntryHostFragment()
        "calendar" -> CalendarFragment()
        "report" -> ReportFragment()
        "wallet" -> NotesFragment()
        else -> SettingsFragment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_CURRENT_TAG, currentTag)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        updateCheckJob?.cancel()
        updateDialog?.dismiss()
        unregisterReceiver(updateDownloadReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val handledPendingInstall = AppUpdater.maybeInstallPendingDownload(this)
        if (!handledPendingInstall) {
            maybeCheckForUpdates()
        }
    }

    private fun maybeCheckForUpdates() {
        if (updateDialog?.isShowing == true) return
        if (updateCheckJob?.isActive == true) return

        updateCheckJob = lifecycleScope.launch {
            AppUpdater.fetchLatestRelease()
                .onSuccess { release ->
                    if (
                        AppUpdater.hasNewerVersion(release) &&
                        !isFinishing &&
                        !isDestroyed &&
                        updateDialog?.isShowing != true
                    ) {
                        updateDialog = AppUpdater.showUpdateDialog(this@MainActivity, release).also { dialog ->
                            dialog.setOnDismissListener {
                                if (updateDialog === dialog) {
                                    updateDialog = null
                                }
                            }
                        }
                    }
                }
                .onFailure {
                    updateDialog = null
                }
        }
    }

    private fun registerUpdateDownloadReceiver() {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateDownloadReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(updateDownloadReceiver, filter)
        }
    }

    companion object {
        private const val KEY_CURRENT_TAG = "key_current_tab_tag"
    }
}
