package com.pben.panservoclient

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val hc05Uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Cambia este nombre por el que pusiste en SerialBT.begin()
    private val deviceName = "PanTilt"   // o "PanTiltMG996R"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnConnect: Button = findViewById(R.id.btnConnect)
        val seekBar: SeekBar = findViewById(R.id.seekBarAngle)
        val tvAngle: TextView = findViewById(R.id.tvAngle)
        val tvStatus: TextView = findViewById(R.id.tvStatus)
        val btnLeft: Button = findViewById(R.id.btnLeft)
        val btnCenter: Button = findViewById(R.id.btnCenter)
        val btnRight: Button = findViewById(R.id.btnRight)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Inicializa slider en 90°
        seekBar.progress = 90
        tvAngle.text = "Ángulo actual: 90°"

        // Conectar / desconectar
        btnConnect.setOnClickListener {
            if (bluetoothSocket?.isConnected == true) {
                disconnect()
                btnConnect.text = "Conectar Bluetooth"
                tvStatus.text = "Estado: Desconectado"
            } else {
                requestBluetoothPermission()
            }
        }

        // Slider controla ángulo
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    sendCommand("P$progress")
                    tvAngle.text = "Ángulo actual: $progress°"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Botones rápidos
        btnLeft.setOnClickListener { sendCommand("P30") }
        btnCenter.setOnClickListener { sendCommand("P90") }
        btnRight.setOnClickListener { sendCommand("P150") }
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
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(hc05Uuid)
                        bluetoothSocket?.connect()
                        outputStream = bluetoothSocket?.outputStream
                        Toast.makeText(this, "Conectado a $deviceName", Toast.LENGTH_SHORT).show()
                    } catch (e: IOException) {
                        Toast.makeText(this, "Error al conectar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "No se encontró $deviceName. Empareja primero.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "Activa Bluetooth primero", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendCommand(command: String) {
        try {
            outputStream?.write("$command\n".toByteArray())
        } catch (e: IOException) {
            Toast.makeText(this, "Error enviando comando", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            // ignorar
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }
}
