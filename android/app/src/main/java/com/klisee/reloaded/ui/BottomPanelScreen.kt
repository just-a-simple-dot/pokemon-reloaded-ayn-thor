package com.klisee.reloaded.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klisee.reloaded.PanelButton
import com.klisee.reloaded.PanelConfig
import com.klisee.reloaded.PanelSection
import com.klisee.reloaded.Preferences
import com.klisee.reloaded.keyHint
import com.klisee.reloaded.keyLabel

/** Which button a dialog is editing: an existing one, or `index == null` for a new one. */
private data class ButtonTarget(val section: Int, val index: Int?)

/**
 * The editor of the second screen's panels (one outside battle, one in battle): the name and key of
 * every button, their order, and the title and columns of each section. Changes are saved at once and
 * show the next time the game opens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomPanelScreen(prefs: Preferences, onBack: () -> Unit) {
    var battle by remember { mutableStateOf(false) }
    var config by remember(battle) { mutableStateOf(prefs.bottomPanel(battle)) }
    var editingButton by remember(battle) { mutableStateOf<ButtonTarget?>(null) }
    var editingSection by remember(battle) { mutableStateOf<Int?>(null) }

    fun store(newConfig: PanelConfig) {
        config = newConfig
        prefs.setBottomPanel(battle, newConfig)
    }

    fun updateSection(index: Int, change: (PanelSection) -> PanelSection) =
        store(config.copy(sections = config.sections.mapIndexed { i, s -> if (i == index) change(s) else s }))

    editingButton?.let { target ->
        val section = config.sections[target.section]
        ButtonDialog(
            initial = target.index?.let { section.buttons[it] },
            onSave = { button ->
                updateSection(target.section) { s ->
                    val buttons = if (target.index == null) s.buttons + button else s.buttons.toMutableList().also { it[target.index] = button }
                    s.copy(buttons = buttons)
                }
                editingButton = null
            },
            onDelete = target.index?.let { index ->
                {
                    updateSection(target.section) { s -> s.copy(buttons = s.buttons.filterIndexed { i, _ -> i != index }) }
                    editingButton = null
                }
            },
            onDismiss = { editingButton = null },
        )
    }

    editingSection?.let { index ->
        SectionDialog(
            initial = config.sections[index],
            onSave = { title, columns ->
                updateSection(index) { it.copy(title = title, columns = columns) }
                editingSection = null
            },
            onDismiss = { editingSection = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pantalla inferior") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsGroup("Cómo funciona", modifier = Modifier.padding(horizontal = 16.dp)) {
                InfoRow(
                    icon = Icons.Filled.Info,
                    title = "Botones de la segunda pantalla",
                    value = "Toca un botón para cambiar su nombre o su tecla. Los cambios se ven al volver a abrir el juego.",
                    items = 1,
                    index = 0,
                )
            }

            SettingsGroup("Panel", modifier = Modifier.padding(horizontal = 16.dp)) {
                ChoiceRow(
                    icon = Icons.Filled.Explore,
                    title = "Fuera de combate",
                    subtitle = "El que se ve mientras exploras",
                    selected = !battle,
                    items = 2,
                    index = 0,
                    onSelect = { battle = false },
                )
                ChoiceRow(
                    icon = Icons.Filled.SportsMma,
                    title = "En combate",
                    subtitle = "Cambia solo al empezar un combate y vuelve al terminar",
                    selected = battle,
                    items = 2,
                    index = 1,
                    onSelect = { battle = true },
                )
            }

            config.sections.forEachIndexed { sectionIndex, section ->
                val rows = section.buttons.size + 2
                SettingsGroup(section.title.ifBlank { "Sección ${sectionIndex + 1}" }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    NavRow(
                        icon = Icons.Filled.Edit,
                        title = "Título y columnas",
                        subtitle = "${section.columns} columnas",
                        items = rows,
                        index = 0,
                        onClick = { editingSection = sectionIndex },
                    )
                    section.buttons.forEachIndexed { index, button ->
                        GroupListItem(
                            headlineContent = { Text(button.label.ifBlank { "(sin nombre)" }, style = MaterialTheme.typography.titleMedium) },
                            supportingContent = { Text("Tecla ${keyLabel(button.key)}", style = MaterialTheme.typography.bodyMedium) },
                            leadingContent = { Icon(Icons.Filled.TouchApp, contentDescription = null) },
                            trailingContent = {
                                Row {
                                    IconButton(enabled = index > 0, onClick = {
                                        updateSection(sectionIndex) { s -> s.copy(buttons = s.buttons.swapped(index, index - 1)) }
                                    }) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir") }
                                    IconButton(enabled = index < section.buttons.lastIndex, onClick = {
                                        updateSection(sectionIndex) { s -> s.copy(buttons = s.buttons.swapped(index, index + 1)) }
                                    }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar") }
                                }
                            },
                            items = rows,
                            index = index + 1,
                            onClick = { editingButton = ButtonTarget(sectionIndex, index) },
                        )
                    }
                    NavRow(
                        icon = Icons.Filled.Add,
                        title = "Añadir botón",
                        items = rows,
                        index = rows - 1,
                        onClick = { editingButton = ButtonTarget(sectionIndex, null) },
                    )
                }
            }

            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { store(PanelConfig.default(battle)) }) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (battle) "Restaurar el panel de combate" else "Restaurar el panel de exploración")
                }
            }
        }
    }
}

private fun <T> List<T>.swapped(a: Int, b: Int): List<T> = toMutableList().also {
    val tmp = it[a]
    it[a] = it[b]
    it[b] = tmp
}

/** Edits (or creates) one button: the name shown on it and the key it presses. */
@Composable
private fun ButtonDialog(
    initial: PanelButton?,
    onSave: (PanelButton) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var key by remember { mutableStateOf(initial?.key) }
    var picking by remember { mutableStateOf(false) }

    if (picking) {
        KeyPickerDialog(
            title = "Tecla del botón",
            current = key,
            allowNone = false,
            onPick = { picked ->
                key = picked
                // A new button with no name yet takes what the key does in the game, if known.
                if (label.isBlank()) label = keyHint(picked)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo botón" else "Editar botón") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(key?.let { "Tecla: ${keyLabel(it)}" } ?: "Elegir tecla")
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Borrar este botón", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = key != null, onClick = { key?.let { onSave(PanelButton(it, label.trim())) } }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Edits a section's title and how many buttons fit in each of its rows. */
@Composable
private fun SectionDialog(initial: PanelSection, onSave: (String, Int) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var columns by remember { mutableStateOf(initial.columns) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sección") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Columnas: $columns")
                Slider(
                    value = columns.toFloat(),
                    onValueChange = { columns = kotlin.math.round(it).toInt() },
                    valueRange = PanelConfig.MIN_COLUMNS.toFloat()..PanelConfig.MAX_COLUMNS.toFloat(),
                    steps = PanelConfig.MAX_COLUMNS - PanelConfig.MIN_COLUMNS - 1,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title.trim(), columns) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
