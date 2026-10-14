/*
 * Copyright (C) 2026 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.customization.picker.color.custom.ui.viewmodel

import android.content.theming.ThemeStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.customization.picker.color.custom.data.CustomThemeRepository
import com.android.customization.picker.color.custom.data.SavedThemesLoadState
import com.android.customization.picker.color.custom.data.WriteResult
import com.android.customization.picker.color.custom.shared.model.CustomThemeDefinition
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CustomThemeStudioViewModelTest {
    @Test
    fun invalidHex_keepsLastValidPreviewPalette() {
        val viewModel = CustomThemeStudioViewModel(FakeRepository(), { 0.0 })
        val valid = viewModel.uiState.value.previewPalette

        viewModel.setSeed(PaletteMode.LIGHT, SeedFamily.PRIMARY, "XYZ")

        assertThat(viewModel.uiState.value.previewPalette).isEqualTo(valid)
        assertThat(viewModel.uiState.value.canSave).isFalse()
    }

    @Test
    fun separateDark_copiesLinkedSeedsBeforeIndependentEdit() {
        val viewModel = CustomThemeStudioViewModel(FakeRepository(), { 0.0 })

        viewModel.setSeparateDark(true)

        assertThat(viewModel.uiState.value.dark).isEqualTo(viewModel.uiState.value.light)
    }

    @Test
    fun apply_savesAndAppliesCurrentTheme() {
        val repository = FakeRepository()
        val viewModel = CustomThemeStudioViewModel(repository, { 0.0 })
        viewModel.setName("Aurora")

        viewModel.apply()

        assertThat(repository.applied?.name).isEqualTo("Aurora")
        assertThat(viewModel.uiState.value.lastWriteResult).isEqualTo(WriteResult.SUCCESS)
    }

    private class FakeRepository : CustomThemeRepository {
        override val state: StateFlow<SavedThemesLoadState> =
            MutableStateFlow(SavedThemesLoadState.Valid(emptyList()))
        var applied: CustomThemeDefinition? = null

        override fun refresh() = Unit
        override fun save(theme: CustomThemeDefinition): WriteResult = WriteResult.SUCCESS
        override fun delete(id: UUID): WriteResult = WriteResult.SUCCESS
        override fun apply(theme: CustomThemeDefinition): WriteResult {
            applied = theme
            return WriteResult.SUCCESS
        }
    }
}
