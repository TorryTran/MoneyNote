package com.moneynote.app.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.TypedValue
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.moneynote.app.BuildConfig
import com.moneynote.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val versionName: String,
    val apkUrl: String?,
    val htmlUrl: String,
    val notes: String
)

object AppUpdater {
    private const val RELEASES_URL = "https://api.github.com/repos/TorryTran/MoneyNote/releases/latest"
    private const val APK_MIME = "application/vnd.android.package-archive"
    private const val PREFS = "app_updater_prefs"
    private const val KEY_PENDING_DOWNLOAD_ID = "pending_download_id"
    private const val FETCH_CACHE_MS = 5 * 60 * 1000L
    @Volatile
    private var cachedRelease: ReleaseInfo? = null
    @Volatile
    private var cachedAtMs: Long = 0L

    suspend fun fetchLatestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val cached = cachedRelease
            if (cached != null && now - cachedAtMs < FETCH_CACHE_MS) {
                return@runCatching cached
            }
            val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "MoneyNote-Android")
            }
            try {
                val stream = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: error("GitHub returned HTTP ${connection.responseCode}")
                }
                stream.bufferedReader().use { reader ->
                    val root = JSONObject(reader.readText())
                    val assets = root.optJSONArray("assets")
                    var apkUrl: String? = null
                    var fallbackApkUrl: String? = null
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val url = asset.optString("browser_download_url")
                            val name = asset.optString("name").lowercase()
                            if (!url.endsWith(".apk", ignoreCase = true)) continue
                            if (fallbackApkUrl == null) fallbackApkUrl = url
                            if ("release" in name && "debug" !in name) {
                                apkUrl = url
                                break
                            }
                        }
                    }

                    ReleaseInfo(
                        versionName = normalizeVersion(root.optString("tag_name", root.optString("name", ""))),
                        apkUrl = apkUrl ?: fallbackApkUrl,
                        htmlUrl = root.optString("html_url", "https://github.com/TorryTran/MoneyNote/releases"),
                        notes = root.optString("body", "").trim()
                    ).also {
                        cachedRelease = it
                        cachedAtMs = now
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    fun currentVersionName(): String = normalizeVersion(BuildConfig.VERSION_NAME)

    fun hasNewerVersion(release: ReleaseInfo): Boolean {
        if (release.versionName.isBlank()) return false
        return compareVersions(release.versionName, currentVersionName()) > 0
    }

    fun enqueueDownload(context: Context, release: ReleaseInfo): Long? {
        val apkUrl = release.apkUrl ?: return null
        val request = DownloadManager.Request(apkUrl.toUri())
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType(APK_MIME)
            .setTitle(context.getString(R.string.update_download_title, release.versionName))
            .setDescription(context.getString(R.string.update_download_description))
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "MoneyNote-${release.versionName}.apk"
            )

        val downloadManager = context.getSystemService(DownloadManager::class.java)
        val downloadId = downloadManager.enqueue(request)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_PENDING_DOWNLOAD_ID, downloadId)
            .apply()
        return downloadId
    }

    fun handleDownloadCompleted(context: Context, downloadId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pendingId = prefs.getLong(KEY_PENDING_DOWNLOAD_ID, -1L)
        if (downloadId != pendingId) return false

        return openInstallerForPendingDownload(context, downloadId)
    }

    fun maybeInstallPendingDownload(context: Context): Boolean {
        val pendingId = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_PENDING_DOWNLOAD_ID, -1L)
        if (pendingId <= 0L) return false
        return openInstallerForPendingDownload(context, pendingId)
    }

    private fun openInstallerForPendingDownload(context: Context, downloadId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val downloadManager = context.getSystemService(DownloadManager::class.java)
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return false
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                prefs.edit().remove(KEY_PENDING_DOWNLOAD_ID).apply()
                Toast.makeText(
                    context,
                    context.getString(R.string.update_download_failed, reason),
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        }

        val uri = downloadManager.getUriForDownloadedFile(downloadId) ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (canHandleIntent(context, settingsIntent)) {
                context.startActivity(settingsIntent)
                Toast.makeText(context, context.getString(R.string.update_grant_install_permission), Toast.LENGTH_LONG).show()
                return true
            }
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (canHandleIntent(context, installIntent)) {
            context.startActivity(installIntent)
            prefs.edit().remove(KEY_PENDING_DOWNLOAD_ID).apply()
            return true
        }

        Toast.makeText(context, context.getString(R.string.update_install_unavailable), Toast.LENGTH_SHORT).show()
        return false
    }

    fun showUpdateDialog(
        context: Context,
        release: ReleaseInfo,
        onDownloadStarted: (() -> Unit)? = null
    ): AlertDialog {
        val message = buildString {
            append(context.getString(R.string.update_available_message, release.versionName, currentVersionName()))
            if (release.notes.isNotBlank()) {
                append("\n\n")
                append(release.notes.take(1200))
            }
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.update_available_title))
            .setMessage(message)
            .setNegativeButton(context.getString(R.string.action_cancel), null)
            .setPositiveButton(
                if (release.apkUrl != null) context.getString(R.string.update_download_action)
                else context.getString(R.string.update_open_release_page),
                null
            )
            .create()

        dialog.setOnShowListener {
            styleUpdateDialog(dialog, context)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnClickListener {
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                if (release.apkUrl != null) {
                    enqueueDownload(context, release)
                    onDownloadStarted?.invoke()
                    Toast.makeText(context, context.getString(R.string.update_download_started), Toast.LENGTH_SHORT).show()
                } else {
                    openReleasePage(context, release.htmlUrl)
                }
                dialog.dismiss()
            }
        }
        dialog.show()
        return dialog
    }

    fun openReleasePage(context: Context, url: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun normalizeVersion(raw: String): String {
        return raw.trim().removePrefix("v").removePrefix("V")
    }

    private fun canHandleIntent(context: Context, intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }

    private fun styleUpdateDialog(dialog: AlertDialog, context: Context) {
        val accent = ContextCompat.getColor(context, R.color.accent_blue)
        dialog.findViewById<android.widget.TextView>(R.id.alertTitle)?.apply {
            setTextColor(accent)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            isAllCaps = false
            setTextColor(accent)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            isAllCaps = false
            setTextColor(accent)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
        val rightParts = right.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
        val max = maxOf(leftParts.size, rightParts.size)
        for (i in 0 until max) {
            val l = leftParts.getOrElse(i) { 0 }
            val r = rightParts.getOrElse(i) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }
}
