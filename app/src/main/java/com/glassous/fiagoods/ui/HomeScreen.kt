package com.glassous.fiagoods.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.glassous.fiagoods.data.model.CargoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    items: List<CargoItem>,
    loading: Boolean,
    onItemClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("FiaGoods") })
        Box(modifier = Modifier.fillMaxSize()) {
            if (loading && items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        Card(elevation = CardDefaults.cardElevation(), modifier = Modifier.fillMaxWidth().clickable { onItemClick(item.id) }) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(item.category, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                val priceText = item.price?.let { "¥" + String.format("%.2f", it) } ?: ""
                                Text(priceText, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(4.dp))
                                val briefText = item.brief
                                Text(briefText, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}