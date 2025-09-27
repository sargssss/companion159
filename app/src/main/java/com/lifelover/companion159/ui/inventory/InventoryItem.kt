package com.lifelover.companion159.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.data.InventoryItem
import com.lifelover.companion159.R

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onQuantityChange: (Int) -> Unit,
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
                            val newQuantity = item.quantity.value - 1
                            item.quantity.value = newQuantity
                            onQuantityChange(newQuantity)
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
                                onQuantityChange(newQuantity)
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
                        val newQuantity = item.quantity.value + 1
                        item.quantity.value = newQuantity
                        onQuantityChange(newQuantity)
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