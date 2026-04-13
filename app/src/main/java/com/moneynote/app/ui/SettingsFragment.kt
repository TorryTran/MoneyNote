package com.moneynote.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.moneynote.app.R
import com.moneynote.app.data.TransactionEntity
import com.moneynote.app.data.TransactionRepository
import com.moneynote.app.data.TransactionType
import com.moneynote.app.databinding.FragmentSettingsBinding
import com.moneynote.app.ui.common.CategoryItem
import com.moneynote.app.ui.common.styleAppDialog
import com.moneynote.app.ui.entry.CategoryStore
import com.moneynote.app.ui.entry.WalletItem
import com.moneynote.app.ui.entry.WalletStore
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SettingsFragment : Fragment(), TabRefreshable {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TransactionRepository
    private lateinit var walletStore: WalletStore
    private lateinit var categoryStore: CategoryStore

    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val all = repository.getAll()
                val wallets = walletStore.load()
                val expenseCategories = categoryStore.load(TransactionType.EXPENSE)
                val incomeCategories = categoryStore.load(TransactionType.INCOME)
                val json = encodeBackup(
                    transactions = all,
                    wallets = wallets,
                    expenseCategories = expenseCategories,
                    incomeCategories = incomeCategories,
                    languageTag = currentLanguageTag()
                )
                requireContext().contentResolver.openOutputStream(uri, "wt").use { out ->
                    requireNotNull(out) { getString(R.string.error_cannot_write_file) }
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                all.size
            }.onSuccess { count ->
                updateStatus(uri.lastPathSegment ?: getString(R.string.file_status_ready))
                Toast.makeText(requireContext(), getString(R.string.file_export_ok, count), Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(requireContext(), e.message ?: getString(R.string.error_export_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val text = requireContext().contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { getString(R.string.error_cannot_read_file) }
                    input.bufferedReader(Charsets.UTF_8).readText()
                }
                val backup = decodeBackup(text)
                repository.replaceAll(backup.transactions)
                walletStore.save(rebuildWallets(backup.transactions, backup.wallets))
                backup.expenseCategories?.let { categoryStore.save(TransactionType.EXPENSE, it) }
                backup.incomeCategories?.let { categoryStore.save(TransactionType.INCOME, it) }
                backup.languageTag?.let { tag ->
                    if (tag == "en" || tag == "vi") {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    }
                }
                backup.transactions.size
            }.onSuccess { count ->
                updateStatus(uri.lastPathSegment ?: getString(R.string.file_status_ready))
                Toast.makeText(requireContext(), getString(R.string.file_import_ok, count), Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(requireContext(), e.message ?: getString(R.string.error_import_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = TransactionRepository.get(requireContext())
        walletStore = WalletStore(requireContext())
        categoryStore = CategoryStore(requireContext())

        binding.btnExportFile.setOnClickListener {
            val name = getString(R.string.default_backup_file_name)
            exportFileLauncher.launch(name)
        }

        binding.btnImportFile.setOnClickListener {
            importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }

        binding.btnLanguage.setOnClickListener { showLanguageDialog() }
        binding.btnTelegram.setOnClickListener { openLink("https://t.me/TorryTran") }
        binding.btnGithub.setOnClickListener { openLink("https://github.com/torrytran") }
        binding.btnMomo.setOnClickListener { openLink("https://me.momo.vn/OeInTJsosqsoIqUnfOuMf8") }
        updateStatus(getString(R.string.file_status_ready))
        updateLanguageLabel()
    }

    private fun updateStatus(value: String) {
        binding.tvFileStatus.text = getString(R.string.settings_status, value)
    }

    private fun showLanguageDialog() {
        val options = arrayOf(
            getString(R.string.language_vietnamese),
            getString(R.string.language_english)
        )
        val current = currentLanguageTag()
        val checked = if (current == "en") 1 else 0

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_language_title))
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val languageTag = if (which == 1) "en" else "vi"
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
                dialog.dismiss()
                updateLanguageLabel()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
            .also {
                it.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_panel)
                styleAppDialog(it, requireContext())
            }
    }

    private fun updateLanguageLabel() {
        val label = if (currentLanguageTag() == "en") {
            getString(R.string.language_english)
        } else {
            getString(R.string.language_vietnamese)
        }
        binding.tvLanguageValue.text = getString(R.string.settings_language_current, label)
    }

    private fun currentLanguageTag(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val fromApp = appLocales[0]?.language?.lowercase().orEmpty()
        if (fromApp == "en" || fromApp == "vi") return fromApp
        val system = resources.configuration.locales[0]?.language?.lowercase().orEmpty()
        return if (system == "en") "en" else "vi"
    }

    private fun openLink(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(requireContext(), getString(R.string.error_cannot_open_link), Toast.LENGTH_SHORT).show()
        }
    }

    private fun rebuildWallets(
        transactions: List<TransactionEntity>,
        backupWallets: List<WalletItem>?
    ): List<WalletItem> {
        val orderedNames = linkedSetOf<String>()
        backupWallets.orEmpty()
            .map { it.name.trim() }
            .filter { it.isNotEmpty() }
            .forEach(orderedNames::add)
        transactions
            .map { it.wallet.trim() }
            .filter { it.isNotEmpty() }
            .forEach(orderedNames::add)
        transactions
            .map { it.transferToWallet.trim() }
            .filter { it.isNotEmpty() }
            .forEach(orderedNames::add)

        if (orderedNames.isEmpty()) {
            return walletStore.load()
        }

        val balances = orderedNames.associateWith { 0L }.toMutableMap()
        transactions.forEach { tx ->
            val walletName = tx.wallet.trim()
            if (walletName.isEmpty()) return@forEach
            if (tx.isTransfer) {
                balances[walletName] = (balances[walletName] ?: 0L) - tx.amount
                val targetWallet = tx.transferToWallet.trim()
                if (targetWallet.isNotEmpty()) {
                    if (!balances.containsKey(targetWallet)) {
                        balances[targetWallet] = 0L
                    }
                    balances[targetWallet] = (balances[targetWallet] ?: 0L) + tx.amount
                }
            } else {
                val delta = if (tx.type == TransactionType.INCOME) tx.amount else -tx.amount
                balances[walletName] = (balances[walletName] ?: 0L) + delta
            }
        }
        return orderedNames.map { WalletItem(it, balances[it] ?: 0L) }
    }

    private fun encodeBackup(
        transactions: List<TransactionEntity>,
        wallets: List<WalletItem>,
        expenseCategories: List<CategoryItem>,
        incomeCategories: List<CategoryItem>,
        languageTag: String
    ): String {
        val txArray = JSONArray()
        transactions.forEach { tx ->
            txArray.put(
                JSONObject().apply {
                    put("type", tx.type.name)
                    put("amount", tx.amount)
                    put("wallet", tx.wallet)
                    put("transferToWallet", tx.transferToWallet)
                    put("isTransfer", tx.isTransfer)
                    put("category", tx.category)
                    put("note", tx.note)
                    put("date", tx.date)
                }
            )
        }

        val walletArray = JSONArray()
        wallets.forEach { wallet ->
            walletArray.put(
                JSONObject().apply {
                    put("name", wallet.name)
                    put("balance", wallet.balance)
                }
            )
        }

        fun encodeCategories(items: List<CategoryItem>): JSONArray {
            val arr = JSONArray()
            items.filter { !it.isEditor }.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("name", item.name)
                        put("iconRes", item.iconRes)
                        put("colorHex", item.colorHex)
                    }
                )
            }
            return arr
        }

        val root = JSONObject().apply {
            put("version", 3)
            put("transactions", txArray)
            put("wallets", walletArray)
            put(
                "categories",
                JSONObject().apply {
                    put("expense", encodeCategories(expenseCategories))
                    put("income", encodeCategories(incomeCategories))
                }
            )
            put(
                "settings",
                JSONObject().apply {
                    put("language", languageTag)
                }
            )
        }
        return root.toString()
    }

    private fun decodeBackup(raw: String): BackupData {
        val text = raw.trim()

        fun decodeTransactionsArray(arr: JSONArray): List<TransactionEntity> {
            val list = mutableListOf<TransactionEntity>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    TransactionEntity(
                        type = TransactionType.valueOf(obj.getString("type")),
                        amount = obj.getLong("amount"),
                        wallet = obj.optString("wallet", getString(R.string.wallet_default_cash)),
                        transferToWallet = obj.optString("transferToWallet", ""),
                        isTransfer = obj.optBoolean("isTransfer", false),
                        category = obj.getString("category"),
                        note = obj.optString("note", ""),
                        date = obj.getLong("date")
                    )
                )
            }
            return list
        }

        fun decodeWalletsArray(arr: JSONArray): List<WalletItem> {
            val list = mutableListOf<WalletItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WalletItem(
                        name = obj.getString("name"),
                        balance = obj.getLong("balance")
                    )
                )
            }
            return list
        }

        fun decodeCategoriesArray(arr: JSONArray): List<CategoryItem> {
            val list = mutableListOf<CategoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CategoryItem(
                        name = obj.getString("name"),
                        iconRes = obj.getInt("iconRes"),
                        colorHex = obj.optString("colorHex", "#FFFFFF"),
                        isEditor = false
                    )
                )
            }
            return list
        }

        if (text.startsWith("[")) {
            val tx = decodeTransactionsArray(JSONArray(text))
            return BackupData(
                transactions = tx,
                wallets = null,
                expenseCategories = null,
                incomeCategories = null,
                languageTag = null
            )
        }

        val root = JSONObject(text)
        val tx = decodeTransactionsArray(root.getJSONArray("transactions"))
        val wallets = root.optJSONArray("wallets")?.let { decodeWalletsArray(it) }
        val categoriesObj = root.optJSONObject("categories")
        val expenseCategories = categoriesObj?.optJSONArray("expense")?.let { decodeCategoriesArray(it) }
        val incomeCategories = categoriesObj?.optJSONArray("income")?.let { decodeCategoriesArray(it) }
        val languageTag = root.optJSONObject("settings")?.optString("language")?.lowercase()

        return BackupData(
            transactions = tx,
            wallets = wallets,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            languageTag = languageTag
        )
    }

    private data class BackupData(
        val transactions: List<TransactionEntity>,
        val wallets: List<WalletItem>?,
        val expenseCategories: List<CategoryItem>?,
        val incomeCategories: List<CategoryItem>?,
        val languageTag: String?
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun refreshTab() {
        val ui = _binding ?: return
        updateLanguageLabel()
        ui.tvFileStatus.text = getString(R.string.settings_status, getString(R.string.file_status_ready))
    }
}
