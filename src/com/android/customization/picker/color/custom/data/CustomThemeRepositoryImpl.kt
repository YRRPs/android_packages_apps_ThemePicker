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

import com.android.customization.picker.color.custom.shared.model.CustomThemeDefinition
import com.android.systemui.monet.CustomThemeContract
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

@Singleton
class CustomThemeRepositoryImpl
@Inject
constructor(
    private val store: CustomThemeSettingsStore,
    private val codec: SavedThemesCodec,
) : CustomThemeRepository {
    private val mutableState = MutableStateFlow<SavedThemesLoadState>(SavedThemesLoadState.Valid(emptyList()))
    override val state: StateFlow<SavedThemesLoadState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    override fun refresh() {
        mutableState.value = codec.decode(store.readSavedThemes())
    }

    override fun save(theme: CustomThemeDefinition): WriteResult {
        val themes = currentThemes() ?: return WriteResult.INVALID_STATE
        if (hasDuplicateName(themes, theme)) return WriteResult.VALIDATION_FAILED
        val updated = themes.filterNot { it.id == theme.id } + theme
        if (!store.writeSavedThemes(codec.encode(updated))) return WriteResult.SAVED_WRITE_FAILED
        mutableState.value = SavedThemesLoadState.Valid(updated)
        return WriteResult.SUCCESS
    }

    override fun delete(id: UUID): WriteResult {
        val themes = currentThemes() ?: return WriteResult.INVALID_STATE
        val updated = themes.filterNot { it.id == id }
        if (!store.writeSavedThemes(codec.encode(updated))) return WriteResult.SAVED_WRITE_FAILED
        mutableState.value = SavedThemesLoadState.Valid(updated)
        return WriteResult.SUCCESS
    }

    override fun reset(): WriteResult {
        if (!store.writeSavedThemes(codec.encode(emptyList()))) return WriteResult.SAVED_WRITE_FAILED
        mutableState.value = SavedThemesLoadState.Valid(emptyList())
        return WriteResult.SUCCESS
    }

    override fun apply(theme: CustomThemeDefinition): WriteResult {
        val saved = save(theme)
        if (saved != WriteResult.SUCCESS) return saved
        val currentRoot = parseActiveRoot() ?: return WriteResult.ACTIVE_WRITE_FAILED
        val active = CustomThemeContract.serialize(theme.toActiveTheme(), currentRoot).toString()
        if (!store.writeActiveTheme(active)) return WriteResult.ACTIVE_WRITE_FAILED
        return WriteResult.SUCCESS
    }

    private fun currentThemes(): List<CustomThemeDefinition>? =
        (mutableState.value as? SavedThemesLoadState.Valid)?.themes

    private fun hasDuplicateName(
        themes: List<CustomThemeDefinition>,
        candidate: CustomThemeDefinition,
    ): Boolean =
        themes.any { it.id != candidate.id && it.normalizedName() == candidate.normalizedName() }

    private fun parseActiveRoot(): JSONObject? {
        val raw = store.readActiveTheme()
        if (raw.isNullOrBlank()) return JSONObject()
        return try {
            JSONObject(raw)
        } catch (exception: Exception) {
            null
        }
    }
}
