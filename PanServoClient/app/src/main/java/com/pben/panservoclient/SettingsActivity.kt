package com.pben.panservoclient

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.os.LocaleListCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnClearLogs: Button
    private lateinit var spinnerLanguage: Spinner

    // Language codes matching the spinner order
    private val languageCodes = arrayOf("", "en", "es") // "" = system default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar: Toolbar = findViewById(R.id.toolbar_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        tvLog = findViewById(R.id.tvLog)
        tvStatus = findViewById(R.id.tvSettingsStatus)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)

        setupLanguageSelector()
        observeBluetoothState()
        displayLogCache()

        btnClearLogs.setOnClickListener {
            BluetoothConnection.clearLogs()
            tvLog.text = ""
        }
    }

    private fun setupLanguageSelector() {
        val languageNames = arrayOf(
            getString(R.string.language_system),
            getString(R.string.language_english),
            getString(R.string.language_spanish)
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        // Set current selection based on current app locale
        val currentLocale = AppCompatDelegate.getApplicationLocales()
        val currentLang = if (currentLocale.isEmpty) "" else currentLocale.get(0)?.language ?: ""
        val selectedIndex = languageCodes.indexOf(currentLang).coerceAtLeast(0)
        spinnerLanguage.setSelection(selectedIndex)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val langCode = languageCodes[position]
                val currentAppLocale = AppCompatDelegate.getApplicationLocales()
                val currentLangCode = if (currentAppLocale.isEmpty) "" else currentAppLocale.get(0)?.language ?: ""

                if (langCode != currentLangCode) {
                    val localeList = if (langCode.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(langCode)
                    }
                    AppCompatDelegate.setApplicationLocales(localeList)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeBluetoothState() {
        BluetoothConnection.isConnected.observe(this) { isConnected ->
            tvStatus.text = if (isConnected) getString(R.string.status_connected_full) else getString(R.string.status_disconnected_full)
        }

        BluetoothConnection.messages.observe(this) { message ->
            logMessage(message)
        }

        BluetoothConnection.errors.observe(this) { error ->
            logMessage(getString(R.string.error_prefix, error))
        }
    }

    private fun displayLogCache() {
        tvLog.text = BluetoothConnection.logCache.joinToString("\n")
        scrollToBottom()
    }

    private fun logMessage(message: String) {
        tvLog.append("$message\n")
        scrollToBottom()
    }

    private fun scrollToBottom() {
        (tvLog.parent as? ScrollView)?.post { (tvLog.parent as ScrollView).fullScroll(View.FOCUS_DOWN) }
    }
}