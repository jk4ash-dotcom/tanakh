package com.tanakhpoc.learner

import android.app.Application
import com.tanakhpoc.learner.data.DssVariantRepository
import com.tanakhpoc.learner.data.PackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TanakhApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _repository = MutableStateFlow<PackRepository?>(null)
    val repository: StateFlow<PackRepository?> = _repository.asStateFlow()

    private val _dssRepository = MutableStateFlow<DssVariantRepository?>(null)
    val dssRepository: StateFlow<DssVariantRepository?> = _dssRepository.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching { PackRepository.load(this@TanakhApp) }
                .onSuccess { _repository.value = it }
                .onFailure { _loadError.value = it.message ?: "Failed to load pack" }
            // DSS pack is optional: empty on failure → no chrome.
            runCatching { DssVariantRepository.load(this@TanakhApp) }
                .onSuccess { _dssRepository.value = it }
                .onFailure { _dssRepository.value = DssVariantRepository.empty() }
        }
    }

    companion object {
        fun from(context: android.content.Context): TanakhApp =
            context.applicationContext as TanakhApp
    }
}
