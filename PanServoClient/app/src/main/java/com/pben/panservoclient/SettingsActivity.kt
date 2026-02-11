package com.pben.panservoclient

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvCentroXValue: TextView
    private lateinit var tvDeadzoneValue: TextView
    private lateinit var tvNeutralValue: TextView
    private lateinit var tvManualPaceValue: TextView
    private lateinit var tvAutoPaceValue: TextView
    private lateinit var tvLog: TextView

    private val defaultCentroX = 2048
    private val defaultDeadzone = 100
    private val defaultNeutral = 90
    private val defaultManualPace = 10
    private val defaultAutoPace = 5

    private var centroX = defaultCentroX
    private var deadzone = defaultDeadzone
    private var neutral = defaultNeutral
    private var manualPace = defaultManualPace
    private var autoPace = defaultAutoPace

    private var listeningJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar: Toolbar = findViewById(R.id.toolbar_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvCentroXValue = findViewById(R.id.tvCentroXValue)
        tvDeadzoneValue = findViewById(R.id.tvDeadzoneValue)
        tvNeutralValue = findViewById(R.id.tvNeutralValue)
        tvManualPaceValue = findViewById(R.id.tvManualPaceValue)
        tvAutoPaceValue = findViewById(R.id.tvAutoPaceValue)
        tvLog = findViewById(R.id.tvLog)

        findViewById<ImageButton>(R.id.btnCentroXMinus).setOnClickListener { updateValue("CAL_X", -1) }
        findViewById<ImageButton>(R.id.btnCentroXPlus).setOnClickListener { updateValue("CAL_X", 1) }
        findViewById<ImageButton>(R.id.btnDeadzoneMinus).setOnClickListener { updateValue("CAL_DZ", -1) }
        findViewById<ImageButton>(R.id.btnDeadzonePlus).setOnClickListener { updateValue("CAL_DZ", 1) }
        findViewById<ImageButton>(R.id.btnNeutralMinus).setOnClickListener { updateValue("CAL_N", -1) }
        findViewById<ImageButton>(R.id.btnNeutralPlus).setOnClickListener { updateValue("CAL_N", 1) }
        findViewById<ImageButton>(R.id.btnManualPaceMinus).setOnClickListener { updateValue("PAN_MP", -1) }
        findViewById<ImageButton>(R.id.btnManualPacePlus).setOnClickListener { updateValue("PAN_MP", 1) }
        findViewById<ImageButton>(R.id.btnAutoPaceMinus).setOnClickListener { updateValue("PAN_AP", -1) }
        findViewById<ImageButton>(R.id.btnAutoPacePlus).setOnClickListener { updateValue("PAN_AP", 1) }

        findViewById<Button>(R.id.btnReset).setOnClickListener { resetToDefaults() }
        findViewById<Button>(R.id.btnReadParams).setOnClickListener { fetchSettings() }
    }

    override fun onResume() {
        super.onResume()
        listenForBluetoothMessages()
    }

    override fun onPause() {
        super.onPause()
        listeningJob?.cancel()
    }

    private fun listenForBluetoothMessages() {
        listeningJob?.cancel()
        if (BluetoothConnection.bluetoothSocket?.isConnected == true) {
            listeningJob = lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val reader = BufferedReader(InputStreamReader(BluetoothConnection.inputStream))
                    while (true) {
                        val response = reader.readLine() ?: break
                        withContext(Dispatchers.Main) {
                            logMessage(response)
                            if (response.startsWith("CAL_X")) {
                                parseSettings(response)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if(e.message?.contains("Socket closed") == false) {
                        withContext(Dispatchers.Main) {
                            logMessage("Error listening for messages: ${e.message}")
                        }
                    }
                }
            }
        } else {
            logMessage("Not connected")
        }
    }

    private fun updateValue(command: String, amount: Int) {
        when (command) {
            "CAL_X" -> centroX = (centroX + amount).coerceIn(0, 4095)
            "CAL_DZ" -> deadzone = (deadzone + amount).coerceIn(0, 500)
            "CAL_N" -> neutral = (neutral + amount).coerceIn(0, 180)
            "PAN_MP" -> manualPace = (manualPace + amount).coerceIn(1, 20)
            "PAN_AP" -> autoPace = (autoPace + amount).coerceIn(1, 20)
        }
        BluetoothConnection.sendCommand("$command:${getCurrentValue(command)}")
        updateUi()
    }

    private fun getCurrentValue(command: String): Int {
        return when (command) {
            "CAL_X" -> centroX
            "CAL_DZ" -> deadzone
            "CAL_N" -> neutral
            "PAN_MP" -> manualPace
            "PAN_AP" -> autoPace
            else -> 0
        }
    }

    private fun fetchSettings() {
        BluetoothConnection.sendCommand("INFO")
    }

    private fun parseSettings(settings: String?) {
        if (settings.isNullOrBlank()) {
            logMessage("Received empty or invalid settings from device")
            return
        }

        val settingsMap = settings.split(",").mapNotNull { part ->
            val pair = part.split(":")
            if (pair.size == 2) {
                pair[0].trim() to pair[1].trim()
            } else {
                null
            }
        }.toMap()

        centroX = settingsMap["CAL_X"]?.toIntOrNull() ?: centroX
        deadzone = settingsMap["CAL_DZ"]?.toIntOrNull() ?: deadzone
        neutral = settingsMap["CAL_N"]?.toIntOrNull() ?: neutral
        manualPace = settingsMap["PAN_MP"]?.toIntOrNull() ?: manualPace
        autoPace = settingsMap["PAN_AP"]?.toIntOrNull() ?: autoPace

        updateUi()
    }

    private fun resetToDefaults() {
        centroX = defaultCentroX
        deadzone = defaultDeadzone
        neutral = defaultNeutral
        manualPace = defaultManualPace
        autoPace = defaultAutoPace

        BluetoothConnection.sendCommand("CAL_X:$centroX")
        BluetoothConnection.sendCommand("CAL_DZ:$deadzone")
        BluetoothConnection.sendCommand("CAL_N:$neutral")
        BluetoothConnection.sendCommand("PAN_MP:$manualPace")
        BluetoothConnection.sendCommand("PAN_AP:$autoPace")

        updateUi()
        logMessage("Settings reset to defaults")
    }

    private fun updateUi() {
        tvCentroXValue.text = centroX.toString()
        tvDeadzoneValue.text = deadzone.toString()
        tvNeutralValue.text = neutral.toString()
        tvManualPaceValue.text = manualPace.toString()
        tvAutoPaceValue.text = autoPace.toString()
    }

    private fun logMessage(message: String) {
        tvLog.append("$message\n")
        val scrollview = tvLog.parent as? ScrollView
        scrollview?.post { scrollview.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
