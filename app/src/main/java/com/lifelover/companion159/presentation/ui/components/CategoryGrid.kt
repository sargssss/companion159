package com.lifelover.companion159.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.toStorageCategory

/**
 * Reusable category selection grid
 *
 * Displays all display categories as clickable cards
 * Can be customized with different layouts
 */
@Composable
fun CategoryGrid(
    modifier: Modifier = Modifier,
    categories: List<DisplayCategory> = DisplayCategory.entries,
    onCategorySelected: (DisplayCategory) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        categories.forEach { category ->
            CategoryCard(
                category = category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}