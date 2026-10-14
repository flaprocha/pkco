package com.example.pkco.ui.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Shown while a screen's first database load is running. */
@Composable
internal fun CenteredProgress(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().testTag(LOADING_TEST_TAG), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

internal const val LOADING_TEST_TAG = "loading"
