package com.teamsabily.safeexit.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class AppRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "safeexit_prefs"
        private const val KEY_SELECTED_PACKAGES = "selected_packages"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the set of package names the user has selected for emergency uninstall.
     */
    fun getSelectedPackages(): Set<String> {
        return prefs.getStringSet(KEY_SELECTED_PACKAGES, emptySet()) ?: emptySet()
    }

    /**
     * Replaces the entire selected packages set with the given [packages].
     */
    fun saveSelectedPackages(packages: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_SELECTED_PACKAGES, packages)
            .apply()
    }

    /**
     * Adds a single package to the selected set.
     */
    fun addPackage(pkg: String) {
        val current = getSelectedPackages().toMutableSet()
        current.add(pkg)
        saveSelectedPackages(current)
    }

    /**
     * Removes a single package from the selected set.
     */
    fun removePackage(pkg: String) {
        val current = getSelectedPackages().toMutableSet()
        current.remove(pkg)
        saveSelectedPackages(current)
    }

    /**
     * Queries the PackageManager for installed apps.
     *
     * @param includeSystem If true, system apps are included in the results.
     *                      If false, only user-installed apps are returned.
     * @return A sorted list of [AppInfo] representing installed applications,
     *         excluding this app itself.
     */
    fun getInstalledApps(includeSystem: Boolean = false): List<AppInfo> {
        val pm: PackageManager = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return installedApps
            .filter { appInfo ->
                // Always exclude our own app
                if (appInfo.packageName == context.packageName) return@filter false

                if (includeSystem) {
                    true
                } else {
                    // Only include apps that are NOT system apps
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
            }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(pm).toString(),
                    icon = try {
                        appInfo.loadIcon(pm)
                    } catch (_: Exception) {
                        null
                    }
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
}
