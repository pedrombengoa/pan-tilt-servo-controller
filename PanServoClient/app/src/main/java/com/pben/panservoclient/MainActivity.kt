package com.pben.panservoclient

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var btnConnect: ImageButton
    private lateinit var btnPlayPause: MaterialButton

    private val hc05Uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Change this to the name you set in SerialBT.begin()
    private val deviceName = "PanTilt"   // or "PanTiltMG996R"

    private val handler = Handler(Looper.getMainLooper())
    private var isHolding = false
    private var isAutopan = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnConnect = findViewById(R.id.btnConnect)
        val seekBar: SeekBar = findViewById(R.id.seekBarAngle)
        val tvAngle: TextView = findViewById(R.id.tvAngle)
        val tvStatus: TextView = findViewById(R.id.tvStatus)
        val btnSkipPrevious: MaterialButton = findViewById(R.id.btnSkipPrevious)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        val btnStop: MaterialButton = findViewById(R.id.btnStop)
        val btnSkipNext: MaterialButton = findViewById(R.id.btnSkipNext)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        updateButtonState(false)

        // Initialize slider at 90°
        seekBar.progress = 90
        tvAngle.text = "Current Angle: 90°"

        // Connect / disconnect
        btnConnect.setOnClickListener {
            if (BluetoothConnection.bluetoothSocket?.isConnected == true) {
                disconnect()
                tvStatus.text = "Status: Disconnected"
            } else {
                requestBluetoothPermission()
            }
        }

        // Slider controls angle
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    BluetoothConnection.sendCommand("P$progress")
                    tvAngle.text = "Current Angle: $progress°"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Quick buttons
        btnSkipPrevious.setOnTouchListener { _, event ->
            handleTouch(event, seekBar, tvAngle, -1)
            true
        }

        btnSkipNext.setOnTouchListener { _, event ->
            handleTouch(event, seekBar, tvAngle, 1)
            true
        }

        btnPlayPause.setOnClickListener { 
            isAutopan = !isAutopan
            if (isAutopan) {
                BluetoothConnection.sendCommand("AUTOPAN")
                btnPlayPause.setIconResource(R.drawable.ic_pause)
            } else {
                BluetoothConnection.sendCommand("P${seekBar.progress}")
                btnPlayPause.setIconResource(R.drawable.ic_play_arrow)
            }
        }

        btnStop.setOnClickListener {
            isAutopan = false
            btnPlayPause.setIconResource(R.drawable.ic_play_arrow)
            BluetoothConnection.sendCommand("RESET")
            seekBar.progress = 90
            tvAngle.text = "Current Angle: 90°"
        }
    }

    override fun onUserLeaveHint() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            enterPictureInPictureMode(PictureInPictureParams.Builder()
                .setAspectRatio(Rational(1, 1))
                .build())
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        val controlsCard = findViewById<View>(R.id.controlsCard)
        if (isInPictureInPictureMode) {
            // Hide the controls in PiP mode
            controlsCard.visibility = View.GONE
        } else {
            // Restore the controls when exiting PiP mode
            controlsCard.visibility = View.VISIBLE
        }
    }

    private fun handleTouch(event: MotionEvent, seekBar: SeekBar, tvAngle: TextView, direction: Int) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isHolding = true
                handler.post(object : Runnable {
                    override fun run() {
                        if (isHolding) {
                            val command = if (direction == -1) "LEFT" else "RIGHT"
                            BluetoothConnection.sendCommand(command)

                            val newProgress = seekBar.progress + direction
                            if (newProgress in 0..180) {
                                seekBar.progress = newProgress
                                tvAngle.text = "Current Angle: $newProgress°"
                            }
                            handler.postDelayed(this, 100)
                        }
                    }
                })
            }
            MotionEvent.ACTION_UP -> {
                isHolding = false
            }
        }
    }

    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            connectToBluetooth()
        } else {
            Toast.makeText(this, "Bluetooth permission is required to connect", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                connectToBluetooth()
            }
        } else {
            connectToBluetooth()
        }
    }


    private fun connectToBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                val device: BluetoothDevice? = pairedDevices?.find { it.name == deviceName }

                if (device != null) {
                    try {
                        BluetoothConnection.bluetoothSocket = device.createRfcommSocketToServiceRecord(hc05Uuid)
                        BluetoothConnection.bluetoothSocket?.connect()
                        BluetoothConnection.outputStream = BluetoothConnection.bluetoothSocket?.outputStream
                        BluetoothConnection.inputStream = BluetoothConnection.bluetoothSocket?.inputStream
                        
                        // Update UI on successful connection
                        findViewById<TextView>(R.id.tvStatus).text = "Status: Connected to $deviceName"
                        Toast.makeText(this, "Connected to $deviceName", Toast.LENGTH_SHORT).show()
                        updateButtonState(true)

                    } catch (e: IOException) {
                        Toast.makeText(this, "Error connecting: ${e.message}", Toast.LENGTH_LONG).show()
                        updateButtonState(false)
                    }
                } else {
                    Toast.makeText(this, "$deviceName not found. Please pair first.", Toast.LENGTH_LONG).show()
                    updateButtonState(false)
                }
            } else {
                 // This part should ideally not be reached on newer APIs due to the permission check above
                 Toast.makeText(this, "Bluetooth permission not granted.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disconnect() {
        try {
            BluetoothConnection.outputStream?.close()
            BluetoothConnection.bluetoothSocket?.close()
            BluetoothConnection.outputStream = null
            BluetoothConnection.bluetoothSocket = null
            findViewById<TextView>(R.id.tvStatus).text = "Status: Disconnected"
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
            updateButtonState(false)
        } catch (e: IOException) {
            // ignore
        }
    }

    private fun updateButtonState(isConnected: Boolean) {
        if (isConnected) {
            btnConnect.setImageResource(R.drawable.ic_bluetooth)
        } else {
            btnConnect.setImageResource(R.drawable.ic_bluetooth_disabled)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }
}
