package com.example.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("arabic_reshaper_prefs", Context.MODE_PRIVATE)

    var bubbleSizeDp: Int
        get() = prefs.getInt(KEY_BUBBLE_SIZE, 56) // 44, 56, 68
        set(value) = prefs.edit().putInt(KEY_BUBBLE_SIZE, value).apply()

    var bubbleOpacity: Float
        get() = prefs.getFloat(KEY_BUBBLE_OPACITY, 0.9f) // 0.3f to 1.0f
        set(value) = prefs.edit().putFloat(KEY_BUBBLE_OPACITY, value).apply()

    var popupWidthDp: Int
        get() = prefs.getInt(KEY_POPUP_WIDTH, 290)
        set(value) = prefs.edit().putInt(KEY_POPUP_WIDTH, value).apply()

    var popupOpacity: Float
        get() = prefs.getFloat(KEY_POPUP_OPACITY, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_POPUP_OPACITY, value).apply()

    var autoCopyOnReshape: Boolean
        get() = prefs.getBoolean(KEY_AUTO_COPY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_COPY, value).apply()

    var convertNumbersToIndic: Boolean
        get() = prefs.getBoolean(KEY_CONVERT_NUMBERS, false)
        set(value) = prefs.edit().putBoolean(KEY_CONVERT_NUMBERS, value).apply()

    var isFloatingServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_ENABLED, value).apply()

    var lastBubbleX: Int
        get() = prefs.getInt(KEY_BUBBLE_X, 100)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_X, value).apply()

    var lastBubbleY: Int
        get() = prefs.getInt(KEY_BUBBLE_Y, 300)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_Y, value).apply()

    var isDarkMode: Boolean?
        get() {
            val contains = prefs.contains(KEY_DARK_MODE)
            return if (contains) prefs.getBoolean(KEY_DARK_MODE, false) else null
        }
        set(value) {
            if (value == null) {
                prefs.edit().remove(KEY_DARK_MODE).apply()
            } else {
                prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            }
        }

    var autoFocusKeyboard: Boolean
        get() = prefs.getBoolean(KEY_AUTO_FOCUS_KEYBOARD, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_FOCUS_KEYBOARD, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, value).apply()

    var customPresets: List<String>
        get() {
            val raw = prefs.getString(KEY_CUSTOM_PRESETS, null)
            if (raw == null) {
                return listOf("عاكس الكلام")
            }
            return if (raw.isBlank()) emptyList() else raw.split("|||")
        }
        set(value) {
            prefs.edit().putString(KEY_CUSTOM_PRESETS, value.joinToString("|||")).apply()
        }

    companion object {
        private const val KEY_BUBBLE_SIZE = "bubble_size_dp"
        private const val KEY_BUBBLE_OPACITY = "bubble_opacity"
        private const val KEY_POPUP_WIDTH = "popup_width_dp"
        private const val KEY_POPUP_OPACITY = "popup_opacity"
        private const val KEY_AUTO_COPY = "auto_copy"
        private const val KEY_CONVERT_NUMBERS = "convert_numbers"
        private const val KEY_FLOATING_ENABLED = "floating_enabled"
        private const val KEY_BUBBLE_X = "bubble_x"
        private const val KEY_BUBBLE_Y = "bubble_y"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_AUTO_FOCUS_KEYBOARD = "auto_focus_keyboard"
        private const val KEY_CUSTOM_PRESETS = "custom_presets"
    }
}
