package com.harvest.autoanswer

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = buildList {
        add(android.Manifest.permission.READ_PHONE_STATE)
        add(android.Manifest.permission.READ_PHONE_NUMBERS)
        add(android.Manifest.permission.CALL_PHONE)
        add(android.Manifest.permission.ANSWER_PHONE_CALLS)
        add(android.Manifest.permission.READ_CALL_LOG)
        add(android.Manifest.permission.WRITE_CALL_LOG)
        add(android.Manifest.permission.READ_CONTACTS)
        add(android.Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshStatus()
        }

    private val roleRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<android.widget.Button>(R.id.grantPermissionsButton).setOnClickListener {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }

        findViewById<android.widget.Button>(R.id.setDefaultDialerButton).setOnClickListener {
            requestDefaultDialer()
        }

        findViewById<android.widget.Switch>(R.id.autoAnswerSwitch).apply {
            isChecked = Prefs.isAutoAnswerEnabled(context)
            setOnCheckedChangeListener { _, checked ->
                Prefs.setAutoAnswerEnabled(context, checked)
                if (checked && !isDefaultDialer()) {
                    Toast.makeText(
                        context,
                        "Won't take effect until this app is set as the default dialer",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        findViewById<android.widget.Switch>(R.id.recordSwitch).apply {
            isChecked = Prefs.isRecordEnabled(context)
            setOnCheckedChangeListener { _, checked ->
                Prefs.setRecordEnabled(context, checked)
            }
        }

        findViewById<android.widget.Button>(R.id.callButton).setOnClickListener {
            val number = findViewById<EditText>(R.id.dialInput).text.toString().trim()
            if (number.isEmpty()) return@setOnClickListener
            if (hasPermission(android.Manifest.permission.CALL_PHONE)) {
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
            } else {
                Toast.makeText(this, "Grant permissions first", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<RecyclerView>(R.id.recordingsList).layoutManager =
            LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        refreshRecordings()
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                roleRequestLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }
        } else {
            @Suppress("DEPRECATION")
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            roleRequestLauncher.launch(intent)
        }
    }

    private fun isDefaultDialer(): Boolean {
        val telecomManager = getSystemService(TelecomManager::class.java)
        return telecomManager.defaultDialerPackage == packageName
    }

    private fun hasPermission(permission: String): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(this, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun refreshStatus() {
        val missing = requiredPermissions.filterNot { hasPermission(it) }
        val dialerSet = isDefaultDialer()

        val status = buildString {
            append(if (missing.isEmpty()) "✓ Permissions granted" else "✗ ${missing.size} permission(s) missing")
            append("\n")
            append(if (dialerSet) "✓ Set as default dialer" else "✗ Not the default dialer — auto-answer won't work yet")
        }
        findViewById<TextView>(R.id.statusText).text = status
    }

    private fun refreshRecordings() {
        val dir = File(getExternalFilesDir(null), "recordings")
        val files = (dir.listFiles()?.toList() ?: emptyList())
            .sortedByDescending { it.lastModified() }

        findViewById<RecyclerView>(R.id.recordingsList).adapter =
            RecordingsAdapter(this, files)
        findViewById<View>(R.id.emptyRecordingsText).visibility =
            if (files.isEmpty()) View.VISIBLE else View.GONE
    }
}
