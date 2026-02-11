package com.pben.panservoclient

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var btnConnect: FloatingActionButton

    private val hc05Uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Cambia este nombre por el que pusiste en SerialBT.begin()
    private val deviceName = "PanTilt"   // o "PanTiltMG996R"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnConnect = findViewById(R.id.btnConnect)
        val seekBar: SeekBar = findViewById(R.id.seekBarAngle)
        val tvAngle: TextView = findViewById(R.id.tvAngle)
        val tvStatus: TextView = findViewById(R.id.tvStatus)
        val btnLeft: MaterialButton = findViewById(R.id.btnLeft)
        val btnCenter: MaterialButton = findViewById(R.id.btnCenter)
        val btnRight: MaterialButton = findViewById(R.id.btnRight)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        updateButtonState(false)

        // Inicializa slider en 90°
        seekBar.progress = 90
        tvAngle.text = "Ángulo actual: 90°"

        // Conectar / desconectar
        btnConnect.setOnClickListener {
            if (BluetoothConnection.bluetoothSocket?.isConnected == true) {
                disconnect()
                tvStatus.text = "Estado: Desconectado"
            } else {
                requestBluetoothPermission()
            }
        }

        // Slider controla ángulo
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    BluetoothConnection.sendCommand("P$progress")
                    tvAngle.text = "Ángulo actual: $progress°"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Botones rápidos
        btnLeft.setOnClickListener { 
            BluetoothConnection.sendCommand("P30") 
            seekBar.progress = 30
            tvAngle.text = "Ángulo actual: 30°"
        }
        btnCenter.setOnClickListener { 
            BluetoothConnection.sendCommand("P90")
            seekBar.progress = 90
            tvAngle.text = "Ángulo actual: 90°"
        }
        btnRight.setOnClickListener { 
            BluetoothConnection.sendCommand("P150")
            seekBar.progress = 150
            tvAngle.text = "Ángulo actual: 150°"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
                        findViewById<TextView>(R.id.tvStatus).text = "Estado: Conectado a $deviceName"
                        Toast.makeText(this, "Conectado a $deviceName", Toast.LENGTH_SHORT).show()
                        updateButtonState(true)

                    } catch (e: IOException) {
                        Toast.makeText(this, "Error al conectar: ${e.message}", Toast.LENGTH_LONG).show()
                        updateButtonState(false)
                    }
                } else {
                    Toast.makeText(this, "No se encontró $deviceName. Empareja primero.", Toast.LENGTH_LONG).show()
                    updateButtonState(false)
                }
            } else {
                 // This part should ideally not be reached on newer APIs due to the permission check above
                 Toast.makeText(this, "Permiso de Bluetooth no concedido.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Activa Bluetooth primero", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disconnect() {
        try {
            BluetoothConnection.outputStream?.close()
            BluetoothConnection.bluetoothSocket?.close()
            BluetoothConnection.outputStream = null
            BluetoothConnection.bluetoothSocket = null
            findViewById<TextView>(R.id.tvStatus).text = "Estado: Desconectado"
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
            updateButtonState(false)
        } catch (e: IOException) {
            // ignorar
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
