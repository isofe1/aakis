package com.example.ui

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryItem
import com.example.data.HistoryRepository
import com.example.data.UserPreferences
import com.example.engine.ArabicReshaperEngine
import com.example.service.FloatingBubbleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HistoryRepository
    val userPrefs = UserPreferences(application)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HistoryRepository(db.historyDao())
    }

    val historyList: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteList: StateFlow<List<HistoryItem>> = repository.favoriteHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reshaper Input State
    private val _inputText = MutableStateFlow(
        if (userPrefs.isFirstLaunch) {
            userPrefs.isFirstLaunch = false
            "مرحبا بكم في تطبيق عاكس"
        } else {
            ""
        }
    )
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _convertNumbers = MutableStateFlow(userPrefs.convertNumbersToIndic)
    val convertNumbers: StateFlow<Boolean> = _convertNumbers.asStateFlow()

    private val _reshapedText = MutableStateFlow("")
    val reshapedText: StateFlow<String> = _reshapedText.asStateFlow()

    // Overlay Permission & Bubble State
    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    // Preferences State
    private val _bubbleSizeDp = MutableStateFlow(userPrefs.bubbleSizeDp)
    val bubbleSizeDp: StateFlow<Int> = _bubbleSizeDp.asStateFlow()

    private val _bubbleOpacity = MutableStateFlow(userPrefs.bubbleOpacity)
    val bubbleOpacity: StateFlow<Float> = _bubbleOpacity.asStateFlow()

    private val _popupWidthDp = MutableStateFlow(userPrefs.popupWidthDp)
    val popupWidthDp: StateFlow<Int> = _popupWidthDp.asStateFlow()

    private val _popupOpacity = MutableStateFlow(userPrefs.popupOpacity)
    val popupOpacity: StateFlow<Float> = _popupOpacity.asStateFlow()

    private val _autoFocusKeyboard = MutableStateFlow(userPrefs.autoFocusKeyboard)
    val autoFocusKeyboard: StateFlow<Boolean> = _autoFocusKeyboard.asStateFlow()

    private val _isDarkMode = MutableStateFlow(userPrefs.isDarkMode)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    private val _customPresets = MutableStateFlow(userPrefs.customPresets)
    val customPresets: StateFlow<List<String>> = _customPresets.asStateFlow()

    init {
        FloatingBubbleService.bubbleSizeState.value = userPrefs.bubbleSizeDp
        FloatingBubbleService.bubbleOpacityState.value = userPrefs.bubbleOpacity
        FloatingBubbleService.popupWidthState.value = userPrefs.popupWidthDp
        FloatingBubbleService.popupOpacityState.value = userPrefs.popupOpacity
        FloatingBubbleService.autoFocusKeyboardState.value = userPrefs.autoFocusKeyboard
        updateReshapedOutput()
        checkOverlayPermission()
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
        updateReshapedOutput()
    }

    fun toggleConvertNumbers() {
        _convertNumbers.value = !_convertNumbers.value
        userPrefs.convertNumbersToIndic = _convertNumbers.value
        updateReshapedOutput()
    }

    private fun updateReshapedOutput() {
        val input = _inputText.value
        _reshapedText.value = ArabicReshaperEngine.reshape(input, convertNumbers = _convertNumbers.value)
    }

    fun saveCurrentToHistory() {
        val orig = _inputText.value
        val shaped = _reshapedText.value
        if (orig.isNotBlank()) {
            viewModelScope.launch {
                repository.insert(orig, shaped)
            }
        }
    }

    fun toggleFavorite(item: HistoryItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun checkOverlayPermission() {
        val context = getApplication<Application>()
        val granted = Settings.canDrawOverlays(context)
        _hasOverlayPermission.value = granted
        if (!granted) {
            _isServiceRunning.value = false
            userPrefs.isFloatingServiceEnabled = false
        } else {
            _isServiceRunning.value = userPrefs.isFloatingServiceEnabled
        }
    }

    fun toggleFloatingService() {
        val context = getApplication<Application>()
        val granted = Settings.canDrawOverlays(context)
        _hasOverlayPermission.value = granted

        if (granted) {
            if (_isServiceRunning.value) {
                FloatingBubbleService.stopService(context)
                _isServiceRunning.value = false
                userPrefs.isFloatingServiceEnabled = false
            } else {
                FloatingBubbleService.startService(context)
                _isServiceRunning.value = true
                userPrefs.isFloatingServiceEnabled = true
            }
        }
    }

    fun updateBubbleSize(sizeDp: Int) {
        _bubbleSizeDp.value = sizeDp
        userPrefs.bubbleSizeDp = sizeDp
        FloatingBubbleService.bubbleSizeState.value = sizeDp
    }

    fun updateBubbleOpacity(opacity: Float) {
        _bubbleOpacity.value = opacity
        userPrefs.bubbleOpacity = opacity
        FloatingBubbleService.bubbleOpacityState.value = opacity
    }

    fun updatePopupWidth(widthDp: Int) {
        _popupWidthDp.value = widthDp
        userPrefs.popupWidthDp = widthDp
        FloatingBubbleService.popupWidthState.value = widthDp
    }

    fun updatePopupOpacity(opacity: Float) {
        _popupOpacity.value = opacity
        userPrefs.popupOpacity = opacity
        FloatingBubbleService.popupOpacityState.value = opacity
    }

    fun updateAutoFocusKeyboard(enabled: Boolean) {
        _autoFocusKeyboard.value = enabled
        userPrefs.autoFocusKeyboard = enabled
        FloatingBubbleService.autoFocusKeyboardState.value = enabled
    }

    fun setDarkModePreference(dark: Boolean?) {
        _isDarkMode.value = dark
        userPrefs.isDarkMode = dark
    }

    fun applyPreset(text: String) {
        _inputText.value = text
        updateReshapedOutput()
    }

    fun addCustomPreset(phrase: String) {
        val trimmed = phrase.trim()
        if (trimmed.isNotBlank()) {
            val current = _customPresets.value.toMutableList()
            if (!current.contains(trimmed)) {
                current.add(trimmed)
                _customPresets.value = current
                userPrefs.customPresets = current
            }
        }
    }

    fun deleteCustomPreset(phrase: String) {
        val current = _customPresets.value.toMutableList()
        if (current.remove(phrase)) {
            _customPresets.value = current
            userPrefs.customPresets = current
        }
    }
}
