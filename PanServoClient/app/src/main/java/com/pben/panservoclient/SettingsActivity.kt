package com.pben.panservoclient

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnClearLogs: Button
    private lateinit var spinnerLanguage: Spinner

    // Language codes matching the spinner order
    private val languageCodes = arrayOf("", "en", "es") // "" = system default

    // Speed displays (read-only TextViews)
    private lateinit var tvPanSpeed: TextView
    private lateinit var tvTiltSpeed: TextView
    private lateinit var tvAutopanSpeed: TextView

    private lateinit var togglePanReversed: MaterialButtonToggleGroup
    private lateinit var toggleTiltReversed: MaterialButtonToggleGroup
    private lateinit var tvMaxPanAngle: TextView
    private lateinit var tvMaxTiltAngle: TextView

    private var configListenersActive = false

    companion object {
        private const val SPEED_MIN = 1
        private const val SPEED_MAX = 10
        private const val ANGLE_MIN = 0
        private const val ANGLE_MAX = 180
        private const val ANGLE_STEP = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        enableImmersiveMode()

        val toolbar: Toolbar = findViewById(R.id.toolbar_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        tvLog = findViewById(R.id.tvLog)
        tvStatus = findViewById(R.id.tvSettingsStatus)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)

        // Speed displays
        tvPanSpeed = findViewById(R.id.tvPanSpeed)
        tvTiltSpeed = findViewById(R.id.tvTiltSpeed)
        tvAutopanSpeed = findViewById(R.id.tvAutopanSpeed)

        // Other config fields
        togglePanReversed = findViewById(R.id.togglePanReversed)
        toggleTiltReversed = findViewById(R.id.toggleTiltReversed)
        tvMaxPanAngle = findViewById(R.id.tvMaxPanAngle)
        tvMaxTiltAngle = findViewById(R.id.tvMaxTiltAngle)

        setupConfigListeners()
        setupLanguageSelector()
        observeBluetoothState()

        btnClearLogs.setOnClickListener {
            BluetoothConnection.clearLogs()
            tvLog.text = ""
        }
    }

    override fun onResume() {
        super.onResume()
        loadConfigFromCache()
        displayLogCache()
    }

    private fun setupConfigListeners() {
        // Speed +/- buttons
        setupStepperButtons(R.id.btnPanSpeedMinus, R.id.btnPanSpeedPlus, tvPanSpeed, ServoCommand.CONFIG_PAN_SPEED, SPEED_MIN, SPEED_MAX, 1)
        setupStepperButtons(R.id.btnTiltSpeedMinus, R.id.btnTiltSpeedPlus, tvTiltSpeed, ServoCommand.CONFIG_TILT_SPEED, SPEED_MIN, SPEED_MAX, 1)
        setupStepperButtons(R.id.btnAutopanSpeedMinus, R.id.btnAutopanSpeedPlus, tvAutopanSpeed, ServoCommand.CONFIG_AUTOPAN_SPEED, SPEED_MIN, SPEED_MAX, 1)

        // Max angle +/- buttons
        setupStepperButtons(R.id.btnMaxPanAngleMinus, R.id.btnMaxPanAnglePlus, tvMaxPanAngle, ServoCommand.MAX_PAN_ANGLE, ANGLE_MIN, ANGLE_MAX, ANGLE_STEP)
        setupStepperButtons(R.id.btnMaxTiltAngleMinus, R.id.btnMaxTiltAnglePlus, tvMaxTiltAngle, ServoCommand.MAX_TILT_ANGLE, ANGLE_MIN, ANGLE_MAX, ANGLE_STEP)

        // Reversed toggles (OFF/ON button groups)
        togglePanReversed.check(R.id.btnPanReversedOff)
        toggleTiltReversed.check(R.id.btnTiltReversedOff)

        togglePanReversed.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && configListenersActive) {
                sendConfigCommand(ServoCommand.CONFIG_PAN_REVERSED, if (checkedId == R.id.btnPanReversedOn) "1" else "0")
            }
        }
        toggleTiltReversed.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && configListenersActive) {
                sendConfigCommand(ServoCommand.CONFIG_TILT_REVERSED, if (checkedId == R.id.btnTiltReversedOn) "1" else "0")
            }
        }

        // Activate listeners after setup to prevent firing during initialization
        configListenersActive = true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupStepperButtons(minusBtnId: Int, plusBtnId: Int, display: TextView, command: ServoCommand, min: Int, max: Int, step: Int) {
        val btnMinus: ImageButton = findViewById(minusBtnId)
        val btnPlus: ImageButton = findViewById(plusBtnId)

        // Prevent the parent ScrollView from intercepting touches on these buttons
        val disallowParentIntercept = View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.parent.requestDisallowInterceptTouchEvent(true)
            }
            false
        }
        btnMinus.setOnTouchListener(disallowParentIntercept)
        btnPlus.setOnTouchListener(disallowParentIntercept)

        btnMinus.setOnClickListener {
            val current = display.text.toString().toIntOrNull() ?: min
            val newValue = (current - step).coerceIn(min, max)
            display.text = newValue.toString()
            sendConfigCommand(command, newValue.toString())
        }
        btnPlus.setOnClickListener {
            val current = display.text.toString().toIntOrNull() ?: min
            val newValue = (current + step).coerceIn(min, max)
            display.text = newValue.toString()
            sendConfigCommand(command, newValue.toString())
        }
    }

    private fun sendConfigCommand(command: ServoCommand, value: String) {
        if (value.isNotBlank() && BluetoothConnection.isConnected.value == true) {
            BluetoothConnection.sendCommand("${command.value}:$value")
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
            applyConfigFromMessage(message)
            logMessage(message)
        }

        BluetoothConnection.errors.observe(this) { error ->
            logMessage(getString(R.string.error_prefix, error))
        }
    }

    private fun loadConfigFromCache() {
        for (cached in BluetoothConnection.logCache) {
            applyConfigFromMessage(cached)
        }
    }

    private fun applyConfigFromMessage(message: String) {
        val parts = message.split(":", limit = 2)
        if (parts.size != 2) return

        val key = parts[0].trim()
        val value = parts[1].trim()

        configListenersActive = false
        when (key) {
            ServoCommand.CONFIG_PAN_SPEED.value -> tvPanSpeed.text = value
            ServoCommand.CONFIG_TILT_SPEED.value -> tvTiltSpeed.text = value
            ServoCommand.CONFIG_AUTOPAN_SPEED.value -> tvAutopanSpeed.text = value
            ServoCommand.CONFIG_PAN_REVERSED.value -> togglePanReversed.check(if (value == "1") R.id.btnPanReversedOn else R.id.btnPanReversedOff)
            ServoCommand.CONFIG_TILT_REVERSED.value -> toggleTiltReversed.check(if (value == "1") R.id.btnTiltReversedOn else R.id.btnTiltReversedOff)
            ServoCommand.MAX_PAN_ANGLE.value -> tvMaxPanAngle.text = value
            ServoCommand.MAX_TILT_ANGLE.value -> tvMaxTiltAngle.text = value
        }
        configListenersActive = true
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
        val nestedScrollView = tvLog.parent as? NestedScrollView ?: return
        nestedScrollView.post {
            nestedScrollView.scrollTo(0, tvLog.bottom)
        }
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }
}