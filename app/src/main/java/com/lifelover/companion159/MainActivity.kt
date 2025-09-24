package com.lifelover.companion159

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import com.lifelover.companion159.ui.screen.MainScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource

data class InventoryItem(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    var quantity: MutableIntState = mutableIntStateOf(1)
)

enum class InventoryType(val title: String, val icon: Int) {
    SHIPS("Борти", R.drawable.drone),//Icons.Default.DirectionsBoat
    AMMUNITION("Боєкомплект", R.drawable.bomb),//Icons.Default.Inventory2
    EQUIPMENT("Обладнання", R.drawable.tool),//Icons.Default.Build
    PROVISIONS("Провізія", R.drawable.food)//Icons.Default.Restaurant
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Companion159Theme {
                //MyApp()
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
                    text = "Інвентарізація",
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
            Text(
                text = "Оберіть категорію:",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

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
                text = inventoryType.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Перейти",
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
                    text = inventoryType.title,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            },
            actions = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Додати ${inventoryType.title.lowercase()}"
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
                        painter = painterResource(inventoryType.icon),                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Список порожній",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Натисніть + щоб додати перший елемент",
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
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Видалити",
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
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Зменшити кількість"
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
                    modifier = Modifier.width(80.dp),
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
                        imageVector = Icons.Default.Add,
                        contentDescription = "Збільшити кількість"
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
                        text = "Додати ${inventoryType.title.lowercase()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Найменування") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Введіть назву...") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Скасувати")
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
                        Text("Додати")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyToolbar() {
    TopAppBar(
        title = { Text("Toolbar") },
        navigationIcon = { /* ... */ },
        actions = { /* ... */ }
    )
}

@Composable
fun PostScreenView(modifier: Modifier) {
    PostScreen(modifier = modifier)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Companion159Theme {
        Greeting("Android")
    }
}