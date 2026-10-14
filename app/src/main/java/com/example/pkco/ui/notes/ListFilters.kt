package com.example.pkco.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pkco.R
import com.example.pkco.data.entity.Category
import com.example.pkco.data.entity.Tag

/** The list's two filters, "Category: …" and "Tag: …"; each menu starts with "All" (a null id). */
@Composable
fun ListFilters(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelectCategory: (categoryId: String?) -> Unit,
    tags: List<Tag>,
    selectedTagId: String?,
    onSelectTag: (tagId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val all = stringResource(R.string.filter_all)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterDropdown(
            label = stringResource(
                R.string.filter_category,
                categories.firstOrNull { it.id == selectedCategoryId }?.name ?: all
            ),
            options = categories.map { it.id to it.name },
            onSelect = onSelectCategory,
            modifier = Modifier.weight(1f)
        )
        FilterDropdown(
            label = stringResource(R.string.filter_tag, tags.firstOrNull { it.id == selectedTagId }?.name ?: all),
            options = tags.map { it.id to it.name },
            onSelect = onSelectTag,
            modifier = Modifier.weight(1f)
        )
    }
}

/** A button showing [label] that opens a menu of "All" followed by [options] (id to name). */
@Composable
private fun FilterDropdown(
    label: String,
    options: List<Pair<String, String>>,
    onSelect: (id: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_all)) },
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        expanded = false
                        onSelect(id)
                    }
                )
            }
        }
    }
}
