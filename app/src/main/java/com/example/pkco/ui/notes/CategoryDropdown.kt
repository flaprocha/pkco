package com.example.pkco.ui.notes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.pkco.R
import com.example.pkco.data.entity.Category

/**
 * Picks the note's one optional category: "No category", every category by name, and "New category…", which
 * opens a dialog. A category created there through [onCreateCategory] becomes the selected one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (categoryId: String?) -> Unit,
    onCreateCategory: suspend (name: String) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name ?: stringResource(R.string.no_category)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.no_category)) },
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        expanded = false
                        onSelect(category.id)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_category_option)) },
                onClick = {
                    expanded = false
                    creating = true
                }
            )
        }
    }

    if (creating) {
        CategoryNameDialog(
            title = stringResource(R.string.new_category),
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name -> onSelect(onCreateCategory(name)) },
            onDone = { creating = false }
        )
    }
}
