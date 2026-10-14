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
package com.android.customization.picker.color.custom.shared.model

import android.content.theming.ThemeStyle
import com.android.systemui.monet.CustomThemeContract
import com.android.systemui.monet.CustomThemeSeeds
import java.util.Locale
import java.util.UUID

data class CustomThemeDefinition(
    val id: UUID,
    val name: String,
    @ThemeStyle.Type val style: Int,
    val separateDark: Boolean,
    val light: CustomThemeSeeds,
    val dark: CustomThemeSeeds?,
) {
    init {
        require(name.trim().length in 1..64) { "Theme name must contain 1 to 64 characters" }
        CustomThemeContract.requireSupportedStyle(style)
        require(separateDark == (dark != null)) { "Dark seeds must match separate-dark mode" }
    }

    fun normalizedName(): String = name.trim().lowercase(Locale.ROOT)

    fun toActiveTheme(): CustomThemeContract.ActiveTheme =
        CustomThemeContract.ActiveTheme(id, style, separateDark, light, dark)
}
