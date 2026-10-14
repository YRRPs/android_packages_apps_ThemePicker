/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.customization.picker.color.custom.data

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface CustomThemeSettingsStore {
    fun readSavedThemes(): String?

    fun writeSavedThemes(value: String): Boolean

    fun readActiveTheme(): String?

    fun writeActiveTheme(value: String): Boolean
}

@Singleton
class SettingsSecureCustomThemeSettingsStore
@Inject
constructor(@ApplicationContext context: Context) : CustomThemeSettingsStore {
    private val resolver = context.contentResolver

    override fun readSavedThemes(): String? =
        Settings.Secure.getStringForUser(
            resolver,
            Settings.Secure.THEME_CUSTOMIZATION_SAVED_THEMES,
            UserHandle.USER_CURRENT,
        )

    override fun writeSavedThemes(value: String): Boolean =
        Settings.Secure.putStringForUser(
            resolver,
            Settings.Secure.THEME_CUSTOMIZATION_SAVED_THEMES,
            value,
            UserHandle.USER_CURRENT,
        )

    override fun readActiveTheme(): String? =
        Settings.Secure.getStringForUser(
            resolver,
            Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
            UserHandle.USER_CURRENT,
        )

    override fun writeActiveTheme(value: String): Boolean =
        Settings.Secure.putStringForUser(
            resolver,
            Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
            value,
            UserHandle.USER_CURRENT,
        )
}
