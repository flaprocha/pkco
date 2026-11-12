package com.example.pkco.ui.notes

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pkco.MainActivity
import com.example.pkco.R
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opens the create form, forces landscape and types a body of several lines: the Body field must be on screen,
 * at least [LANDSCAPE_BODY_MIN_HEIGHT] tall and show the text, reached by scrolling the form. Nothing is saved.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class NoteFormLandscapeUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val body = (1..12).joinToString("\n") { "Landscape line $it ${System.currentTimeMillis()}" }

    private fun setOrientation(orientation: Int) {
        rule.activityRule.scenario.onActivity { it.requestedOrientation = orientation }
    }

    private fun isLandscape(): Boolean {
        var landscape = false
        rule.activityRule.scenario.onActivity {
            landscape = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        return landscape
    }

    @Test
    fun landscapeForm_bodyFieldKeepsMinimumHeightAndShowsText() {
        rule.waitUntilAtLeastOneExists(
            hasContentDescription(rule.activity.getString(R.string.add_note)),
            TIMEOUT_MS
        )
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.add_note)).performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag(FormTestTags.BODY), TIMEOUT_MS)

        // The activity is recreated in landscape; the form is restored from rememberSaveable state.
        setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        rule.waitUntil(TIMEOUT_MS) { isLandscape() }
        rule.waitUntilAtLeastOneExists(hasTestTag(FormTestTags.BODY), TIMEOUT_MS)

        rule.onNodeWithTag(FormTestTags.BODY).performScrollTo().performTextInput(body)
        rule.onNodeWithTag(FormTestTags.BODY)
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(LANDSCAPE_BODY_MIN_HEIGHT)
            .assertTextContains(body)
    }

    @After
    fun restoreOrientation() {
        setOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
