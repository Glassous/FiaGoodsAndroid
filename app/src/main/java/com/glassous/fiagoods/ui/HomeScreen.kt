package com.glassous.fiagoods.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.material.icons.filled.Close
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
import com.glassous.fiagoods.ui.global.UploadState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.widget.Toast
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.compose.LocalImageLoader
import com.glassous.fiagoods.util.buildOssThumbnailUrl
import com.glassous.fiagoods.data.SessionPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import com.glassous.fiagoods.SettingsActivity
import com.glassous.fiagoods.DetailActivity
import androidx.compose.material3.RangeSlider
import kotlin.math.roundToInt

// 防止 R8 混淆局部数据类，保持在顶层
data class LinkThumb(val link: String, val preview: String?)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    items: List<CargoItem>,
    loading: Boolean,
    onItemClick: (CargoItem) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onCreateItemWithImagesAndUrls: (CargoItem, List<Uri>, List<String>, (Int, Int) -> Unit, (Boolean) -> Unit) -> Unit,
    onAddImageUrlsDirect: (String, List<String>, (Boolean) -> Unit) -> Unit,
    columnsPerRow: Int,
    titleMaxLen: Int, // 【新增参数】：从外部接收配置，确保实时生效
    onRefresh: () -> Unit
) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filterOpen by remember { mutableStateOf(false) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var includeUngrouped by remember { mutableStateOf(false) }
    var includeUncategorized by remember { mutableStateOf(false) }
    var imageCountRange by remember { mutableStateOf(0f..10f) } // 新增：图片数量筛选范围
    var addOpen by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var createProgress by remember { mutableStateOf(0f) }
    var createMessage by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }

    // 【删除】：不再在内部 remember，改用传入的 titleMaxLen 参数
    // val titleMaxLen = remember { SessionPrefs.getTitleMaxLen(ctx) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = if (addOpen || filterOpen) Modifier.fillMaxSize().blur(12.dp) else Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("FiaGoods") },
                actions = {
                    if (!loading) {
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
                    val base = if (query.isBlank()) items else items.filter { it.description.contains(query, true) || it.categories.any { c -> c.contains(query, true) } }
                    val list = base.filter {
                        val groupMatch = if (selectedGroups.isEmpty() && !includeUngrouped) {
                            true
                        } else {
                            it.groupNames.any { g -> selectedGroups.contains(g) } || (includeUngrouped && it.groupNames.isEmpty())
                        }
                        val categoryMatch = if (selectedCategories.isEmpty() && !includeUncategorized) {
                            true
                        } else {
                            it.categories.any { c -> selectedCategories.contains(c) } || (includeUncategorized && it.categories.isEmpty())
                        }
                        val count = it.imageUrls.size
                        val imageCountMatch = count >= imageCountRange.start.roundToInt() && count <= imageCountRange.endInclusive.roundToInt()

                        groupMatch && categoryMatch && imageCountMatch && (!favoritesOnly || favorites.contains(it.id))
                    }
                    val orderedList = list.sortedByDescending { favorites.contains(it.id) }
                    val clipboard = LocalClipboardManager.current
                    val imageLoader = LocalImageLoader.current
                    val conf = LocalConfiguration.current
                    val paginationEnabled = SessionPrefs.isPaginationEnabled(ctx)
                    val pageSize = SessionPrefs.getHomePageSize(ctx).coerceAtLeast(1)
                    var currentPage by remember(orderedList, pageSize, paginationEnabled) { mutableStateOf(1) }
                    val totalPages = if (paginationEnabled) ((orderedList.size + pageSize - 1) / pageSize).coerceAtLeast(1) else 1
                    val displayList = if (paginationEnabled) orderedList.drop(((currentPage - 1).coerceAtLeast(0)) * pageSize).take(pageSize) else orderedList
                    var linkUnchangedMap by remember(items) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

                    androidx.compose.runtime.LaunchedEffect(displayList) {
                        val cols = columnsPerRow.coerceAtLeast(1)
                        val density = ctx.resources.displayMetrics.density
                        val innerWidthDp = conf.screenWidthDp.toFloat() - 24f - 8f * (cols - 1)
                        val widthPx = ((innerWidthDp / cols) * density).toInt()
                        val heightPx = (widthPx * 9 / 16)

                        val gson = Gson()
                        val prevJson = SessionPrefs.getLinkSnapshot(ctx)

                        // 使用 TypeToken.getParameterized 防止崩溃
                        val prevType = TypeToken.getParameterized(Map::class.java, String::class.java, LinkThumb::class.java).type

                        val prevMap: Map<String, LinkThumb> = try {
                            prevJson?.let { gson.fromJson<Map<String, LinkThumb>>(it, prevType) } ?: emptyMap()
                        } catch (_: Exception) { emptyMap() }

                        val newUnchanged = displayList.associate { it.id to ((prevMap[it.id]?.link ?: "") == it.link) }
                        linkUnchangedMap = newUnchanged
                        val prefetch = displayList.take(100)
                            .filter { !(newUnchanged[it.id] ?: false) }
                            .mapNotNull { it.imageUrls.firstOrNull() }
                        prefetch.forEach { u ->
                            val t = buildOssThumbnailUrl(u, widthPx)
                            val req = ImageRequest.Builder(ctx)
                                .data(t)
                                .size(widthPx, heightPx)
                                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                                .memoryCacheKey(u)
                                .diskCacheKey(t)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .build()
                            imageLoader.enqueue(req)
                        }
                        val currentMap = displayList.associate { it.id to LinkThumb(it.link, it.imageUrls.firstOrNull()) }
                        try {
                            SessionPrefs.setLinkSnapshot(ctx, gson.toJson(currentMap))
                        } catch (_: Exception) { }
                    }
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
                        if (paginationEnabled) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { if (currentPage > 1) currentPage -= 1 }, enabled = currentPage > 1) { Text("上一页") }
                                Text("第 $currentPage / $totalPages 页", style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = { if (currentPage < totalPages) currentPage += 1 }, enabled = currentPage < totalPages) { Text("下一页") }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(columnsPerRow.coerceAtLeast(1)),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 12.dp + bottom),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalItemSpacing = 8.dp
                        ) {
                            items(displayList, key = { it.id }) { item ->
                                Card(elevation = CardDefaults.cardElevation(), modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onItemClick(item) }, onLongClick = {
                                    clipboard.setText(AnnotatedString(item.link))
                                    Toast.makeText(ctx, "链接已复制", Toast.LENGTH_SHORT).show()
                                })) {
                                    Column {
                                        var previewIndex by remember(item.id) { mutableStateOf(0) }
                                        val previewUrl = item.imageUrls.getOrNull(previewIndex)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        ) {
                                            val conf = LocalConfiguration.current
                                            val cols = columnsPerRow.coerceAtLeast(1)
                                            val density = ctx.resources.displayMetrics.density
                                            val innerWidthDp = conf.screenWidthDp.toFloat() - 24f - 8f * (cols - 1)
                                            val widthPx = ((innerWidthDp / cols) * density).toInt()
                                            val thumbUrl = previewUrl?.let { buildOssThumbnailUrl(it, widthPx) }
                                            val request = remember(thumbUrl) {
                                                ImageRequest.Builder(ctx)
                                                    .data(thumbUrl)
                                                    .size(widthPx, (widthPx * 9 / 16))
                                                    .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                                                    .memoryCacheKey(previewUrl)
                                                    .diskCacheKey(thumbUrl ?: previewUrl)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                                    .networkCachePolicy(if (linkUnchangedMap[item.id] == true) CachePolicy.READ_ONLY else CachePolicy.ENABLED)
                                                    .crossfade(false)
                                                    .build()
                                            }
                                            AsyncImage(
                                                model = request,
                                                contentDescription = null,
                                                contentScale = ContentScale.FillWidth,
                                                modifier = Modifier.fillMaxWidth(),
                                                onError = {
                                                    if (previewIndex < item.imageUrls.size - 1) {
                                                        previewIndex++
                                                    }
                                                }
                                            )
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
                                            val limit = titleMaxLen
                                            val title = if (limit <= 0) "" else if (item.description.length <= limit) item.description else item.description.take(limit)
                                            Text(title, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (addOpen) {
                // ... add dialog content ... (保持原样，为了节省篇幅已折叠，实际使用时请直接保留这部分代码)
                var description by remember { mutableStateOf("") }
                var newCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
                var newGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
                var link by remember { mutableStateOf("") }
                var priceText by remember { mutableStateOf("") }
                var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                var imageUrlsText by remember { mutableStateOf("") }
                val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> imageUris = uris ?: emptyList() }
                val canSave = true
                AppDialog(onDismiss = { addOpen = false }, title = "新增商品", content = {
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") }, singleLine = true)
                    val groupOptions = remember(items) { items.flatMap { it.groupNames }.filter { it.isNotBlank() }.distinct().sorted() }
                    val categoryOptions = remember(items) { items.flatMap { it.categories }.filter { it.isNotBlank() }.distinct().sorted() }
                    Text("类别", style = MaterialTheme.typography.titleMedium)
                    var newCategoryInput by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = newCategoryInput, onValueChange = { newCategoryInput = it }, placeholder = { Text("输入类别后添加") }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val v = newCategoryInput.trim()
                            if (v.isNotBlank()) {
                                newCategories = newCategories + v
                                newCategoryInput = ""
                            }
                        }) { Text("添加") }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(newCategories.toList()) { c ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), shape = RoundedCornerShape(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(c, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp).clickable { newCategories = newCategories - c })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryOptions.filter { !newCategories.contains(it) }) { c ->
                            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.clickable { newCategories = newCategories + c }.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(c, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("分组", style = MaterialTheme.typography.titleMedium)
                    var newGroupInput by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = newGroupInput, onValueChange = { newGroupInput = it }, placeholder = { Text("输入分组后添加") }, singleLine = true, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val v = newGroupInput.trim()
                            if (v.isNotBlank()) {
                                newGroups = newGroups + v
                                newGroupInput = ""
                            }
                        }) { Text("添加") }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(newGroups.toList()) { g ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), shape = RoundedCornerShape(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(g, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp).clickable { newGroups = newGroups - g })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(groupOptions.filter { !newGroups.contains(it) }) { g ->
                            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.clickable { newGroups = newGroups + g }.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(g, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("链接") }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("售价") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { pickImages.launch("image/*") }) { Text("选择图片") }
                        if (imageUris.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Text("已选择" + imageUris.size + "张", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = imageUrlsText, onValueChange = { imageUrlsText = it }, label = { Text("图片URL（多行，每行一个）") }, singleLine = false)
                    Text("提示：换行为多个", style = MaterialTheme.typography.bodySmall)
                }, actions = {
                    androidx.compose.material3.TextButton(onClick = { addOpen = false }) { Text("取消") }
                    androidx.compose.material3.TextButton(onClick = {
                        val id = UUID.randomUUID().toString()
                        val priceVal = priceText.toDoubleOrNull() ?: 0.0
                        val item = CargoItem(
                            id = id,
                            description = description,
                            imageUrls = emptyList(),
                            groupNames = newGroups.toList(),
                            categories = newCategories.toList(),
                            price = priceVal,
                            link = link
                        )
                        showCreateDialog = true
                        creating = true
                        createProgress = 0f
                        createMessage = "正在创建商品数据…"
                        createError = null
                        UploadState.start("新增商品")
                        val urlsList = imageUrlsText.lines().map { it.trim() }.filter { it.isNotBlank() }
                        onCreateItemWithImagesAndUrls(item, imageUris, urlsList, { done, total ->
                            val t = total.coerceAtLeast(1)
                            createProgress = done.toFloat() / t.toFloat()
                            createMessage = "已上传第" + done + "/" + total + "张"
                            UploadState.update(createProgress)
                        }, { ok ->
                            creating = false
                            createProgress = 1f
                            if (!ok) {
                                createError = "创建或上传失败"
                            }
                            UploadState.finish()
                        })
                        addOpen = false
                    }, enabled = canSave) { Text("保存") }
                    if (creating) {
                        androidx.compose.material3.TextButton(onClick = { UploadState.setBackground(true); showCreateDialog = false }) { Text("后台完成") }
                    }
                })
            }

            if (showCreateDialog) {
                AppDialog(onDismiss = { if (!creating) { showCreateDialog = false } }, title = "新增商品", content = {
                    androidx.compose.material3.LinearProgressIndicator(progress = createProgress, modifier = Modifier.fillMaxWidth())
                    Text(((createProgress * 100).toInt()).toString() + "%")
                    if (createMessage.isNotBlank()) {
                        Text(createMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (createError != null) {
                        Text(createError!!, color = MaterialTheme.colorScheme.error)
                    }
                }, actions = {
                    if (creating) {
                        androidx.compose.material3.TextButton(onClick = { UploadState.setBackground(true); showCreateDialog = false }) { Text("后台完成") }
                    }
                    if (!creating) {
                        androidx.compose.material3.TextButton(onClick = { showCreateDialog = false }) { Text("关闭") }
                    }
                })
            }

            androidx.compose.runtime.LaunchedEffect(createProgress, creating) {
                if (!creating && createProgress >= 1f) {
                    showCreateDialog = false
                }
            }

            if (filterOpen) {
                // ... filter dialog content ... (保持原样，已折叠)
                val groupOptions = remember(items) { items.flatMap { it.groupNames }.filter { it.isNotBlank() }.distinct().sorted() }
                val categoryOptions = remember(items) { items.flatMap { it.categories }.filter { it.isNotBlank() }.distinct().sorted() }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = includeUngrouped, onCheckedChange = { includeUngrouped = it })
                                Text("未分组", style = MaterialTheme.typography.bodyMedium)
                            }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = includeUncategorized, onCheckedChange = { includeUncategorized = it })
                                Text("未分类", style = MaterialTheme.typography.bodyMedium)
                            }
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

                    // 修改：将图片数量筛选移至类别下方，并移除 steps 实现平滑滑动
                    Spacer(Modifier.height(12.dp))
                    Text("图片数量", style = MaterialTheme.typography.titleMedium)
                    RangeSlider(
                        value = imageCountRange,
                        onValueChange = { imageCountRange = it },
                        valueRange = 0f..10f,
                        // steps 移除，默认为 0，实现平滑滑动
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${imageCountRange.start.roundToInt()}")
                        Text("${imageCountRange.endInclusive.roundToInt()}")
                    }

                }, actions = {
                    androidx.compose.material3.TextButton(onClick = {
                        selectedGroups = emptySet()
                        selectedCategories = emptySet()
                        includeUngrouped = false
                        includeUncategorized = false
                        imageCountRange = 0f..10f // 重置图片数量范围
                    }) { Text("清空") }
                    androidx.compose.material3.TextButton(onClick = { filterOpen = false }) { Text("确定") }
                })
            }
        }
        val bg = UploadState.background.collectAsState().value
        val up = UploadState.uploading.collectAsState().value
        val p = UploadState.progress.collectAsState().value
        if (bg && up) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Card(
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(),
                    modifier = Modifier
                        .padding(16.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable { showCreateDialog = true; UploadState.setBackground(false) }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = p, modifier = Modifier.size(40.dp))
                        Text(((p * 100).toInt()).toString() + "%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}