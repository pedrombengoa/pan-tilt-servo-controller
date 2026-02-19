package com.pben.panservoclient

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var btnConnect: ImageButton
    private lateinit var btnPlayPause: MaterialButton
    private lateinit var compassNeedle: ImageView
    private lateinit var tvAngle: TextView
    private lateinit var tvStatus: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isHolding = false
    private var isAutopan = false
    private var pollingJob: Job? = null
    private var currentAngle: Int = 90

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnConnect = findViewById(R.id.btnConnect)
        compassNeedle = findViewById(R.id.compassNeedle)
        tvAngle = findViewById(R.id.tvAngle)
        tvStatus = findViewById(R.id.tvStatus)
        val btnSkipPrevious: MaterialButton = findViewById(R.id.btnSkipPrevious)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        val btnStop: MaterialButton = findViewById(R.id.btnStop)
        val btnSkipNext: MaterialButton = findViewById(R.id.btnSkipNext)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        observeBluetoothState()
        updateAngleUI()
        updatePipParams()

        btnConnect.setOnClickListener {
            if (BluetoothConnection.isConnected.value == true) {
                BluetoothConnection.disconnect()
            } else {
                requestBluetoothPermission()
            }
        }

        btnSkipPrevious.setOnTouchListener { _, event ->
            handleTouch(event, -1)
            true
        }

        btnSkipNext.setOnTouchListener { _, event ->
            handleTouch(event, 1)
            true
        }

        btnPlayPause.setOnClickListener {
            isAutopan = !isAutopan
            updatePlayPauseButton()
            if (isAutopan) {
                BluetoothConnection.sendCommand("AUTOPAN")
            } else {
                BluetoothConnection.sendCommand("P$currentAngle")
            }
        }

        btnStop.setOnClickListener {
            isAutopan = false
            updatePlayPauseButton()
            BluetoothConnection.sendCommand("RESET")
        }

        requestBluetoothPermission()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeBluetoothState() {
        BluetoothConnection.isConnected.observe(this) { isConnected ->
            updateButtonState(isConnected)
            tvStatus.text = if (isConnected) "Status: Connected" else "Status: Disconnected"
            if (isConnected) {
                startPolling()
            } else {
                pollingJob?.cancel()
            }
        }

        BluetoothConnection.messages.observe(this) {
            if (it.startsWith("CAL_X")) {
                parseAngle(it)
            }
        }

        BluetoothConnection.errors.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePlayPauseButton() {
        btnPlayPause.setIconResource(if (isAutopan) R.drawable.ic_pause else R.drawable.ic_play_arrow)
    }

    private fun startPolling() {
        if (BluetoothConnection.isConnected.value != true) return
        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                BluetoothConnection.sendCommand("INFO")
                delay(1000)
            }
        }
    }

    private fun parseAngle(settings: String?) {
        if (settings.isNullOrBlank()) return

        val settingsMap = settings.split(",").mapNotNull { part ->
            val pair = part.split(":")
            if (pair.size == 2) pair[0].trim() to pair[1].trim() else null
        }.toMap()

        currentAngle = settingsMap["CAL_N"]?.toIntOrNull() ?: currentAngle
        updateAngleUI()
    }

    private fun updateAngleUI() {
        tvAngle.text = "Current Angle: $currentAngle°"
        compassNeedle.rotation = currentAngle.toFloat()
    }

    override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Handled by auto-enter in updatePipParams()
                } else {
                    enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        findViewById<View>(R.id.controlsCard).visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
    }

    private fun handleTouch(event: MotionEvent, direction: Int) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isHolding = true
                handler.post(object : Runnable {
                    override fun run() {
                        if (isHolding) {
                            BluetoothConnection.sendCommand(if (direction == -1) "LEFT" else "RIGHT")
                            handler.postDelayed(this, 100)
                        }
                    }
                })
            }
            MotionEvent.ACTION_UP -> isHolding = false
        }
    }

    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            BluetoothConnection.connect(this, bluetoothAdapter)
        } else {
            Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                BluetoothConnection.connect(this, bluetoothAdapter)
            }
        } else {
            BluetoothConnection.connect(this, bluetoothAdapter)
        }
    }

    private fun updateButtonState(isConnected: Boolean) {
        btnConnect.setImageResource(if (isConnected) R.drawable.ic_bluetooth else R.drawable.ic_bluetooth_disabled)
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(1, 1))
                .setAutoEnterEnabled(true)
                .build()
            setPictureInPictureParams(params)
        }
    }
}