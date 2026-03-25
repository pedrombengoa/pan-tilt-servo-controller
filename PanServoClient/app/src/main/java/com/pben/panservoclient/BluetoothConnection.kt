package com.pben.panservoclient

import android.app.Application
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
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
    private lateinit var appContext: Application

    val isConnected = MutableLiveData(false)
    val messages = MutableLiveData<String>()
    val errors = MutableLiveData<String>()
    val logCache = mutableListOf<String>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var listeningJob: Job? = null
    @Volatile
    private var isConnecting = false

    private val hc05Uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val deviceName = "PanTilt"

    fun init(application: Application) {
        appContext = application
    }

    private fun getString(resId: Int, vararg args: Any): String {
        return appContext.getString(resId, *args)
    }

    fun connect(context: Context, adapter: BluetoothAdapter) {
        if (isConnected.value == true || isConnecting) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            errors.postValue(getString(R.string.error_bluetooth_permission))
            return
        }
        val device: BluetoothDevice? = adapter.bondedDevices.find { it.name == deviceName }
        if (device == null) {
            errors.postValue(getString(R.string.error_device_not_found, deviceName))
            return
        }

        isConnecting = true
        scope.launch {
            try {
                val socket = device.createInsecureRfcommSocketToServiceRecord(hc05Uuid)
                socket.connect()
                bluetoothSocket = socket
                outputStream = socket.outputStream
                inputStream = socket.inputStream
                isConnected.postValue(true)
                startListening()
            } catch (e: IOException) {
                isConnected.postValue(false)
                errors.postValue(getString(R.string.error_connecting, e.message ?: ""))
            } finally {
                isConnecting = false
            }
        }
    }

    fun disconnect() {
        Log.d("BluetoothConnection", "Disconnect called")
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
        if (listeningJob?.isActive == true) return
        listeningJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                while (isActive) {
                    val response = reader.readLine() ?: break
                    messages.postValue(response)
                    logCache.add(response)
                }
            } catch (e: IOException) {
                if (isActive) {
                    val errorMsg = getString(R.string.error_connection_lost, e.message ?: "")
                    errors.postValue(errorMsg)
                    logCache.add(getString(R.string.error_prefix, errorMsg))
                    isConnected.postValue(false)
                }
            }
        }
    }

    private fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
    }

    fun sendCommand(command: ServoCommand) {
        sendCommand(command.value)
    }

    fun sendCommand(command: String) {
        if (isConnected.value != true) {
            val errorMsg = getString(R.string.error_not_connected)
            errors.postValue(errorMsg)
            logCache.add(getString(R.string.error_prefix, errorMsg))
            return
        }
        scope.launch {
            try {
                outputStream?.write("$command\n".toByteArray())
            } catch (e: IOException) {
                val errorMsg = getString(R.string.error_sending_command, e.message ?: "")
                errors.postValue(errorMsg)
                logCache.add(getString(R.string.error_prefix, errorMsg))
            }
        }
    }

    fun clearLogs() {
        logCache.clear()
    }
}