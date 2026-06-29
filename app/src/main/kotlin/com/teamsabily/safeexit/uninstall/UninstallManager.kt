package com.teamsabily.safeexit.uninstall

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.app.admin.DevicePolicyManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class UninstallManager(private val context: Context) {

    /**
     * Represents the result of a single package uninstall operation.
     */
    data class UninstallResult(
        val packageName: String,
        val appName: String,
        val success: Boolean
    )

    private val _results = MutableSharedFlow<UninstallResult>(replay = 0, extraBufferCapacity = 64)

    /** Observable flow of uninstall results as they arrive. */
    val results: SharedFlow<UninstallResult> = _results.asSharedFlow()

    /**
     * Initiates uninstall for each package in [packages].
     *
     * Each entry is a pair of (packageName, appName). For packages that are not
     * currently installed, a success result is emitted immediately without
     * attempting uninstall.
     *
     * Results are emitted to the [results] SharedFlow as each uninstall completes.
     *
     * @param packages List of (packageName, appName) pairs to uninstall.
     */
    fun uninstallPackages(packages: List<Pair<String, String>>) {
        if (packages.isEmpty()) return

        val sessionId = UUID.randomUUID().toString()
        val action = "com.teamsabily.safeexit.UNINSTALL_RESULT_$sessionId"

        // Build a lookup map from packageName -> appName for resolving results
        val packageToName = packages.associate { it.first to it.second }

        // Track how many results we're still waiting for
        val pendingCount = AtomicInteger(packages.size)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val status = intent.getIntExtra(
                    PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE
                )
                val pkg = intent.getStringExtra("extra_package_name") ?: return
                val name = packageToName[pkg] ?: pkg

                val success = status == PackageInstaller.STATUS_SUCCESS

                _results.tryEmit(UninstallResult(pkg, name, success))

                if (pendingCount.decrementAndGet() <= 0) {
                    try {
                        ctx.unregisterReceiver(this)
                    } catch (_: IllegalArgumentException) {
                        // Receiver already unregistered
                    }
                }
            }
        }

        val filter = IntentFilter(action)
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

        var requestCode = 0

        for ((packageName, appName) in packages) {
            if (!isPackageInstalled(packageName)) {
                // Package is already gone — emit success immediately
                _results.tryEmit(UninstallResult(packageName, appName, true))
                if (pendingCount.decrementAndGet() <= 0) {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: IllegalArgumentException) {
                        // Receiver already unregistered
                    }
                }
                continue
            }

            val intent = Intent(action).apply {
                setPackage(context.packageName)
                putExtra("extra_package_name", packageName)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode++,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            try {
                context.packageManager.packageInstaller.uninstall(
                    packageName,
                    pendingIntent.intentSender
                )
            } catch (e: Exception) {
                _results.tryEmit(UninstallResult(packageName, appName, false))
                if (pendingCount.decrementAndGet() <= 0) {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: IllegalArgumentException) {
                        // Receiver already unregistered
                    }
                }
            }
        }
    }

    /**
     * Checks whether this app is the Device Owner.
     *
     * Silent (no-confirmation) uninstall via [PackageInstaller] requires
     * the calling app to be the Device Owner.
     *
     * @return true if this app is the device owner, false otherwise.
     */
    fun isDeviceOwner(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    /**
     * Checks whether a package is currently installed on the device.
     *
     * @param packageName The package name to check.
     * @return true if the package is installed, false otherwise.
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
