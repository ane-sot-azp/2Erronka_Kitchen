package com.example.osiskitchen.ui

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.osiskitchen.R

@Composable
fun KitchenPlatosStockScreen(
        viewModel: KitchenPlatosStockViewModel,
        onGoComandas: () -> Unit,
        onGoIngredientes: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var step by remember { mutableStateOf(1) }
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var stockDialogPlato by remember { mutableStateOf<KitchenPlatoStock?>(null) }
    var stockDialogText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (!error.isNullOrBlank()) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    val categories =
        remember(uiState.platos) {
            uiState.platos
                .mapNotNull { p ->
                    val id = p.kategoriaId ?: return@mapNotNull null
                    val label = p.kategoriaIzena?.trim().takeUnless { it.isNullOrBlank() } ?: "Kategoria $id"
                    id to label
                }
                .distinctBy { it.first }
                .sortedBy { it.first }
        }

    val categoryButtons =
        remember(categories) {
            val primeroId = categories.firstOrNull { motaOrderKey(it.second) == 0 }?.first
            val segundoId = categories.firstOrNull { motaOrderKey(it.second) == 1 }?.first
            val postreId = categories.firstOrNull { motaOrderKey(it.second) == 2 }?.first
            val bebidasId = categories.firstOrNull { motaOrderKey(it.second) == 3 }?.first
            listOf(
                CategoryButtonModel(
                    id = null,
                    label = "Dena",
                    iconResId = null
                ),
                CategoryButtonModel(
                    id = primeroId,
                    label = "",
                    iconResId = R.drawable.primero
                ),
                CategoryButtonModel(
                    id = segundoId,
                    label = "",
                    iconResId = R.drawable.segundo
                ),
                CategoryButtonModel(
                    id = postreId,
                    label = "",
                    iconResId = R.drawable.postre
                ),
                CategoryButtonModel(
                    id = bebidasId,
                    label = "",
                    iconResId = R.drawable.bebidas
                )
            )
        }

    val filtered =
        remember(uiState.platos, query, selectedCategoryId) {
            val q = query.trim().lowercase()
            uiState.platos.filter { p ->
                val matchesQuery =
                    q.isBlank() ||
                        p.izena.lowercase().contains(q) ||
                        (p.kategoriaIzena?.lowercase()?.contains(q) == true)
                val matchesCategory = selectedCategoryId == null || p.kategoriaId == selectedCategoryId
                matchesQuery && matchesCategory
            }
        }

    KitchenChrome(
            selectedTab = KitchenTab.Platos,
            onSelectTab = {
                when (it) {
                    KitchenTab.Comandas -> onGoComandas()
                    KitchenTab.Platos -> Unit
                    KitchenTab.Ingredientes -> onGoIngredientes()
                }
            },
            onLogoClick = { viewModel.refresh() },
            actionIcon = Icons.Filled.Refresh,
            actionIconContentDescription = "Eguneratu",
            onAction = { viewModel.refresh() }
    ) { modifier ->
        Surface(modifier = modifier.fillMaxSize()) {
            Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                stockDialogPlato?.let { plato ->
                    AlertDialog(
                            onDismissRequest = { stockDialogPlato = null },
                            title = { Text(text = plato.izena) },
                            text = {
                                OutlinedTextField(
                                        value = stockDialogText,
                                        onValueChange = { stockDialogText = it },
                                        singleLine = true,
                                        label = { Text(text = "Stock") },
                                        keyboardOptions =
                                                KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                TextButton(
                                        onClick = {
                                            val parsed = stockDialogText.trim().toIntOrNull()
                                            if (parsed != null) {
                                                val target = parsed.coerceAtLeast(0)
                                                val delta = target - plato.stock
                                                if (delta != 0)
                                                        viewModel.adjustStock(plato.id, delta)
                                            }
                                            stockDialogPlato = null
                                        }
                                ) { Text(text = "Gorde") }
                            },
                            dismissButton = {
                                TextButton(onClick = { stockDialogPlato = null }) {
                                    Text(text = "Utzi")
                                }
                            }
                    )
                }

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(text = "Platerak bilatu") }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { step = (step - 1).coerceAtLeast(1) }) {
                            Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Pausoa jaitsi"
                            )
                        }
                        Text(
                                text = step.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { step = (step + 1).coerceAtMost(50) }) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Pausoa igo")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val orange = remember { Color(0xFFF3863A) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categoryButtons.forEach { model ->
                        val selected = selectedCategoryId == model.id
                        val enabled = model.iconResId == null || model.id != null
                        Surface(
                            color = if (selected) orange else MaterialTheme.colorScheme.surface,
                            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(18.dp),
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(enabled = enabled) { selectedCategoryId = model.id }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (model.iconResId != null) {
                                    Icon(
                                        painter = painterResource(model.iconResId),
                                        contentDescription = null,
                                        tint = if (selected) Color.White else orange,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                if (model.label.isNotBlank()) {
                                    Text(
                                        text = model.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.isLoading && uiState.platos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@Surface
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                                text = "Ez dago platerarik",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    return@Surface
                }

                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val columns = if (isLandscape) 3 else 2
                val cardBaseColor = lerp(orange, Color.White, 0.82f)

                LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { plato ->
                        val isUpdating = uiState.updatingIds.contains(plato.id)
                        val cardColor = if (isUpdating) cardBaseColor.copy(alpha = 0.75f) else cardBaseColor
                        val stockColor = if (plato.stock < 5) Color.Red else MaterialTheme.colorScheme.onSurface
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor = cardColor
                                        )
                        ) {
                            Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = plato.izena,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2
                                        )
                                        val cat = plato.kategoriaIzena
                                        if (!cat.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                    text = cat,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                                text = plato.stock.toString(),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = stockColor,
                                                modifier =
                                                        Modifier.clickable(
                                                                enabled = !isUpdating
                                                        ) {
                                                            stockDialogPlato = plato
                                                            stockDialogText = plato.stock.toString()
                                                        }
                                        )
                                        if (isUpdating) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                            enabled = !isUpdating && plato.stock - step >= 0,
                                            onClick = { viewModel.adjustStock(plato.id, -step) }
                                    ) {
                                        Icon(
                                                imageVector = Icons.Filled.Remove,
                                                contentDescription = "Stock-a kendu"
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    IconButton(
                                            enabled = !isUpdating,
                                            onClick = { viewModel.adjustStock(plato.id, step) }
                                    ) {
                                        Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Stock-a gehitu"
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

private data class CategoryButtonModel(
    val id: Int?,
    val label: String,
    val iconResId: Int?
)

private fun motaOrderKey(mota: String): Int {
    val lower = mota.trim().lowercase()
    return when {
        lower.contains("prim") || lower.contains("lehen") -> 0
        lower.contains("segu") || lower.contains("big") -> 1
        lower.contains("post") -> 2
        lower.contains("bebi") || lower.contains("edar") -> 3
        else -> 99
    }
}

@Composable
fun KitchenIngredientesStockScreen(
        viewModel: KitchenIngredientesStockViewModel,
        onGoComandas: () -> Unit,
        onGoPlatos: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var step by remember { mutableStateOf(1) }
    var query by remember { mutableStateOf("") }
    var stockDialogIngrediente by remember { mutableStateOf<KitchenIngredienteStock?>(null) }
    var stockDialogText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (!error.isNullOrBlank()) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    val filtered =
            remember(uiState.ingredientes, query) {
                val q = query.trim().lowercase()
                if (q.isBlank()) uiState.ingredientes
                else uiState.ingredientes.filter { it.izena.lowercase().contains(q) }
            }

    KitchenChrome(
            selectedTab = KitchenTab.Ingredientes,
            onSelectTab = {
                when (it) {
                    KitchenTab.Comandas -> onGoComandas()
                    KitchenTab.Platos -> onGoPlatos()
                    KitchenTab.Ingredientes -> Unit
                }
            },
            onLogoClick = { viewModel.refresh() },
            actionIcon = Icons.Filled.Refresh,
            actionIconContentDescription = "Eguneratu",
            onAction = { viewModel.refresh() }
    ) { modifier ->
        Surface(modifier = modifier.fillMaxSize()) {
            Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val orange = remember { Color(0xFFF3863A) }
                stockDialogIngrediente?.let { ingrediente ->
                    AlertDialog(
                            onDismissRequest = { stockDialogIngrediente = null },
                            title = { Text(text = ingrediente.izena) },
                            text = {
                                OutlinedTextField(
                                        value = stockDialogText,
                                        onValueChange = { stockDialogText = it },
                                        singleLine = true,
                                        label = { Text(text = "Stock") },
                                        keyboardOptions =
                                                KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                TextButton(
                                        onClick = {
                                            val parsed = stockDialogText.trim().toIntOrNull()
                                            if (parsed != null) {
                                                val target = parsed.coerceAtLeast(0)
                                                val delta = target - ingrediente.stock
                                                if (delta != 0)
                                                        viewModel.adjustStock(ingrediente.id, delta)
                                            }
                                            stockDialogIngrediente = null
                                        }
                                ) { Text(text = "Gorde") }
                            },
                            dismissButton = {
                                TextButton(onClick = { stockDialogIngrediente = null }) {
                                    Text(text = "Utzi")
                                }
                            }
                    )
                }

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(text = "Osagaiak bilatu") }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { step = (step - 1).coerceAtLeast(1) }) {
                            Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Pausoa jaitsi"
                            )
                        }
                        Text(
                                text = step.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { step = (step + 1).coerceAtMost(50) }) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Pausoa igo")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.isLoading && uiState.ingredientes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@Surface
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                                text = "Ez dago osagairik",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    return@Surface
                }

                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val columns = if (isLandscape) 3 else 2
                val cardBaseColor = lerp(orange, Color.White, 0.82f)

                LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { ingrediente ->
                        val isUpdating = uiState.updatingIds.contains(ingrediente.id)
                        val cardColor = if (isUpdating) cardBaseColor.copy(alpha = 0.75f) else cardBaseColor
                        val stockColor = if (ingrediente.stock < 5) Color.Red else MaterialTheme.colorScheme.onSurface

                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardColor)
                        ) {
                            Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = ingrediente.izena,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2
                                        )
                                        Text(
                                                text = "Gutx: ${ingrediente.gutxienekoStock}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                                text = ingrediente.stock.toString(),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = stockColor,
                                                modifier =
                                                        Modifier.clickable(
                                                                enabled = !isUpdating
                                                        ) {
                                                            stockDialogIngrediente = ingrediente
                                                            stockDialogText = ingrediente.stock.toString()
                                                        }
                                        )
                                        if (isUpdating) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                }

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                            enabled = !isUpdating && ingrediente.stock - step >= 0,
                                            onClick = { viewModel.adjustStock(ingrediente.id, -step) }
                                    ) {
                                        Icon(
                                                imageVector = Icons.Filled.Remove,
                                                contentDescription = "Stock-a kendu"
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    IconButton(
                                            enabled = !isUpdating,
                                            onClick = { viewModel.adjustStock(ingrediente.id, step) }
                                    ) {
                                        Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Stock-a gehitu"
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
