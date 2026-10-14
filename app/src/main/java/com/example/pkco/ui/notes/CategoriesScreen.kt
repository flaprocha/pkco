package com.example.pkco.ui.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pkco.R
import com.example.pkco.data.entity.Category
import kotlinx.coroutines.launch

/**
 * Creates, renames and deletes categories. [categories] is null while the first load is running. Rejected names
 * (blank, or already used ignoring case) show a message under the field or inside the rename dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    categories: List<Category>?,
    onBack: () -> Unit,
    onCreate: suspend (name: String) -> Unit,
    onRename: suspend (id: String, name: String) -> Unit,
    onDelete: (id: String) -> Unit,
) {
    var newName by rememberSaveable { mutableStateOf("") }
    var addError by rememberSaveable { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }
    // Ids, so the dialogs survive rotation; a dialog closes if its category disappears.
    var renamingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val add = {
        if (!adding) {
            adding = true
            scope.launch {
                val rejected = saveCategoryName(newName, onCreate)
                adding = false
                if (rejected == null) newName = "" else addError = rejected
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        addError = null
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.new_category)) },
                    singleLine = true,
                    isError = addError != null,
                    supportingText = addError?.let { { Text(stringResource(it)) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { add() })
                )
                TextButton(
                    onClick = { add() },
                    enabled = !adding,
                    modifier = Modifier.padding(top = 4.dp)
                ) { Text(stringResource(R.string.add)) }
            }
            when {
                categories == null -> Unit
                categories.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.empty_categories))
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(categories, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            onRename = { renamingId = category.id },
                            onDelete = { deletingId = category.id }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    categories?.firstOrNull { it.id == renamingId }?.let { category ->
        // Keyed by id, so the field is prefilled with this category's name.
        key(category.id) {
            CategoryNameDialog(
                title = stringResource(R.string.rename_category),
                confirmLabel = stringResource(R.string.save),
                onConfirm = { name -> onRename(category.id, name) },
                onDone = { renamingId = null },
                initialName = category.name
            )
        }
    }

    categories?.firstOrNull { it.id == deletingId }?.let { category ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = { Text(stringResource(R.string.delete_category_message)) },
            confirmButton = {
                TextButton(onClick = {
                    deletingId = null
                    onDelete(category.id)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun CategoryRow(category: Category, onRename: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            category.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onRename) { Text(stringResource(R.string.rename)) }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
    }
}
