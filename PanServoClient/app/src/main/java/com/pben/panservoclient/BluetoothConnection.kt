package com.pben.panservoclient

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.*

object BluetoothConnection {
    var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    val isConnected = MutableLiveData(false)
    val messages = MutableLiveData<String>()
    val errors = MutableLiveData<String>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var listeningJob: Job? = null

    private val hc05Uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val deviceName = "PanTilt"

    fun connect(context: Context, adapter: BluetoothAdapter) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            errors.postValue("Bluetooth permission not granted.")
            return
        }
        val device: BluetoothDevice? = adapter.bondedDevices.find { it.name == deviceName }
        if (device == null) {
            errors.postValue("$deviceName not found. Please pair first.")
            return
        }

        scope.launch {
            try {
                val socket = device.createRfcommSocketToServiceRecord(hc05Uuid)
                socket.connect()
                bluetoothSocket = socket
                outputStream = socket.outputStream
                inputStream = socket.inputStream
                isConnected.postValue(true)
                startListening()
            } catch (e: IOException) {
                isConnected.postValue(false)
                errors.postValue("Error connecting: ${e.message}")
            }
        }
    }

    fun disconnect() {
        stopListening()
        scope.launch {
            try {
                inputStream?.close()
                outputStream?.close()
                bluetoothSocket?.close()
            } catch (e: IOException) { /* Ignore errors on close */ }
            bluetoothSocket = null
            isConnected.postValue(false)
        }
    }

    private fun startListening() {
        if (isConnected.value != true || listeningJob?.isActive == true) return
        listeningJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                while (isActive) {
                    val response = reader.readLine() ?: break
                    messages.postValue(response)
                }
            } catch (e: IOException) {
                if (isActive) {
                    errors.postValue("Connection lost: ${e.message}")
                    isConnected.postValue(false)
                }
            }
        }
    }

    private fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
    }

    fun sendCommand(command: String) {
        if (isConnected.value != true) {
            errors.postValue("Not connected.")
            return
        }
        scope.launch {
            try {
                outputStream?.write("$command\n".toByteArray())
            } catch (e: IOException) {
                errors.postValue("Error sending command: ${e.message}")
            }
        }
    }
}