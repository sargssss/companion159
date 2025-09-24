package com.lifelover.companion159

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lifelover.companion159.ui.screen.PostScreen
import com.lifelover.companion159.ui.theme.Companion159Theme
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

data class InventoryItem(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    var quantity: MutableIntState = mutableIntStateOf(1)
)

enum class InventoryType(val title: Int, val icon: Int) {
    SHIPS(R.string.drones, R.drawable.drone),
    AMMUNITION(R.string.ammo, R.drawable.bomb),
    EQUIPMENT(R.string.tool, R.drawable.tool),
    PROVISIONS(R.string.food, R.drawable.food)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Companion159Theme {
                InventoryApp()
            }
        }
    }
}

@Composable
fun InventoryApp() {
    var currentScreen by remember { mutableStateOf<InventoryType?>(null) }

    // State for each inventory type
    val inventoryStates = remember {
        InventoryType.entries.associateWith {
            mutableStateListOf<InventoryItem>()
        }
    }

    if (currentScreen == null) {
        MainMenuScreen(
            onInventoryTypeSelected = { inventoryType ->
                currentScreen = inventoryType
            }
        )
    } else {
        InventoryScreen(
            inventoryType = currentScreen!!,
            items = inventoryStates[currentScreen!!]!!,
            onBackPressed = {
                currentScreen = null
            }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onInventoryTypeSelected: (InventoryType) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.in_stock),
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InventoryType.values().forEach { inventoryType ->
                InventoryMenuButton(
                    inventoryType = inventoryType,
                    onClick = { onInventoryTypeSelected(inventoryType) }
                )
            }
        }
    }
}

@Composable
fun InventoryMenuButton(
    inventoryType: InventoryType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(inventoryType.icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(inventoryType.title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(id = R.drawable.arrow_right),
                contentDescription = stringResource(id = R.string.go_to),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventoryType: InventoryType,
    items: MutableList<InventoryItem>,
    onBackPressed: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar with back button
        TopAppBar(
            title = {
                Text(
                    text = stringResource(inventoryType.title),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_left),
                        contentDescription = stringResource(id = R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.plus_circle),
                        contentDescription = stringResource(
                            id = R.string.add,
                            stringResource(inventoryType.title).lowercase()
                        )
                    )
                }
            }
        )

        // Items list
        if (items.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(inventoryType.icon),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = R.string.empty_list),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = R.string.push_plus_to_add),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    InventoryItemCard(
                        item = item,
                        onDelete = {
                            items.remove(item)
                        }
                    )
                }
            }
        }
    }

    // Add item dialog
    if (showDialog) {
        AddItemDialog(
            inventoryType = inventoryType,
            onDismiss = { showDialog = false },
            onAdd = { name ->
                items.add(InventoryItem(name = name, quantity = mutableIntStateOf(1)))
                showDialog = false
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Name row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(id = R.drawable.trash),
                        contentDescription = stringResource(id = R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (item.quantity.value > 0) {
                            item.quantity.value = item.quantity.value - 1
                        }
                    },
                    enabled = item.quantity.value > 0
                ) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(R.drawable.minus_circle),
                        contentDescription = stringResource(id = R.string.decrease_quantity)
                    )
                }

                OutlinedTextField(
                    value = item.quantity.value.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { newQuantity ->
                            if (newQuantity >= 0) {
                                item.quantity.value = newQuantity
                            }
                        }
                    },
                    modifier = Modifier.width(80.dp).height(48.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                IconButton(
                    onClick = {
                        item.quantity.value = item.quantity.value + 1
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(R.drawable.plus_circle),
                        contentDescription = stringResource(id = R.string.increase_quantity)
                    )
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(
    inventoryType: InventoryType,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(inventoryType.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(
                            id = R.string.add
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(stringResource(id = R.string.naming)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(id = R.string.enter_title)) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (itemName.isNotBlank()) {
                                onAdd(itemName.trim())
                            }
                        },
                        enabled = itemName.isNotBlank()
                    ) {
                        Text(stringResource(id = R.string.add))
                    }
                }
            }
        }
    }
}