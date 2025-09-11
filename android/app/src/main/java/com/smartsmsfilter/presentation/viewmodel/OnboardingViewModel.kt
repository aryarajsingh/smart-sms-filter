package com.smartsmsfilter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsmsfilter.data.preferences.PreferencesManager
import com.smartsmsfilter.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    // Current user preferences
    val userPreferences: StateFlow<UserPreferences?> = preferencesManager.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    fun saveUserPreferences(preferences: UserPreferences) {
        _uiState.value = _uiState.value.copy(isSaving = true)
        
        viewModelScope.launch {
            try {
                preferencesManager.saveUserPreferences(preferences)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isCompleted = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save preferences: ${e.message}"
                )
            }
        }
    }
    
    fun skipOnboarding() {
        _uiState.value = _uiState.value.copy(isSaving = true)
        
        viewModelScope.launch {
            try {
                preferencesManager.setOnboardingCompleted(true)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isCompleted = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to complete onboarding: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class OnboardingUiState(
    val isSaving: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)
