package com.pben.panservoclient

import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object BluetoothConnection {
    var bluetoothSocket: BluetoothSocket? = null
    var outputStream: OutputStream? = null
    var inputStream: InputStream? = null

    fun sendCommand(command: String) {
        if (bluetoothSocket?.isConnected == true) {
            try {
                outputStream?.write("$command\n".toByteArray())
            } catch (e: IOException) {
                // Handle error
            }
        }
    }
}
