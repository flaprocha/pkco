package com.example.pkco.ui.notes

import android.content.pm.ActivityInfo
import android.view.Surface
import android.view.WindowInsets
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pkco.MainActivity
import com.example.pkco.R
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Forces landscape in both directions on the list and checks that the search field stays between the left and
 * right system bars and display cutout. With gesture navigation and no side cutout the check is trivially true.
 * Nothing is saved.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ListLandscapeInsetsUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun setOrientation(orientation: Int) {
        rule.activityRule.scenario.onActivity { it.requestedOrientation = orientation }
    }

    private fun rotation(): Int {
        var rotation = -1
        rule.activityRule.scenario.onActivity { rotation = it.display.rotation }
        return rotation
    }

    /** Left and right edges (px, window coordinates) of the area free of system bars and cutout. */
    private fun safeHorizontalRange(): Pair<Float, Float> {
        var range = 0f to 0f
        rule.activityRule.scenario.onActivity {
            val decor = it.window.decorView
            val insets = decor.rootWindowInsets
                .getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            range = insets.left.toFloat() to (decor.width - insets.right).toFloat()
        }
        return range
    }

    private fun searchFieldInsideSafeArea(): Boolean {
        val nodes = rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        if (nodes.size != 1) return false
        val bounds = nodes.single().boundsInWindow
        val (left, right) = safeHorizontalRange()
        return bounds.left >= left && bounds.right <= right
    }

    private fun checkOrientation(orientation: Int, expectedRotation: Int) {
        setOrientation(orientation)
        rule.waitUntil(TIMEOUT_MS) { rotation() == expectedRotation }
        rule.waitUntilAtLeastOneExists(hasSetTextAction(), TIMEOUT_MS)
        // Waits for the new insets to reach the layout; a field still under a bar times out.
        try {
            rule.waitUntil(TIMEOUT_MS) { searchFieldInsideSafeArea() }
        } catch (e: ComposeTimeoutException) {
            val bounds = rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().firstOrNull()?.boundsInWindow
            throw AssertionError(
                "Rotation $expectedRotation: search field $bounds outside the safe range ${safeHorizontalRange()}", e
            )
        }
    }

    @Test
    fun landscapeBothDirections_searchFieldClearOfSystemBarsAndCutout() {
        rule.waitUntilAtLeastOneExists(
            hasContentDescription(rule.activity.getString(R.string.add_note)),
            TIMEOUT_MS
        )
        checkOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Surface.ROTATION_90)
        checkOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, Surface.ROTATION_270)
    }

    @After
    fun restoreOrientation() {
        setOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
