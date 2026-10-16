/*
 * Copyright (C) 2026 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.customization.picker.color.custom.ui.viewmodel

import android.content.theming.ThemeStyle
import androidx.test.filters.SmallTest
import com.android.customization.picker.color.custom.data.CustomThemeRepository
import com.android.customization.picker.color.custom.data.SavedThemesLoadState
import com.android.customization.picker.color.custom.data.WriteResult
import com.android.customization.picker.color.custom.shared.model.CustomThemeDefinition
import com.android.customization.picker.color.custom.ui.contrast.CustomThemeContrastChecker
import com.android.systemui.monet.CustomThemePalette
import com.android.systemui.monet.CustomThemePaletteGenerator
import com.android.systemui.monet.CustomThemeSeeds
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SmallTest
@RunWith(RobolectricTestRunner::class)
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
    fun apply_warningRequiresExplicitConfirmation() {
        val repository = FakeRepository()
        val checker =
            object : CustomThemeContrastChecker() {
                override fun check(palette: CustomThemePalette) = listOf("Filled button")
            }
        val viewModel = CustomThemeStudioViewModel(repository, { 0.0 }, checker)
        viewModel.setName("Aurora")

        viewModel.requestApply()
        assertThat(repository.applied).isNull()
        assertThat(viewModel.uiState.value.awaitingApplyConfirmation).isTrue()

        viewModel.confirmApplyAnyway()
        assertThat(repository.applied?.name).isEqualTo("Aurora")
        assertThat(viewModel.uiState.value.lastWriteResult).isEqualTo(WriteResult.SUCCESS)
    }

    @Test
    fun invalidSeedRecovery_withNameEnteredLast_refreshesPreviewBeforeApply() {
        val repository = FakeRepository()
        val viewModel = CustomThemeStudioViewModel(repository, { 0.0 })
        viewModel.setName("")
        viewModel.setSeed(PaletteMode.LIGHT, SeedFamily.PRIMARY, "YRRP low")

        for (family in SeedFamily.values()) {
            viewModel.setSeed(PaletteMode.LIGHT, family, "#FFFF66")
        }

        val yellow = 0xFFFFFF66.toInt()
        val expected = CustomThemePaletteGenerator.generate(
            CustomThemeSeeds(yellow, yellow, yellow, yellow),
            null,
            false,
            ThemeStyle.VIBRANT,
            0.0,
        )
        assertThat(viewModel.uiState.value.canSave).isFalse()
        assertThat(viewModel.uiState.value.previewPalette.lightDynamicColors)
            .isEqualTo(expected.lightDynamicColors)
        assertThat(viewModel.uiState.value.previewPalette.darkDynamicColors)
            .isEqualTo(expected.darkDynamicColors)

        viewModel.setName("YRRP low")
        assertThat(viewModel.uiState.value.canSave).isTrue()
        val before = viewModel.uiState.value
        viewModel.save()
        viewModel.requestApply()
        if (viewModel.uiState.value.awaitingApplyConfirmation) viewModel.confirmApplyAnyway()

        assertThat(repository.applied?.light?.primary).isEqualTo(yellow)
        assertThat(viewModel.uiState.value.light).isEqualTo(before.light)
        assertThat(viewModel.uiState.value.previewPalette).isSameInstanceAs(before.previewPalette)

        viewModel.setSeed(PaletteMode.LIGHT, SeedFamily.PRIMARY, "#E53935")
        assertThat(viewModel.uiState.value.previewPalette.lightDynamicColors)
            .isNotEqualTo(expected.lightDynamicColors)
    }

    @Test
    fun invalidDarkSeedRecovery_refreshesSeparateDarkPreview() {
        val viewModel = CustomThemeStudioViewModel(FakeRepository(), { 0.0 })
        viewModel.setSeparateDark(true)
        val light = viewModel.uiState.value.previewPalette.lightDynamicColors
        viewModel.setSeed(PaletteMode.DARK, SeedFamily.TERTIARY, "invalid")
        val retained = viewModel.uiState.value.previewPalette

        viewModel.setSeed(PaletteMode.DARK, SeedFamily.TERTIARY, "#43A047")

        assertThat(viewModel.uiState.value.canSave).isTrue()
        assertThat(viewModel.uiState.value.previewPalette.lightDynamicColors).isEqualTo(light)
        assertThat(viewModel.uiState.value.previewPalette.darkDynamicColors)
            .isNotEqualTo(retained.darkDynamicColors)
    }

    private class FakeRepository : CustomThemeRepository {
        override val state: StateFlow<SavedThemesLoadState> =
            MutableStateFlow(SavedThemesLoadState.Valid(emptyList()))
        var applied: CustomThemeDefinition? = null

        override fun refresh() = Unit
        override fun save(theme: CustomThemeDefinition): WriteResult = WriteResult.SUCCESS
        override fun delete(id: UUID): WriteResult = WriteResult.SUCCESS
        override fun reset(): WriteResult = WriteResult.SUCCESS
        override fun apply(theme: CustomThemeDefinition): WriteResult {
            applied = theme
            return WriteResult.SUCCESS
        }
    }
}
