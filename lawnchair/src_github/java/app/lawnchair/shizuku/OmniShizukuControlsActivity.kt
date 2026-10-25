/*
 * Copyright (C) 2026 Omni Launcher contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package app.lawnchair.shizuku

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import rikka.shizuku.Shizuku
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * GitHub-flavor-only controls. Every privileged operation is an allowlisted preset in UserService.
 */
class OmniShizukuControlsActivity : Activity() {

    private lateinit var statusView: TextView
    private lateinit var permissionButton: Button
    private lateinit var balancedButton: Button
    private lateinit var snappyButton: Button
    private lateinit var restoreButton: Button

    private val worker: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { task -> Thread(task, "OmniShizukuControls") }
    }

    @Volatile
    private var service: IOmniShizukuService? = null
    private var bindRequested = false

    private val serviceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(packageName, OmniShizukuUserService::class.java.name),
        )
            .tag("omni-system-controls-v1")
            .daemon(false)
            .processNameSuffix("omni-controls")
            .debuggable(com.android.launcher3.BuildConfig.DEBUG)
            .version(1)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IOmniShizukuService.Stub.asInterface(binder)
            runOnUiThread {
                statusView.text = "Connected to Shizuku. Reading settings…"
                setControlsEnabled(true)
            }
            refreshStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bindRequested = false
            runOnUiThread {
                statusView.text = "Shizuku service disconnected. Reconnect to continue."
                setControlsEnabled(false)
            }
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread { refreshConnectionState() }
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        service = null
        bindRequested = false
        runOnUiThread {
            statusView.text = "Shizuku stopped. Start it again, then reconnect."
            setControlsEnabled(false)
        }
    }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_PERMISSION) {
                runOnUiThread {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        connectUserService()
                    } else {
                        statusView.text = "Permission was not granted. No system settings were changed."
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Omni System Controls"
        buildContent()

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        refreshConnectionState()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        if (bindRequested) {
            runCatching { Shizuku.unbindUserService(serviceArgs, serviceConnection, true) }
        }
        service = null
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun buildContent() {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        statusView = TextView(this).apply {
            textSize = 14f
            text = "Checking Shizuku…"
            setPadding(0, dp(12), 0, dp(20))
        }
        val description = TextView(this).apply {
            textSize = 15f
            text = "These presets change Android's three global animation scales only. They do not overclock the CPU/GPU or change thermal limits. Shizuku must be started by you and its permission must be approved."
            setPadding(0, 0, 0, dp(16))
        }

        permissionButton = button("Connect / request Shizuku permission") {
            requestPermissionOrConnect()
        }
        balancedButton = button("Balanced animations · 0.75×") {
            applyPreset(PRESET_BALANCED, "Balanced preset applied.")
        }
        snappyButton = button("Snappy animations · 0.5×") {
            applyPreset(PRESET_SNAPPY, "Snappy preset applied.")
        }
        restoreButton = button("Restore Android default · 1.0×") {
            applyPreset(PRESET_DEFAULT, "Default animation scales restored.")
        }
        setControlsEnabled(false)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            addView(description, matchWidthWrapHeight())
            addView(permissionButton, matchWidthWrapHeight())
            addView(balancedButton, matchWidthWrapHeight())
            addView(snappyButton, matchWidthWrapHeight())
            addView(restoreButton, matchWidthWrapHeight())
            addView(statusView, matchWidthWrapHeight())
        }
        setContentView(ScrollView(this).apply { addView(content) })
    }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { action() }
        }

    private fun matchWidthWrapHeight() =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun setControlsEnabled(connected: Boolean) {
        balancedButton.isEnabled = connected && service != null
        snappyButton.isEnabled = connected && service != null
        restoreButton.isEnabled = connected && service != null
    }

    private fun refreshConnectionState() {
        if (!Shizuku.pingBinder()) {
            statusView.text = "Shizuku is not running. Start it from the Shizuku app, then tap Connect."
            setControlsEnabled(false)
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            connectUserService()
        } else {
            statusView.text = "Shizuku is available. Tap Connect to grant this app permission."
            setControlsEnabled(false)
        }
    }

    private fun requestPermissionOrConnect() {
        if (!Shizuku.pingBinder()) {
            statusView.text = "Shizuku is not running. Start it first (wireless debugging works on Android 11+)."
            setControlsEnabled(false)
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            connectUserService()
        } else {
            Shizuku.requestPermission(REQUEST_PERMISSION)
        }
    }

    private fun connectUserService() {
        if (bindRequested || service != null) return
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            statusView.text = "Start Shizuku and grant permission before connecting."
            setControlsEnabled(false)
            return
        }
        bindRequested = true
        statusView.text = "Connecting to the restricted Shizuku service…"
        try {
            Shizuku.bindUserService(serviceArgs, serviceConnection)
        } catch (error: Throwable) {
            bindRequested = false
            statusView.text = "Connection failed: ${error.javaClass.simpleName}: ${error.message.orEmpty()}"
            setControlsEnabled(false)
        }
    }

    private fun refreshStatus() {
        val current = service ?: return
        worker.execute {
            val message = runCatching { current.getStatus() }
                .getOrElse { "Could not read settings: ${it.javaClass.simpleName}: ${it.message.orEmpty()}" }
            runOnUiThread { statusView.text = message }
        }
    }

    private fun applyPreset(preset: Int, successMessage: String) {
        val current = service
        if (current == null) {
            statusView.text = "Connect to Shizuku first."
            setControlsEnabled(false)
            return
        }
        setControlsEnabled(false)
        statusView.text = "Applying the selected preset…"
        worker.execute {
            val message = runCatching {
                if (current.applyAnimationPreset(preset)) {
                    "$successMessage\n\n${current.getStatus()}"
                } else {
                    "Could not apply all settings. Check Shizuku state and try again."
                }
            }.getOrElse { "Operation failed: ${it.javaClass.simpleName}: ${it.message.orEmpty()}" }
            runOnUiThread {
                statusView.text = message
                setControlsEnabled(service != null)
            }
        }
    }

    private companion object {
        const val REQUEST_PERMISSION = 7401
        const val PRESET_DEFAULT = 0
        const val PRESET_BALANCED = 1
        const val PRESET_SNAPPY = 2
    }
}
