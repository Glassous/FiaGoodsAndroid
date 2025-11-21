package com.glassous.fiagoods.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.glassous.fiagoods.data.model.CargoItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.blur
import java.util.UUID
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.PaddingValues
import com.glassous.fiagoods.ui.components.AppDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import com.glassous.fiagoods.SettingsActivity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    items: List<CargoItem>,
    loading: Boolean,
    onItemClick: (CargoItem) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onAddItemWithImage: (CargoItem, Uri?) -> Unit,
    columnsPerRow: Int,
    onRefresh: () -> Unit
) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var query by remember { mutableStateOf("") }
    var filterOpen by remember { mutableStateOf(false) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var addOpen by remember { mutableStateOf(false) }
    Column(modifier = if (addOpen || filterOpen) Modifier.fillMaxSize().blur(12.dp) else Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("FiaGoods") },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                }
                IconButton(onClick = { favoritesOnly = !favoritesOnly }) {
                    val tint = if (favoritesOnly) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface
                    Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = tint)
                }
                IconButton(onClick = { filterOpen = true }) {
                    Icon(Icons.Filled.FilterList, contentDescription = null)
                }
                IconButton(onClick = { addOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
                val context = LocalContext.current
                IconButton(onClick = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
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
                val base = if (query.isBlank()) items else items.filter { it.description.contains(query, true) || it.category.contains(query, true) }
                val list = base.filter {
                    (selectedGroups.isEmpty() || selectedGroups.contains(it.groupName)) &&
                    (selectedCategories.isEmpty() || selectedCategories.contains(it.category)) &&
                    (!favoritesOnly || favorites.contains(it.id))
                }
                val ctx = LocalContext.current
                val clipboard = LocalClipboardManager.current
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        placeholder = { Text("搜索") },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(columnsPerRow.coerceAtLeast(1)),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp + bottom),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp
                    ) {
                        items(list) { item ->
                            Card(elevation = CardDefaults.cardElevation(), modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onItemClick(item) }, onLongClick = {
                                clipboard.setText(AnnotatedString(item.link))
                                Toast.makeText(ctx, "链接已复制", Toast.LENGTH_SHORT).show()
                            })) {
                                Column {
                                    val previewUrl = item.imageUrls.firstOrNull()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    ) {
                                        AsyncImage(model = previewUrl, contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                                        val tint = if (favorites.contains(item.id)) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, tint, CircleShape)
                                                .clickable { onToggleFavorite(item.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        val title = if (item.description.length <= 7) item.description else item.description.take(7)
                                        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (addOpen) {
            var description by remember { mutableStateOf("") }
            var category by remember { mutableStateOf("") }
            var groupName by remember { mutableStateOf("") }
            var link by remember { mutableStateOf("") }
            var priceText by remember { mutableStateOf("") }
            var imageUri by remember { mutableStateOf<Uri?>(null) }
            val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }
            val canSave = true
            AppDialog(onDismiss = { addOpen = false }, title = "新增商品", content = {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("类别") }, singleLine = true)
                OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("分组") }, singleLine = true)
                OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("链接") }, singleLine = true)
                OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("售价") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { pickImage.launch("image/*") }) { Text("选择图片") }
                    if (imageUri != null) {
                        Spacer(Modifier.width(8.dp))
                        Text("已选择", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }, actions = {
                androidx.compose.material3.TextButton(onClick = { addOpen = false }) { Text("取消") }
                androidx.compose.material3.TextButton(onClick = {
                    val id = UUID.randomUUID().toString()
                    val priceVal = priceText.toDoubleOrNull() ?: 0.0
                    val item = CargoItem(
                        id = id,
                        description = description,
                        imageUrls = emptyList(),
                        groupName = groupName,
                        category = category,
                        price = priceVal,
                        link = link
                    )
                    onAddItemWithImage(item, imageUri)
                    addOpen = false
                }, enabled = canSave) { Text("保存") }
            })
        }

        if (filterOpen) {
            val groupOptions = remember(items) { items.map { it.groupName }.filter { it.isNotBlank() }.distinct().sorted() }
            val categoryOptions = remember(items) { items.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() }
            var groupExpanded by remember { mutableStateOf(true) }
            var categoryExpanded by remember { mutableStateOf(true) }
            var groupQuery by remember { mutableStateOf("") }
            var categoryQuery by remember { mutableStateOf("") }
            val filteredGroups = remember(groupOptions, groupQuery) { if (groupQuery.isBlank()) groupOptions else groupOptions.filter { it.contains(groupQuery, true) } }
            val filteredCategories = remember(categoryOptions, categoryQuery) { if (categoryQuery.isBlank()) categoryOptions else categoryOptions.filter { it.contains(categoryQuery, true) } }
            AppDialog(onDismiss = { filterOpen = false }, title = "筛选", content = {
                if (groupOptions.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("分组", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { groupExpanded = !groupExpanded }) {
                            Icon(imageVector = if (groupExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                    if (groupExpanded) {
                        OutlinedTextField(value = groupQuery, onValueChange = { groupQuery = it }, label = { Text("搜索分组") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                        Spacer(Modifier.height(6.dp))
                        filteredGroups.forEach { g ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = selectedGroups.contains(g), onCheckedChange = {
                                    selectedGroups = if (selectedGroups.contains(g)) selectedGroups - g else selectedGroups + g
                                })
                                Text(g, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
                if (categoryOptions.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("类别", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { categoryExpanded = !categoryExpanded }) {
                            Icon(imageVector = if (categoryExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                    if (categoryExpanded) {
                        OutlinedTextField(value = categoryQuery, onValueChange = { categoryQuery = it }, label = { Text("搜索类别") }, singleLine = true, shape = RoundedCornerShape(20.dp))
                        Spacer(Modifier.height(6.dp))
                        filteredCategories.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = selectedCategories.contains(c), onCheckedChange = {
                                    selectedCategories = if (selectedCategories.contains(c)) selectedCategories - c else selectedCategories + c
                                })
                                Text(c, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }, actions = {
                androidx.compose.material3.TextButton(onClick = {
                    selectedGroups = emptySet()
                    selectedCategories = emptySet()
                }) { Text("清空") }
                androidx.compose.material3.TextButton(onClick = { filterOpen = false }) { Text("确定") }
            })
        }
    }
}
