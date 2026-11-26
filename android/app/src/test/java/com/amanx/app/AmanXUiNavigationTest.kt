package com.amanx.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.domain.model.ThemeMode
import com.amanx.app.ui.screens.WelcomeOnboardingScreen
import com.amanx.app.ui.theme.AmanXTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmanXUiNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun welcomeScreen_togglesLanguageRtlLtrAndAdvances() {
        var advanced = false

        composeRule.setContent {
            var currentLang by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(AppLanguage.AR)
            }
            AmanXTheme(themeMode = ThemeMode.DARK, language = currentLang) {
                WelcomeOnboardingScreen(
                    language = currentLang,
                    onLanguageChange = { currentLang = it },
                    onNext = { advanced = true }
                )
            }
        }

        composeRule.onNodeWithText("مرحباً بك في AMAN X").assertIsDisplayed()
        composeRule.onNodeWithText("English").performClick()
        composeRule.onNodeWithText("Welcome to AMAN X").assertIsDisplayed()
        composeRule.onNodeWithText("Continue to Security Setup").performClick()
        assertTrue(advanced)
    }
}
