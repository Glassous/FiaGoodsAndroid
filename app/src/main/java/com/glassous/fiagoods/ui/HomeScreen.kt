package com.glassous.fiagoods.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.glassous.fiagoods.data.model.CargoItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    items: List<CargoItem>,
    loading: Boolean,
    onItemClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        var query by remember { mutableStateOf("") }
        var searchOpen by remember { mutableStateOf(false) }
        TopAppBar(
            title = { Text("FiaGoods") },
            actions = {
                AnimatedVisibility(
                    visible = searchOpen,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        placeholder = { Text("搜索") },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { query = ""; searchOpen = false }) { Icon(Icons.Filled.Close, contentDescription = null) }
                        },
                        modifier = Modifier.widthIn(min = 120.dp, max = 240.dp)
                    )
                }
                IconButton(onClick = { searchOpen = !searchOpen }) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                }
            }
        )
        Box(modifier = Modifier.fillMaxSize()) {
            if (loading && items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LoadingIndicator(color = LoadingIndicatorDefaults.indicatorColor)
                            Text("正在加载…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                val list = if (query.isBlank()) items else items.filter { it.name.contains(query, true) || it.category.contains(query, true) }
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list) { item ->
                        Card(elevation = CardDefaults.cardElevation(), modifier = Modifier.fillMaxWidth().clickable { onItemClick(item.id) }) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(item.category, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(6.dp))
                                    val priceText = item.price?.let { "¥" + String.format("%.2f", it) } ?: "¥0.00"
                                    Text("售价: $priceText", style = MaterialTheme.typography.bodyLarge)
                                    Spacer(Modifier.height(4.dp))
                                    Text("销量: ${item.sold}", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(2.dp))
                                    Text("库存: ${item.stock ?: 0}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Box(modifier = Modifier
                                    .fillMaxHeight(0.9f)
                                    .heightIn(max = 96.dp)
                                    .width(84.dp)
                                    .clip(RoundedCornerShape(8.dp))) {
                                    val previewUrl = item.imageUrls.firstOrNull()
                                    AsyncImage(model = previewUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}