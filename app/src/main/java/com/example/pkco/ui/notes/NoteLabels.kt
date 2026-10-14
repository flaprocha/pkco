package com.example.pkco.ui.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.pkco.R
import com.example.pkco.data.entity.Tag

/** Tag names sorted ignoring case, so they show in the same order everywhere. */
internal fun sortedTagNames(tags: List<Tag>): List<String> =
    tags.map { it.name }.sortedWith(String.CASE_INSENSITIVE_ORDER)

/** The tags as "#kotlin #study", or an empty string when there are none. */
@Composable
internal fun tagsText(tags: List<Tag>): String =
    sortedTagNames(tags).map { stringResource(R.string.tag_label, it) }.joinToString(" ")
