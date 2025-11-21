package com.glassous.fiagoods.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.blur
import android.app.DownloadManager
import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import coil.compose.AsyncImage
import coil.compose.AsyncImage
import androidx.compose.material3.LinearProgressIndicator
import com.glassous.fiagoods.data.model.CargoItem
import com.glassous.fiagoods.ui.components.AppDialog
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import com.glassous.fiagoods.ui.global.UploadState
import androidx.compose.ui.platform.LocalConfiguration
import coil.request.ImageRequest
import com.glassous.fiagoods.util.buildOssThumbnailUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(item: CargoItem, onBack: () -> Unit, onSave: (String, Map<String, Any?>, (Boolean) -> Unit) -> Unit, onDelete: (String, (Boolean) -> Unit) -> Unit, onAddImage: (Uri) -> Unit, onDeleteImage: (String) -> Unit, onAddImageWithProgress: (Uri, (Long, Long) -> Unit, (Boolean, String?) -> Unit) -> Unit, onDeleteImageWithProgress: (String, (Float) -> Unit, (Boolean, String?) -> Unit) -> Unit, onAddImageUrlsDirect: (List<String>, (Boolean) -> Unit) -> Unit, groupOptions: List<String> = emptyList(), categoryOptions: List<String> = emptyList()) {
    var previewUrl by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(item.description) }
    var category by remember { mutableStateOf(item.category) }
    var groupName by remember { mutableStateOf(item.groupName) }
    var link by remember { mutableStateOf(item.link) }
    var priceText by remember { mutableStateOf(String.format("%.2f", item.price)) }
    var deleteCandidateUrl by remember { mutableStateOf<String?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadInBackground by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveProgress by remember { mutableStateOf(0f) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showItemSaveDialog by remember { mutableStateOf(false) }
    var savingItem by remember { mutableStateOf(false) }
    var itemSaveProgress by remember { mutableStateOf(0f) }
    var itemSaveError by remember { mutableStateOf<String?>(null) }
    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var urlDialogText by remember { mutableStateOf("") }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var bulkDeleting by remember { mutableStateOf(false) }
    var bulkProgress by remember { mutableStateOf(0f) }
    var bulkError by remember { mutableStateOf<String?>(null) }
    var bulkSelected by remember { mutableStateOf<Set<String>>(emptySet()) }
    val pickNew = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val list = uris ?: emptyList()
        if (list.isNotEmpty()) {
            pendingUris = list
            showUploadDialog = true
            uploading = true
            uploadProgress = 0f
            uploadError = null
            UploadState.start("图片上传")
            var idx = 0
            fun uploadNext() {
                val total = pendingUris.size
                val target = pendingUris[idx]
                onAddImageWithProgress(target, { current, totalBytes ->
                    val per = if (totalBytes > 0) current.toFloat() / totalBytes.toFloat() else 0f
                    uploadProgress = (idx.toFloat() + per) / total.toFloat()
                    UploadState.update(uploadProgress)
                }, { ok, err ->
                    if (!ok) {
                        uploading = false
                        uploadError = err ?: "上传失败"
                        UploadState.setBackground(false)
                    } else {
                        idx++
                        if (idx >= total) {
                            uploading = false
                            showUploadDialog = false
                            UploadState.finish()
                        } else {
                            uploadNext()
                        }
                    }
                })
            }
            uploadNext()
        }
    }
    
    val scope = rememberCoroutineScope()
    val http = remember { OkHttpClient() }
    fun saveToGallery(url: String, fileName: String, mime: String = "image/jpeg"): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FiaGoods")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        ctx.contentResolver.openOutputStream(uri)?.use { os ->
            val req = Request.Builder().url(url).build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val bytes = resp.body?.bytes() ?: return false
                os.write(bytes)
            }
        } ?: return false
        return true
    }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val clipboard = LocalClipboardManager.current
    val hasDialog = previewUrl != null || showUploadDialog || deleteCandidateUrl != null || showSaveDialog || showDeleteConfirm || showBulkDeleteDialog
    Column(modifier = if (hasDialog) Modifier.fillMaxSize().blur(12.dp) else Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("详情") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            actions = {
                if (!editing) {
                    IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, contentDescription = null) }
                } else {
                    IconButton(onClick = { editing = false }) { Icon(Icons.Filled.Close, contentDescription = null) }
                    IconButton(onClick = {
                        val patch = mutableMapOf<String, Any?>()
                        if (description != item.description) patch["description"] = description
                        if (category != item.category) patch["category"] = category
                        if (groupName != item.groupName) patch["group_name"] = groupName
                        if (link != item.link) patch["link"] = link
                        val priceVal = priceText.toDoubleOrNull()
                        if (priceVal != null && priceVal != item.price) patch["price"] = priceVal
                        showItemSaveDialog = true
                        savingItem = true
                        itemSaveProgress = 0f
                        itemSaveError = null
                        onSave(item.id, patch) { ok ->
                            savingItem = false
                            itemSaveProgress = 1f
                            if (!ok) {
                                itemSaveError = "保存失败"
                            }
                        }
                        editing = false
                    }) { Icon(Icons.Filled.Save, contentDescription = null) }
                }
            }
        )
        LazyColumn(modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp + bottom)) {
            item {
                if (!editing) {
                    var copied by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.description, style = MaterialTheme.typography.displaySmall.copy(fontSize = 16.sp, lineHeight = 16.sp), modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(item.description))
                            copied = true
                        }) {
                            Icon(imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
            item {
                if (item.imageUrls.isEmpty()) {
                    if (editing) Button(onClick = { pickNew.launch("image/*") }) { Text("新增图片") }
                } else if (item.imageUrls.size == 1) {
                    val url = item.imageUrls.first()
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).clip(RoundedCornerShape(12.dp))) {
                        val ctx = LocalContext.current
                        val conf = LocalConfiguration.current
                        val widthPx = (conf.screenWidthDp * ctx.resources.displayMetrics.density).toInt()
                        val thumbUrl = buildOssThumbnailUrl(url, widthPx)
                        val request = ImageRequest.Builder(ctx)
                            .data(thumbUrl)
                            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                            .crossfade(true)
                            .build()
                        AsyncImage(
                            model = request,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).clickable { previewUrl = url }
                        )
                        if (editing) {
                            Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { deleteCandidateUrl = url }) { Icon(Icons.Filled.Close, contentDescription = null) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (editing) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { pickNew.launch("image/*") }) { Text("新增图片") }
                            Button(onClick = { bulkSelected = emptySet(); showBulkDeleteDialog = true }, enabled = item.imageUrls.isNotEmpty()) { Text("批量删除图片") }
                        }
                        var urlInlineText by remember { mutableStateOf("") }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = urlInlineText, onValueChange = { urlInlineText = it }, label = { Text("图片URL") }, modifier = Modifier.weight(1f), singleLine = false)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { showAddUrlDialog = true }) { Icon(Icons.Filled.Add, contentDescription = null) }
                        }
                    } else {
                        Button(onClick = {
                            showSaveDialog = true
                            saving = true
                            saveProgress = 0f
                            saveError = null
                            scope.launch {
                                val total = item.imageUrls.size.coerceAtLeast(1)
                                var done = 0
                                for ((idx, u) in item.imageUrls.withIndex()) {
                                    val name = (item.description.ifBlank { "image" }) + "_" + (idx + 1) + ".jpg"
                                    val ok = withContext(Dispatchers.IO) { saveToGallery(u, name) }
                                    done++
                                    saveProgress = done.toFloat() / total.toFloat()
                                    if (!ok) {
                                        saveError = "第" + (idx + 1) + "张保存失败"
                                        saving = false
                                        break
                                    }
                                }
                                if (saveError == null) {
                                    saving = false
                                }
                            }
                        }) { Text("下载全部图片") }
                    }
                } else {
                    val state = rememberCarouselState { item.imageUrls.size }
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    var imageHeights by remember { mutableStateOf<Map<Int, androidx.compose.ui.unit.Dp>>(emptyMap()) }
                    HorizontalMultiBrowseCarousel(
                        state = state,
                        preferredItemWidth = 220.dp,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                        itemSpacing = 8.dp,
                        flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state)
                    ) { itemIndex: Int ->
                        val url = item.imageUrls[itemIndex]
                        val itemHeight = imageHeights[itemIndex] ?: 220.dp
                        Box(modifier = Modifier.height(itemHeight).fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
                            val ctx = LocalContext.current
                            val widthPx = with(density) { 220.dp.toPx().toInt() }
                            val thumbUrl = buildOssThumbnailUrl(url, widthPx)
                            val request = ImageRequest.Builder(ctx)
                                .data(thumbUrl)
                                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                                .crossfade(true)
                                .build()
                            AsyncImage(
                                model = request,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).clickable { previewUrl = url },
                                onSuccess = { success ->
                                    val dw = success.result.drawable.intrinsicWidth
                                    val dh = success.result.drawable.intrinsicHeight
                                    if (dw > 0 && dh > 0) {
                                        val hPx = with(density) { 220.dp.toPx() } * (dh.toFloat() / dw.toFloat())
                                        val hDp = with(density) { hPx.toDp() }
                                        imageHeights = imageHeights + (itemIndex to hDp)
                                    }
                                }
                            )
                            if (editing) {
                                Row(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { deleteCandidateUrl = url }) { Icon(Icons.Filled.Close, contentDescription = null) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (editing) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { pickNew.launch("image/*") }) { Text("新增图片") }
                            Button(onClick = { bulkSelected = emptySet(); showBulkDeleteDialog = true }, enabled = item.imageUrls.isNotEmpty()) { Text("批量删除图片") }
                        }
                        var urlInlineText by remember { mutableStateOf("") }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = urlInlineText, onValueChange = { urlInlineText = it }, label = { Text("图片URL") }, modifier = Modifier.weight(1f), singleLine = false)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { showAddUrlDialog = true }) { Icon(Icons.Filled.Add, contentDescription = null) }
                        }
                    } else {
                        Button(onClick = {
                            showSaveDialog = true
                            saving = true
                            saveProgress = 0f
                            saveError = null
                            scope.launch {
                                val total = item.imageUrls.size.coerceAtLeast(1)
                                var done = 0
                                for ((idx, u) in item.imageUrls.withIndex()) {
                                    val name = (item.description.ifBlank { "image" }) + "_" + (idx + 1) + ".jpg"
                                    val ok = withContext(Dispatchers.IO) { saveToGallery(u, name) }
                                    done++
                                    saveProgress = done.toFloat() / total.toFloat()
                                    if (!ok) {
                                        saveError = "第" + (idx + 1) + "张保存失败"
                                        saving = false
                                        break
                                    }
                                }
                                if (saveError == null) {
                                    saving = false
                                }
                            }
                        }) { Text("下载全部图片") }
                    }
                }
            }
            if (!editing) {
                item { FieldCard(label = "类别", value = item.category) }
                item { FieldCard(label = "售价", value = "¥" + String.format("%.2f", item.price)) }
                item { FieldCard(label = "分组", value = item.groupName) }
                item { FieldCard(label = "链接", value = item.link) }
            } else {
                item {
                    var categoryExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("类别") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), trailingIcon = { IconButton(onClick = { categoryExpanded = !categoryExpanded }) { Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = null) } })
                        DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            categoryOptions.forEach { c ->
                                DropdownMenuItem(text = { Text(c) }, onClick = { category = c; categoryExpanded = false })
                            }
                        }
                    }
                }
                item {
                    var groupExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("分组") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), trailingIcon = { IconButton(onClick = { groupExpanded = !groupExpanded }) { Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = null) } })
                        DropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                            groupOptions.forEach { g ->
                                DropdownMenuItem(text = { Text(g) }, onClick = { groupName = g; groupExpanded = false })
                            }
                        }
                    }
                }
                item { OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("链接") }, singleLine = false, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp, max = 120.dp), shape = RoundedCornerShape(20.dp)) }
                item { OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("售价") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) }
                
            }
            item {
                Button(onClick = { showDeleteConfirm = true }) { Text("删除商品") }
            }
        }
        if (showAddUrlDialog) {
            AppDialog(onDismiss = { showAddUrlDialog = false }, title = "添加图片URL", content = {
                OutlinedTextField(value = urlDialogText, onValueChange = { urlDialogText = it }, label = { Text("每行一个URL") }, modifier = Modifier.fillMaxWidth(), singleLine = false)
                Text("提示：换行为多个")
            }, actions = {
                androidx.compose.material3.TextButton(onClick = { showAddUrlDialog = false }) { Text("取消") }
                androidx.compose.material3.TextButton(onClick = {
                    val urls = urlDialogText.lines().map { it.trim() }.filter { it.isNotBlank() }
                    onAddImageUrlsDirect(urls) { }
                    showAddUrlDialog = false
                    urlDialogText = ""
                }) { Text("保存") }
            })
        }
        if (showBulkDeleteDialog) {
            AppDialog(onDismiss = { if (!bulkDeleting) { showBulkDeleteDialog = false } }, title = if (bulkDeleting) "正在批量删除图片" else "批量删除图片", content = {
                if (!bulkDeleting) {
                    val ctx = LocalContext.current
                    val conf = LocalConfiguration.current
                    val widthPx = (conf.screenWidthDp * ctx.resources.displayMetrics.density).toInt()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item.imageUrls.forEach { u ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(checked = bulkSelected.contains(u), onCheckedChange = { checked ->
                                    bulkSelected = if (checked) bulkSelected + u else bulkSelected - u
                                })
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.height(64.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))) {
                                    val thumbUrl = buildOssThumbnailUrl(u, widthPx)
                                    val request = ImageRequest.Builder(ctx)
                                        .data(thumbUrl)
                                        .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                                        .crossfade(true)
                                        .build()
                                    AsyncImage(model = request, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                } else {
                    LinearProgressIndicator(progress = bulkProgress, modifier = Modifier.fillMaxWidth())
                    if (bulkError != null) {
                        Text(bulkError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }, actions = {
                androidx.compose.material3.TextButton(onClick = { showBulkDeleteDialog = false }, enabled = !bulkDeleting) { Text("取消") }
                androidx.compose.material3.TextButton(onClick = { UploadState.setBackground(true); showBulkDeleteDialog = false }, enabled = bulkDeleting) { Text("后台完成") }
                androidx.compose.material3.TextButton(onClick = {
                    if (bulkSelected.isNotEmpty()) {
                        bulkDeleting = true
                        bulkError = null
                        bulkProgress = 0f
                        UploadState.start("批量删除图片")
                        val targets = bulkSelected.toList()
                        var idx = 0
                        fun deleteNext() {
                            val total = targets.size
                            val url = targets[idx]
                            onDeleteImageWithProgress(url, { per ->
                                bulkProgress = ((idx.toFloat()) + per).div(total.toFloat())
                                UploadState.update(bulkProgress)
                            }, { ok, err ->
                                if (!ok) {
                                    bulkDeleting = false
                                    bulkError = err ?: "删除失败"
                                    UploadState.setBackground(false)
                                } else {
                                    idx++
                                    if (idx >= total) {
                                        bulkDeleting = false
                                        showBulkDeleteDialog = false
                                        bulkSelected = emptySet()
                                        UploadState.finish()
                                    } else {
                                        deleteNext()
                                    }
                                }
                            })
                        }
                        deleteNext()
                    }
                }, enabled = !bulkDeleting) { Text("删除") }
            })
        }
    }
    if (previewUrl != null) {
        AppDialog(onDismiss = { previewUrl = null }, title = "图片预览", content = {
            val ctx = LocalContext.current
            val conf = LocalConfiguration.current
            val widthPx = (conf.screenWidthDp * ctx.resources.displayMetrics.density).toInt()
            val thumbUrl = buildOssThumbnailUrl(previewUrl!!, widthPx)
            val request = ImageRequest.Builder(ctx)
                .data(thumbUrl)
                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                .crossfade(true)
                .build()
            AsyncImage(model = request, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)))
        }, actions = {
            androidx.compose.material3.TextButton(onClick = { previewUrl = null }) { Text("取消") }
            androidx.compose.material3.TextButton(onClick = {
                val fname = (item.description.ifBlank { "image" }) + ".jpg"
                showSaveDialog = true
                saving = true
                saveProgress = 0f
                saveError = null
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { previewUrl?.let { saveToGallery(it, fname) } ?: false }
                    saveProgress = 1f
                    saving = false
                    if (!ok) saveError = "保存失败"
                }
            }) { Text("下载") }
        })
    }
    if (showUploadDialog) {
        AppDialog(onDismiss = { if (!uploading) { showUploadDialog = false } }, title = "正在上传图片", content = {
            LinearProgressIndicator(progress = uploadProgress, modifier = Modifier.fillMaxWidth())
            Text(((uploadProgress * 100).toInt()).toString() + "%")
            if (uploadError != null) {
                Text(uploadError!!, color = MaterialTheme.colorScheme.error)
            }
        }, actions = {
            androidx.compose.material3.TextButton(onClick = { UploadState.setBackground(true); showUploadDialog = false }, enabled = uploading) { Text("后台完成") }
            androidx.compose.material3.TextButton(onClick = { showUploadDialog = false }, enabled = !uploading) { Text("关闭") }
        })
    }
    run {
        val bg = UploadState.background.collectAsState().value
        val up = UploadState.uploading.collectAsState().value
        val p = UploadState.progress.collectAsState().value
        val lbl = UploadState.label.collectAsState().value
        if (bg && up) {
            Box(modifier = Modifier.fillMaxSize()) {
                Card(
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable {
                            val text = lbl ?: ""
                            if (text.contains("删除")) {
                                showBulkDeleteDialog = true
                            } else {
                                showUploadDialog = true
                            }
                            UploadState.setBackground(false)
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = p, modifier = Modifier.size(40.dp))
                        Text(((p * 100).toInt()).toString() + "%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
    if (showItemSaveDialog) {
        AppDialog(onDismiss = { if (!savingItem) { showItemSaveDialog = false } }, title = "正在保存商品", content = {
            LinearProgressIndicator(progress = itemSaveProgress, modifier = Modifier.fillMaxWidth())
            Text(((itemSaveProgress * 100).toInt()).toString() + "%")
            if (itemSaveError != null) {
                Text(itemSaveError!!, color = MaterialTheme.colorScheme.error)
            }
        }, actions = {
            androidx.compose.material3.TextButton(onClick = { showItemSaveDialog = false }, enabled = !savingItem) { Text("关闭") }
        })
    }
    if (deleteCandidateUrl != null) {
        var deleting by remember { mutableStateOf(false) }
        var deleteProgress by remember { mutableStateOf(0f) }
        var deleteError by remember { mutableStateOf<String?>(null) }
        AppDialog(onDismiss = { if (!deleting) { deleteCandidateUrl = null } }, title = "确认删除该图片？", content = {
            if (deleting) {
                LinearProgressIndicator(progress = deleteProgress, modifier = Modifier.fillMaxWidth())
                Text(((deleteProgress * 100).toInt()).toString() + "%")
                if (deleteError != null) {
                    Text(deleteError!!, color = MaterialTheme.colorScheme.error)
                }
            }
        }, actions = {
            androidx.compose.material3.TextButton(onClick = { deleteCandidateUrl = null }, enabled = !deleting) { Text("取消") }
            androidx.compose.material3.TextButton(onClick = { UploadState.setBackground(true); deleteCandidateUrl = null }, enabled = deleting) { Text("后台完成") }
            androidx.compose.material3.TextButton(onClick = {
                deleting = true
                deleteError = null
                deleteProgress = 0f
                UploadState.start("删除图片")
                onDeleteImageWithProgress(deleteCandidateUrl!!, { p ->
                    deleteProgress = p.coerceIn(0f, 1f)
                    UploadState.update(deleteProgress)
                }, { ok, err ->
                    deleting = false
                    if (ok) {
                        deleteCandidateUrl = null
                        UploadState.finish()
                    } else {
                        deleteError = err ?: "删除失败"
                        UploadState.setBackground(false)
                    }
                })
            }, enabled = !deleting) { Text("删除") }
        })
    }
    if (showSaveDialog) {
        AppDialog(onDismiss = { if (!saving) { showSaveDialog = false } }, title = "正在保存图片", content = {
            LinearProgressIndicator(progress = saveProgress, modifier = Modifier.fillMaxWidth())
            Text(((saveProgress * 100).toInt()).toString() + "%")
            if (saveError != null) {
                Text(saveError!!, color = MaterialTheme.colorScheme.error)
            }
        }, actions = {
            androidx.compose.material3.TextButton(onClick = { showSaveDialog = false }, enabled = !saving) { Text("关闭") }
        })
    }
    if (showDeleteConfirm) {
        var deletingItem by remember { mutableStateOf(false) }
        var deletingProgress by remember { mutableStateOf(0f) }
        var deletingError by remember { mutableStateOf<String?>(null) }
        AppDialog(onDismiss = { if (!deletingItem) { showDeleteConfirm = false } }, title = if (deletingItem) "正在删除商品" else "确认删除该商品？", content = {
            if (deletingItem) {
                LinearProgressIndicator(progress = deletingProgress, modifier = Modifier.fillMaxWidth())
                Text(((deletingProgress * 100).toInt()).toString() + "%")
                if (deletingError != null) {
                    Text(deletingError!!, color = MaterialTheme.colorScheme.error)
                }
            }
        }, actions = {
            androidx.compose.material3.TextButton(onClick = { showDeleteConfirm = false }, enabled = !deletingItem) { Text("取消") }
            androidx.compose.material3.TextButton(onClick = {
                deletingItem = true
                deletingError = null
                deletingProgress = 0.5f
                onDelete(item.id) { ok ->
                    deletingItem = false
                    deletingProgress = 1f
                    if (ok) {
                        showDeleteConfirm = false
                        onBack()
                    } else {
                        deletingError = "删除失败"
                    }
                }
            }, enabled = !deletingItem) { Text("删除") }
        })
    }

    LaunchedEffect(uploadProgress, uploading) {
        if (!uploading && uploadProgress >= 1f) {
            showUploadDialog = false
            UploadState.setBackground(false)
        }
    }
    LaunchedEffect(itemSaveProgress, savingItem) {
        if (!savingItem && itemSaveProgress >= 1f) {
            showItemSaveDialog = false
        }
    }
    LaunchedEffect(saveProgress, saving) {
        if (!saving && saveProgress >= 1f) {
            showSaveDialog = false
        }
    }
}

@Composable
private fun FieldCard(label: String, value: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(3000)
            copied = false
        }
    }
    Card(elevation = CardDefaults.cardElevation()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(value))
                copied = true
            }) {
                Icon(imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
