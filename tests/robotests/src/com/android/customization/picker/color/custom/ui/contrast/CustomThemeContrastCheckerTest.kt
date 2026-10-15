/* Copyright (C) 2026 The Android Open Source Project */
package com.android.customization.picker.color.custom.ui.contrast

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SmallTest
@RunWith(RobolectricTestRunner::class)
class CustomThemeContrastCheckerTest {
    @Test
    fun weakPrimaryPair_identifiesFilledButton() {
        val roles = mapOf("primary" to 0xFF222222.toInt(), "on_primary" to 0xFF111111.toInt())

        val warnings = CustomThemeContrastChecker().checkRoles(roles)

        assertThat(warnings).contains("Filled button")
    }

    @Test
    fun strongPrimaryPair_hasNoFilledButtonWarning() {
        val roles = mapOf("primary" to 0xFF000000.toInt(), "on_primary" to 0xFFFFFFFF.toInt())

        val warnings = CustomThemeContrastChecker().checkRoles(roles)

        assertThat(warnings).doesNotContain("Filled button")
    }
}
