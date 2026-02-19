package com.pben.panservoclient

import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar: Toolbar = findViewById(R.id.toolbar_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        tvLog = findViewById(R.id.tvLog)
        tvStatus = findViewById(R.id.tvSettingsStatus)

        observeBluetoothState()
    }

    private fun observeBluetoothState() {
        BluetoothConnection.isConnected.observe(this) { isConnected ->
            tvStatus.text = if (isConnected) "Status: Connected" else "Status: Disconnected"
        }

        BluetoothConnection.messages.observe(this) { message ->
            logMessage(message)
        }

        BluetoothConnection.errors.observe(this) { error ->
            logMessage("ERROR: $error")
        }
    }

    private fun logMessage(message: String) {
        tvLog.append("$message\n")
        (tvLog.parent as? ScrollView)?.post { (tvLog.parent as ScrollView).fullScroll(View.FOCUS_DOWN) }
    }
}