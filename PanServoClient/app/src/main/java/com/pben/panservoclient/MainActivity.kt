package com.pben.panservoclient

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var btnConnect: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private lateinit var speedometer: ImageView
    private lateinit var tvAngle: TextView
    private lateinit var tvTiltAngle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnUp: ImageButton
    private lateinit var btnDown: ImageButton

    private val handler = Handler(Looper.getMainLooper())
    private var isHolding = false
    private var isAutopan = false
    private var currentAngle: Int = 90

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BluetoothConnection.init(application)
        setContentView(R.layout.activity_main)
        enableImmersiveMode()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnConnect = findViewById(R.id.btnConnect)
        speedometer = findViewById(R.id.speedometer)
        tvAngle = findViewById(R.id.tvAngle)
        tvTiltAngle = findViewById(R.id.tvTiltAngle)
        tvStatus = findViewById(R.id.tvStatus)
        val btnSkipPrevious: ImageButton = findViewById(R.id.btnSkipPrevious)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        val btnStop: ImageButton = findViewById(R.id.btnStop)
        val btnSkipNext: ImageButton = findViewById(R.id.btnSkipNext)
        btnUp = findViewById(R.id.btnUp)
        btnDown = findViewById(R.id.btnDown)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        observeBluetoothState()
        updateAngleUI(currentAngle)
        updatePipParams()

        btnConnect.setOnClickListener {
            if (BluetoothConnection.isConnected.value == true) {
                BluetoothConnection.disconnect()
            } else {
                requestBluetoothPermission()
            }
        }

        btnSkipPrevious.setOnTouchListener { _, event ->
            isAutopan = !isAutopan
            updatePlayPauseButton()
            handleContinuousPress(event, ServoCommand.LEFT)
            true
        }

        btnSkipNext.setOnTouchListener { _, event ->
            isAutopan = !isAutopan
            updatePlayPauseButton()
            handleContinuousPress(event, ServoCommand.RIGHT)
            true
        }

        btnPlayPause.setOnClickListener {
            isAutopan = !isAutopan
            updatePlayPauseButton()
            BluetoothConnection.sendCommand(ServoCommand.AUTOPAN)
        }

        btnStop.setOnClickListener {
            isAutopan = false
            updatePlayPauseButton()
            BluetoothConnection.sendCommand(ServoCommand.RESET)
        }

        btnUp.setOnTouchListener { _, event ->
            handleContinuousPress(event, ServoCommand.UP)
            true
        }

        btnDown.setOnTouchListener { _, event ->
            handleContinuousPress(event, ServoCommand.DOWN)
            true
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
            tvStatus.text = if (isConnected) getString(R.string.status_connected) else getString(R.string.status_disconnected)
        }

        BluetoothConnection.messages.observe(this) { message ->
            // Nuevo formato: Channel: Joystick | Command: LEFT | Pan: 89 | Tilt: 90
            val regex = Regex("Pan: (\\d+) \\| Tilt: (\\d+)")
            val match = regex.find(message)
            if (match != null) {
                val pan = match.groupValues[1].toIntOrNull()
                val tilt = match.groupValues[2].toIntOrNull()
                if (pan != null) {
                    currentAngle = pan
                    updateAngleUI(currentAngle)
                }
                if (tilt != null) {
                    tvTiltAngle.text = getString(R.string.angle_format, tilt)
                }
            }
        }

        BluetoothConnection.errors.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePlayPauseButton() {
        btnPlayPause.setImageResource(if (isAutopan) R.drawable.ic_pause_white else R.drawable.ic_play_arrow_white)
    }

    private fun updateAngleUI(position: Int) {
        tvAngle.text = getString(R.string.angle_format, position)
        // 90 = norte (0°), 0 = oeste (-90°), 180 = este (+90°)
        val rotation = (position - 90).toFloat()
        speedometer.rotation = rotation
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

    private fun handleContinuousPress(event: MotionEvent, command: ServoCommand) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isHolding = true
                handler.post(object : Runnable {
                    override fun run() {
                        if (isHolding) {
                            BluetoothConnection.sendCommand(command)
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
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show()
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