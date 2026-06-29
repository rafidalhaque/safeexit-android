package com.teamsabily.safeexit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teamsabily.safeexit.data.AppInfo
import com.teamsabily.safeexit.data.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _includeSystemApps = MutableStateFlow(false)
    val includeSystemApps: StateFlow<Boolean> = _includeSystemApps.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(
        _allApps,
        _searchQuery,
    ) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val selectedCount: StateFlow<Int> = _selectedPackages
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    init {
        loadSelectedPackages()
        loadApps()
    }

    private fun loadSelectedPackages() {
        viewModelScope.launch {
            _selectedPackages.value = repository.getSelectedPackages()
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps(includeSystem = _includeSystemApps.value)
            _allApps.value = apps.sortedBy { it.appName.lowercase() }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSystemApps() {
        _includeSystemApps.value = !_includeSystemApps.value
        loadApps()
    }

    fun toggleAppSelection(packageName: String) {
        viewModelScope.launch {
            val current = _selectedPackages.value.toMutableSet()
            if (current.contains(packageName)) {
                current.remove(packageName)
                repository.removePackage(packageName)
            } else {
                current.add(packageName)
                repository.addPackage(packageName)
            }
            _selectedPackages.value = current
        }
    }
}
