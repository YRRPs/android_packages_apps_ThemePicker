/* Copyright (C) 2026 The Android Open Source Project */
package com.android.customization.picker.color.custom.ui.activity

import android.app.UiModeManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.customization.picker.color.custom.data.CustomThemeRepositoryImpl
import com.android.customization.picker.color.custom.data.SavedThemesCodec
import com.android.customization.picker.color.custom.data.SettingsSecureCustomThemeSettingsStore
import com.android.customization.picker.color.custom.ui.compose.CustomThemeStudioScreen
import com.android.customization.picker.color.custom.ui.viewmodel.CustomThemeStudioViewModel

class CustomThemeStudioActivity : ComponentActivity() {
    private val viewModel: CustomThemeStudioViewModel by viewModels {
        val repository =
            CustomThemeRepositoryImpl(
                SettingsSecureCustomThemeSettingsStore(applicationContext),
                SavedThemesCodec(),
            )
        val uiModeManager = getSystemService(UiModeManager::class.java)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CustomThemeStudioViewModel(
                    repository = repository,
                    contrastProvider = { uiModeManager.contrast.toDouble() },
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CustomThemeStudioScreen(viewModel, onClose = ::finish) } }
    }
}
