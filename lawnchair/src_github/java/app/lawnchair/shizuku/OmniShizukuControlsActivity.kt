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
 * GitHub-flavor-only diagnostics. Shizuku is used to inspect gfxinfo, not to tune animation speed.
 */
class OmniShizukuControlsActivity : Activity() {

    private lateinit var statusView: TextView
    private lateinit var permissionButton: Button
    private lateinit var captureButton: Button
    private lateinit var resetStatsButton: Button

    private val worker: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { task -> Thread(task, "OmniShizukuDiagnostics") }
    }

    @Volatile
    private var service: IOmniShizukuService? = null
    private var bindRequested = false

    private val serviceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(packageName, OmniShizukuUserService::class.java.name),
        )
            .tag("omni-performance-diagnostics-v2")
            .daemon(false)
            .processNameSuffix("omni-performance")
            .debuggable(com.android.launcher3.BuildConfig.DEBUG)
            .version(2)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IOmniShizukuService.Stub.asInterface(binder)
            runOnUiThread {
                statusView.text = "Connected to Shizuku. Reading device diagnostics…"
                setDiagnosticsEnabled(true)
            }
            refreshStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bindRequested = false
            runOnUiThread {
                statusView.text = "Shizuku service disconnected. Reconnect to continue."
                setDiagnosticsEnabled(false)
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
            setDiagnosticsEnabled(false)
        }
    }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_PERMISSION) {
                runOnUiThread {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        connectUserService()
                    } else {
                        statusView.text = "Permission was not granted. No settings were changed."
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Omni Performance Diagnostics"
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
            textSize = 13f
            text = "Checking Shizuku…"
            setTextIsSelectable(true)
            setPadding(0, dp(12), 0, dp(20))
        }
        val description = TextView(this).apply {
            textSize = 15f
            text = "Read-only performance diagnostics for Omni. This screen does not change animation duration, refresh rate, CPU/GPU policy, or thermal settings. Start Shizuku yourself and approve the permission request. Reproduce an app-open / return-home hitch, then capture gfxinfo frame statistics."
            setPadding(0, 0, 0, dp(16))
        }

        permissionButton = button("Connect / request Shizuku permission") {
            requestPermissionOrConnect()
        }
        captureButton = button("Capture launcher frame statistics") {
            captureFrameStats()
        }
        resetStatsButton = button("Reset frame-stat counters") {
            resetFrameStats()
        }
        setDiagnosticsEnabled(false)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            addView(description, matchWidthWrapHeight())
            addView(permissionButton, matchWidthWrapHeight())
            addView(captureButton, matchWidthWrapHeight())
            addView(resetStatsButton, matchWidthWrapHeight())
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
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

    private fun setDiagnosticsEnabled(connected: Boolean) {
        captureButton.isEnabled = connected && service != null
        resetStatsButton.isEnabled = connected && service != null
    }

    private fun refreshConnectionState() {
        if (!Shizuku.pingBinder()) {
            statusView.text = "Shizuku is not running. Start it from the Shizuku app, then tap Connect."
            setDiagnosticsEnabled(false)
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            connectUserService()
        } else {
            statusView.text = "Shizuku is available. Tap Connect to grant this app permission."
            setDiagnosticsEnabled(false)
        }
    }

    private fun requestPermissionOrConnect() {
        if (!Shizuku.pingBinder()) {
            statusView.text = "Shizuku is not running. Start it first (wireless debugging works on Android 11+)."
            setDiagnosticsEnabled(false)
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
            setDiagnosticsEnabled(false)
            return
        }
        bindRequested = true
        statusView.text = "Connecting to the restricted diagnostics service…"
        try {
            Shizuku.bindUserService(serviceArgs, serviceConnection)
        } catch (error: Throwable) {
            bindRequested = false
            statusView.text = "Connection failed: ${error.javaClass.simpleName}: ${error.message.orEmpty()}"
            setDiagnosticsEnabled(false)
        }
    }

    private fun refreshStatus() {
        val current = service ?: return
        worker.execute {
            val message = runCatching { current.getStatus() }
                .getOrElse {
                    "Could not read diagnostics: ${it.javaClass.simpleName}: ${it.message.orEmpty()}"
                }
            runOnUiThread { statusView.text = message }
        }
    }

    private fun captureFrameStats() {
        val current = service
        if (current == null) {
            statusView.text = "Connect to Shizuku first."
            setDiagnosticsEnabled(false)
            return
        }
        setDiagnosticsEnabled(false)
        statusView.text = "Capturing frame statistics…"
        worker.execute {
            val message = runCatching { current.getFrameStats() }
                .getOrElse {
                    "Capture failed: ${it.javaClass.simpleName}: ${it.message.orEmpty()}"
                }
            runOnUiThread {
                statusView.text = message
                setDiagnosticsEnabled(service != null)
            }
        }
    }

    private fun resetFrameStats() {
        val current = service
        if (current == null) {
            statusView.text = "Connect to Shizuku first."
            setDiagnosticsEnabled(false)
            return
        }
        setDiagnosticsEnabled(false)
        statusView.text = "Resetting gfxinfo counters only…"
        worker.execute {
            val message = runCatching {
                if (current.resetFrameStats()) {
                    "Frame counters reset. This does not change animation or display settings.\n\n"
                        + current.getStatus()
                } else {
                    "Could not reset gfxinfo counters. Check that Shizuku is still running."
                }
            }.getOrElse {
                "Reset failed: ${it.javaClass.simpleName}: ${it.message.orEmpty()}"
            }
            runOnUiThread {
                statusView.text = message
                setDiagnosticsEnabled(service != null)
            }
        }
    }

    private companion object {
        const val REQUEST_PERMISSION = 7401
    }
}
