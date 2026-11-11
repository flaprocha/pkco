package com.example.pkco.ui.notes

import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pkco.MainActivity
import com.example.pkco.R
import com.example.pkco.data.PkcoDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real app on the device: create a note (rotating in the middle of the form), open it, edit its title,
 * delete it. Names carry a timestamp, so notes already on the phone do not matter; the rows are removed afterwards.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class NoteFlowUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val stamp = System.currentTimeMillis()
    private val title = "UI test $stamp"
    private val editedTitle = "UI test edited $stamp"
    private val body = "Body $stamp"
    private val tag = "uitest$stamp"
    private val pendingTag = "pending$stamp"

    private fun str(@StringRes id: Int, vararg args: Any): String = rule.activity.getString(id, *args)

    private fun waitForText(text: String) = rule.waitUntilAtLeastOneExists(hasText(text), TIMEOUT_MS)

    private fun waitForNoText(text: String) = rule.waitUntilDoesNotExist(hasText(text), TIMEOUT_MS)

    /** The list is shown once the "+" button (content description "Add note") exists. */
    private fun waitForList() =
        rule.waitUntilAtLeastOneExists(hasContentDescription(str(R.string.add_note)), TIMEOUT_MS)

    @Test
    fun createRotateEditAndDeleteNote() {
        waitForList()
        rule.waitUntilDoesNotExist(hasTestTag(LOADING_TEST_TAG), TIMEOUT_MS)
        rule.onNodeWithContentDescription(str(R.string.add_note)).performClick()

        rule.onNodeWithTag(FormTestTags.TITLE).performTextInput(title)
        rule.onNodeWithTag(FormTestTags.BODY).performTextInput(body)
        rule.onNodeWithTag(FormTestTags.TAG).performTextInput(tag)
        rule.onNodeWithText(str(R.string.add)).performClick()
        rule.onNodeWithTag(FormTestTags.TAG).performTextInput(pendingTag)

        // Rotation: the activity is recreated; the typed text, the chip and the tag field must survive.
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag(FormTestTags.TITLE).assertTextContains(title)
        rule.onNodeWithTag(FormTestTags.BODY).assertTextContains(body)
        rule.onNodeWithTag(FormTestTags.TAG).assertTextContains(pendingTag)
        rule.onNodeWithText(str(R.string.tag_chip, tag)).assertExists()
        rule.onNodeWithText(str(R.string.char_count, title.length, InputLimits.TITLE)).assertExists()

        rule.onNodeWithTag(FormTestTags.TAG).performTextClearance()
        rule.onNodeWithText(str(R.string.save)).performClick()

        // Back on the list with the new note (the form's title field also holds the text, so wait for the list first).
        waitForList()
        waitForText(title)
        rule.onNodeWithText(title).performClick()

        // View, then Edit the title.
        waitForText(str(R.string.note_tags, str(R.string.tag_label, tag)))
        rule.onNodeWithText(str(R.string.edit)).performClick()
        rule.onNodeWithTag(FormTestTags.TITLE).performTextClearance()
        rule.onNodeWithTag(FormTestTags.TITLE).performTextInput(editedTitle)
        rule.onNodeWithText(str(R.string.save)).performClick()
        // The View screen is back once its "Edit" button exists (the form's heading is "Edit note").
        waitForText(str(R.string.edit))
        rule.onNodeWithText(editedTitle).assertExists()

        // Delete with the confirmation dialog.
        rule.onNodeWithText(str(R.string.delete)).performClick()
        rule.onNode(hasText(str(R.string.delete)) and hasAnyAncestor(isDialog())).performClick()
        waitForList()
        waitForNoText(editedTitle)
        waitForNoText(title)

        // Delete is permanent: no row with either title is left in the database.
        val db = PkcoDatabase.getInstance(rule.activity.applicationContext).openHelper.readableDatabase
        val count = db.query("SELECT COUNT(*) FROM notes WHERE title IN (?, ?)", arrayOf<Any>(title, editedTitle))
            .use { it.moveToFirst(); it.getInt(0) }
        assertEquals(0, count)
    }

    /** Removes this test's rows if the test failed before deleting the note, and its tags (unused tags are kept). */
    @After
    fun removeTestRows() {
        val db = PkcoDatabase.getInstance(rule.activity.applicationContext).openHelper.writableDatabase
        val titles = arrayOf<Any>(title, editedTitle)
        db.execSQL("DELETE FROM note_tags WHERE noteId IN (SELECT id FROM notes WHERE title IN (?, ?))", titles)
        db.execSQL("DELETE FROM notes WHERE title IN (?, ?)", titles)
        db.execSQL("DELETE FROM tags WHERE name IN (?, ?)", arrayOf<Any>(tag, pendingTag))
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
