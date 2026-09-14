package com.jayesh.cashcollect.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jayesh.cashcollect.data.repository.SettingsRepository
import com.jayesh.cashcollect.domain.model.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun saveBrotherNumber(number: String) {
        viewModelScope.launch {
            settingsRepo.updateBrotherWhatsAppNumber(number)
        }
    }

    fun saveCommissionRate(rate: Int) {
        viewModelScope.launch {
            settingsRepo.updateCommissionRate(rate)
        }
    }

    fun saveMessageTemplate(template: String) {
        viewModelScope.launch {
            settingsRepo.updateMessageTemplate(template)
        }
    }

    class Factory(
        private val settingsRepo: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepo) as T
        }
    }
}
