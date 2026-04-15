package com.example.osiskitchen.ui

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.delay

@Composable
fun KitchenOrdersScreen(
    viewModel: KitchenOrdersViewModel,
    onGoPlatos: () -> Unit,
    onGoIngredientes: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    data class NoteDialogData(val title: String, val note: String)
    var noteDialog by remember { mutableStateOf<NoteDialogData?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refresh()
            delay(30_000)
        }
    }

    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (!error.isNullOrBlank()) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    KitchenChrome(
        selectedTab = KitchenTab.Comandas,
        onSelectTab = {
            when (it) {
                KitchenTab.Comandas -> Unit
                KitchenTab.Platos -> onGoPlatos()
                KitchenTab.Ingredientes -> onGoIngredientes()
            }
        },
        onLogoClick = { viewModel.refresh() },
        actionIcon = Icons.Filled.Refresh,
        actionIconContentDescription = "Eguneratu",
        onAction = { viewModel.refresh() }
    ) { modifier ->
        Surface(modifier = modifier.fillMaxSize()) {
            noteDialog?.let { dialog ->
                AlertDialog(
                    onDismissRequest = { noteDialog = null },
                    title = { Text(text = dialog.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    text = {
                        Text(
                            text = dialog.note,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState())
                        )
                    },
                    confirmButton = { TextButton(onClick = { noteDialog = null }) { Text(text = "Itxi") } }
                )
            }
            when {
                uiState.isLoading && uiState.groups.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.groups.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ez daude komandak txanda honetarako",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val columnCount = if (isLandscape) 3 else 2

                    val sortedGroups =
                        remember(uiState.groups) {
                            uiState.groups.sortedWith(
                                compareBy<KitchenOrderGroup> { group ->
                                    val done = group.komandak.all { it.egoera } || isEgoeraDone(group.eskariaEgoera)
                                    if (done) 1 else 0
                                }.thenBy { it.eskariaId ?: it.erreserbaId }
                            )
                        }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = 12.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedGroups.size) { index ->
                            val group = sortedGroups[index]
                            val reservaColor = Color(0xFF5B1C1C)
                            val groupDone = group.komandak.all { it.egoera } || isEgoeraDone(group.eskariaEgoera)
                            val groupAlpha = if (groupDone) 0.55f else 1f
                            val cardContainer =
                                if (groupDone) {
                                    lerp(reservaColor, Color.White, 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = cardContainer
                                    )
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).alpha(groupAlpha)) {
                                    Surface(
                                        color = reservaColor,
                                        contentColor = Color.White,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                            Text(
                                                text = buildGroupTitle(group),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val line =
                                                    listOfNotNull(
                                                        group.customerName?.takeIf { it.isNotBlank() },
                                                        group.txanda?.takeIf { it.isNotBlank() }
                                                    ).joinToString(" · ")
                                                if (line.isNotBlank()) {
                                                    Text(
                                                        text = line,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                                Text(
                                                    text = egoeraLabel(group.eskariaEgoera, groupDone),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                val people = group.personCount
                                                if (people != null) {
                                                    Text(
                                                        text = "$people",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val komandas = group.komandak.sortedBy { it.id }
                                    komandas.forEach { komanda ->
                                        val isUpdating = uiState.updatingKomandaIds.contains(komanda.id)
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = komanda.plateraIzena,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                val notes = komanda.oharrak
                                                val hasNotes =
                                                    !notes.isNullOrBlank() && !notes.equals("null", ignoreCase = true)
                                                if (hasNotes) {
                                                    IconButton(
                                                        onClick = {
                                                            noteDialog =
                                                                NoteDialogData(
                                                                    title = komanda.plateraIzena,
                                                                    note = notes
                                                                )
                                                        },
                                                        modifier = Modifier.width(32.dp).height(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.ErrorOutline,
                                                            contentDescription = "Oharra ikusi",
                                                            tint = Color(0xFF5B1C1C)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "x${komanda.kopurua}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            val baseButtonColor = Color(0xFF5B1C1C)
                                            val doneBackground = lerp(baseButtonColor, Color.White, 0.65f)
                                            val buttonContainerColor =
                                                if (komanda.egoera) doneBackground else baseButtonColor
                                            val buttonContentColor =
                                                if (komanda.egoera) Color.Black else Color.White
                                            FilledTonalButton(
                                                onClick = { viewModel.setKomandaEgoera(komanda.id, !komanda.egoera) },
                                                enabled = !isUpdating && !isEgoeraDone(group.eskariaEgoera),
                                                modifier = Modifier.fillMaxWidth().height(58.dp),
                                                colors =
                                                    ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = buttonContainerColor,
                                                        contentColor = buttonContentColor,
                                                        disabledContainerColor = buttonContainerColor.copy(alpha = 0.5f),
                                                        disabledContentColor = buttonContentColor.copy(alpha = 0.65f)
                                                    )
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    if (isUpdating) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(22.dp),
                                                            strokeWidth = 3.dp,
                                                            color = buttonContentColor
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                    }
                                                    Text(
                                                        text = if (komanda.egoera) "Atzera" else "Egina",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isEgoeraDone(egoera: String?): Boolean {
    val e = egoera?.trim().orEmpty().lowercase()
    if (e.isBlank()) return false
    return e.contains("prest") || e.contains("entrega")
}

private fun egoeraLabel(egoera: String?, groupDone: Boolean): String {
    val e = egoera?.trim().orEmpty().lowercase()
    return when {
        e.contains("entrega") -> "Entregatua"
        e.contains("prest") -> "Prest"
        groupDone -> "Prest"
        else -> "Bidalita"
    }
}

private fun buildGroupTitle(group: KitchenOrderGroup): String {
    val mesa = formatMahaiaLabel(group.tablesLabel) ?: "Erreserba ${group.erreserbaId}"
    val id = group.eskariaId ?: group.erreserbaId
    return "$mesa · #$id Eskaera"
}

private fun formatMahaiaLabel(raw: String?): String? {
    val s = raw?.trim().orEmpty()
    if (s.isBlank()) return null
    val m = Regex("""\bMahai\s*(\d+)\b""", RegexOption.IGNORE_CASE).find(s) ?: return s
    val num = m.groupValues.getOrNull(1)?.toIntOrNull() ?: return s
    return "$num. Mahaia"
}
