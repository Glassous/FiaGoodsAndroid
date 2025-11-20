package com.glassous.fiagoods.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.glassous.fiagoods.data.model.CargoItem

@Composable
fun DetailScreen(item: CargoItem) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(item.name, style = MaterialTheme.typography.headlineSmall)
        Text(item.category, style = MaterialTheme.typography.titleSmall)
        val priceText = item.price?.let { "¥" + String.format("%.2f", it) } ?: ""
        Text(priceText, style = MaterialTheme.typography.titleMedium)
        Text("库存: ${item.stock ?: 0}", style = MaterialTheme.typography.bodyMedium)
        Text("已售: ${item.sold}", style = MaterialTheme.typography.bodyMedium)
        Text(item.brief, style = MaterialTheme.typography.bodyMedium)
        Text(item.description, style = MaterialTheme.typography.bodyMedium)
        Text(item.specs, style = MaterialTheme.typography.bodyMedium)
        if (item.imageUrls.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(item.imageUrls) { url ->
                    Card(modifier = Modifier.size(120.dp)) {
                        AsyncImage(model = url, contentDescription = null)
                    }
                }
            }
        }
    }
}