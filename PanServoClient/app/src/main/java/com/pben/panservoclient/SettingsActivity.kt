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
    private lateinit var btnResetConfig: Button
    private lateinit var spinnerLanguage: Spinner

    // Language codes matching the spinner order (español first = default)
    private val languageCodes = arrayOf("es", "en")

    private lateinit var togglePanReversed: MaterialButtonToggleGroup
    private lateinit var toggleTiltReversed: MaterialButtonToggleGroup
    private lateinit var tvMaxPanAngle: TextView
    private lateinit var tvMaxTiltAngle: TextView
    private lateinit var tvServoStepMs: TextView
    private lateinit var tvAutoPanStepMs: TextView

    private var configListenersActive = false

    companion object {
        private const val ANGLE_MIN = 0
        private const val ANGLE_MAX = 270
        private const val ANGLE_STEP = 10
        private const val SERVO_STEP_MS_MIN = 10
        private const val SERVO_STEP_MS_MAX = 2000
        private const val SERVO_STEP_MS_STEP = 10
        private const val AUTO_PAN_STEP_MS_MIN = 10
        private const val AUTO_PAN_STEP_MS_MAX = 2000
        private const val AUTO_PAN_STEP_MS_STEP = 50
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

        // Config fields
        togglePanReversed = findViewById(R.id.togglePanReversed)
        toggleTiltReversed = findViewById(R.id.toggleTiltReversed)
        tvMaxPanAngle = findViewById(R.id.tvMaxPanAngle)
        tvMaxTiltAngle = findViewById(R.id.tvMaxTiltAngle)
        tvServoStepMs = findViewById(R.id.tvServoStepMs)
        tvAutoPanStepMs = findViewById(R.id.tvAutoPanStepMs)

        setupConfigListeners()
        setupLanguageSelector()
        observeBluetoothState()

        btnClearLogs.setOnClickListener {
            BluetoothConnection.clearLogs()
            tvLog.text = ""
        }

        btnResetConfig = findViewById(R.id.btnResetConfig)
        btnResetConfig.setOnClickListener {
            BluetoothConnection.sendCommand(ServoCommand.RESET_CONFIG)
        }
    }

    override fun onResume() {
        super.onResume()
        loadConfigFromCache()
        displayLogCache()
    }

    private fun setupConfigListeners() {
        // Max angle +/- buttons
        setupStepperButtons(R.id.btnMaxPanAngleMinus, R.id.btnMaxPanAnglePlus, tvMaxPanAngle, ServoCommand.MAX_PAN_ANGLE, ANGLE_MIN, ANGLE_MAX, ANGLE_STEP)
        setupStepperButtons(R.id.btnMaxTiltAngleMinus, R.id.btnMaxTiltAnglePlus, tvMaxTiltAngle, ServoCommand.MAX_TILT_ANGLE, ANGLE_MIN, ANGLE_MAX, ANGLE_STEP)

        // Servo step ms +/- buttons
        setupStepperButtons(R.id.btnServoStepMsMinus, R.id.btnServoStepMsPlus, tvServoStepMs, ServoCommand.CONFIG_SERVO_STEP_MS, SERVO_STEP_MS_MIN, SERVO_STEP_MS_MAX, SERVO_STEP_MS_STEP)

        // Auto pan step ms +/- buttons
        setupStepperButtons(R.id.btnAutoPanStepMsMinus, R.id.btnAutoPanStepMsPlus, tvAutoPanStepMs, ServoCommand.CONFIG_AUTO_PAN_STEP_MS, AUTO_PAN_STEP_MS_MIN, AUTO_PAN_STEP_MS_MAX, AUTO_PAN_STEP_MS_STEP)

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
            getString(R.string.language_spanish),
            getString(R.string.language_english)
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        // Set current selection based on current app locale (default to español = index 0)
        val currentLocale = AppCompatDelegate.getApplicationLocales()
        val currentLang = if (currentLocale.isEmpty) "es" else currentLocale.get(0)?.language ?: "es"
        val selectedIndex = languageCodes.indexOf(currentLang).coerceAtLeast(0)
        spinnerLanguage.setSelection(selectedIndex)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val langCode = languageCodes[position]
                val currentAppLocale = AppCompatDelegate.getApplicationLocales()
                val currentLangCode = if (currentAppLocale.isEmpty) "es" else currentAppLocale.get(0)?.language ?: "es"

                if (langCode != currentLangCode) {
                    val localeList = LocaleListCompat.forLanguageTags(langCode)
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
            ServoCommand.CONFIG_PAN_REVERSED.value -> togglePanReversed.check(if (value == "1") R.id.btnPanReversedOn else R.id.btnPanReversedOff)
            ServoCommand.CONFIG_TILT_REVERSED.value -> toggleTiltReversed.check(if (value == "1") R.id.btnTiltReversedOn else R.id.btnTiltReversedOff)
            ServoCommand.MAX_PAN_ANGLE.value -> tvMaxPanAngle.text = value
            ServoCommand.MAX_TILT_ANGLE.value -> tvMaxTiltAngle.text = value
            ServoCommand.CONFIG_SERVO_STEP_MS.value -> tvServoStepMs.text = value
            ServoCommand.CONFIG_AUTO_PAN_STEP_MS.value -> tvAutoPanStepMs.text = value
            ServoCommand.RESET_POSITION.value -> {
                // Position reset — no config UI changes needed, server will re-send config if applicable
            }
            ServoCommand.RESET_CONFIG.value -> {
                // Reset config UI to placeholder — server will re-send actual config values
                val placeholder = getString(R.string.config_placeholder)
                togglePanReversed.check(R.id.btnPanReversedOff)
                toggleTiltReversed.check(R.id.btnTiltReversedOff)
                tvMaxPanAngle.text = placeholder
                tvMaxTiltAngle.text = placeholder
                tvServoStepMs.text = placeholder
                tvAutoPanStepMs.text = placeholder
            }
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