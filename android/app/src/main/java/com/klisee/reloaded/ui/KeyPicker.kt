package com.klisee.reloaded.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klisee.reloaded.GAME_KEYS

/**
 * Picks any key of the keyboard the game can be sent, grouped as on a keyboard. With `allowNone`,
 * "Sin asignar" is offered too and picking it reports null.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyPickerDialog(
    title: String,
    current: String?,
    allowNone: Boolean,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (allowNone) {
                    FilterChip(selected = current == null, onClick = { onPick(null) }, label = { Text("Sin asignar") })
                }
                for ((group, keys) in GAME_KEYS.groupBy { it.group }) {
                    Text(group, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        for (key in keys) {
                            FilterChip(selected = key.id == current, onClick = { onPick(key.id) }, label = { Text(key.label) })
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
