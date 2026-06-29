package lab.insan.safeexit.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import lab.insan.safeexit.data.AppInfo
import lab.insan.safeexit.data.AppRepository
import lab.insan.safeexit.uninstall.UninstallManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val uninstallManager = UninstallManager(application)

    private val _selectedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val selectedApps: StateFlow<List<AppInfo>> = _selectedApps.asStateFlow()

    private val _isDeviceOwner = MutableStateFlow(false)
    val isDeviceOwner: StateFlow<Boolean> = _isDeviceOwner.asStateFlow()

    private val _uninstallResults = MutableStateFlow<List<UninstallManager.UninstallResult>>(emptyList())
    val uninstallResults: StateFlow<List<UninstallManager.UninstallResult>> = _uninstallResults.asStateFlow()

    private val _showResults = MutableStateFlow(false)
    val showResults: StateFlow<Boolean> = _showResults.asStateFlow()

    private val _isUninstalling = MutableStateFlow(false)
    val isUninstalling: StateFlow<Boolean> = _isUninstalling.asStateFlow()

    private var expectedResultCount = 0

    init {
        checkDeviceOwner()
        loadSelectedApps()
        collectUninstallResults()
    }

    private fun checkDeviceOwner() {
        _isDeviceOwner.value = uninstallManager.isDeviceOwner()
    }

    private fun loadSelectedApps() {
        viewModelScope.launch {
            val selectedPackages = repository.getSelectedPackages()
            val pm = getApplication<Application>().packageManager
            val apps = selectedPackages.map { packageName ->
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon: Drawable? = try {
                        pm.getApplicationIcon(appInfo)
                    } catch (_: Exception) {
                        null
                    }
                    AppInfo(
                        packageName = packageName,
                        appName = appName,
                        icon = icon,
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                    AppInfo(
                        packageName = packageName,
                        appName = packageName.substringAfterLast('.'),
                        icon = null,
                    )
                }
            }
            _selectedApps.value = apps
        }
    }

    fun executePanicUninstall() {
        val apps = _selectedApps.value
        if (apps.isEmpty()) return

        viewModelScope.launch {
            _isUninstalling.value = true
            _uninstallResults.value = emptyList()
            expectedResultCount = apps.size
            val packages = apps.map { Pair(it.packageName, it.appName) }
            uninstallManager.uninstallPackages(packages)
        }
    }

    private fun collectUninstallResults() {
        viewModelScope.launch {
            uninstallManager.results.collect { result ->
                val currentResults = _uninstallResults.value.toMutableList()
                currentResults.add(result)
                _uninstallResults.value = currentResults

                if (result.success) {
                    repository.removePackage(result.packageName)
                }

                if (currentResults.size >= expectedResultCount && expectedResultCount > 0) {
                    _showResults.value = true
                    _isUninstalling.value = false
                }
            }
        }
    }

    fun dismissResults() {
        _showResults.value = false
        _uninstallResults.value = emptyList()
        expectedResultCount = 0
        loadSelectedApps()
    }

    fun refreshSelectedApps() {
        loadSelectedApps()
    }
}
