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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.material3.LinearProgressIndicator
import com.glassous.fiagoods.data.model.CargoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(item: CargoItem, onBack: () -> Unit, onSave: (String, Map<String, Any?>) -> Unit, onDelete: (String, (Boolean) -> Unit) -> Unit, onAddImage: (Uri) -> Unit, onDeleteImage: (String) -> Unit, onAddImageWithProgress: (Uri, (Long, Long) -> Unit, (Boolean, String?) -> Unit) -> Unit, onDeleteImageWithProgress: (String, (Float) -> Unit, (Boolean, String?) -> Unit) -> Unit) {
    var previewUrl by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(item.name) }
    var category by remember { mutableStateOf(item.category) }
    var priceText by remember { mutableStateOf(item.price?.let { String.format("%.2f", it) } ?: "") }
    var soldText by remember { mutableStateOf(item.sold.toString()) }
    var unlimited by remember { mutableStateOf(item.stock == null) }
    var stockText by remember { mutableStateOf(item.stock?.toString() ?: "") }
    var brief by remember { mutableStateOf(item.brief) }
    var description by remember { mutableStateOf(item.description) }
    var specs by remember { mutableStateOf(item.specs) }
    var deleteCandidateUrl by remember { mutableStateOf<String?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val pickNew = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            showUploadDialog = true
            uploading = true
            uploadProgress = 0f
            uploadError = null
            onAddImageWithProgress(uri, { current, total ->
                uploadProgress = if (total > 0) current.toFloat() / total.toFloat() else 0f
            }, { ok, err ->
                uploading = false
                if (ok) {
                    showUploadDialog = false
                } else {
                    uploadError = err ?: "上传失败"
                }
            })
        }
    }
    
    fun download(url: String, fileName: String) {
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, fileName)
        dm.enqueue(request)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            modifier = Modifier.height(56.dp),
            title = { Text("详情") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            actions = {
                if (!editing) {
                    IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, contentDescription = null) }
                } else {
                    IconButton(onClick = { editing = false }) { Icon(Icons.Filled.Close, contentDescription = null) }
                    val canSave = name.isNotBlank() && category.isNotBlank() && (unlimited || stockText.toIntOrNull() != null) && (priceText.isBlank() || priceText.toDoubleOrNull() != null) && soldText.toIntOrNull() != null
                    IconButton(onClick = {
                        val patch = mutableMapOf<String, Any?>()
                        patch["name"] = name
                        patch["category"] = category
                        patch["price"] = priceText.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                        patch["sold"] = soldText.toIntOrNull() ?: item.sold
                        patch["stock"] = if (unlimited) null else stockText.toIntOrNull()
                        patch["brief"] = brief
                        patch["description"] = description
                        patch["specs"] = specs
                        onSave(item.id, patch)
                        editing = false
                    }, enabled = canSave) { Icon(Icons.Filled.Save, contentDescription = null) }
                }
            }
        )
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                if (!editing) {
                    Text(item.name, style = MaterialTheme.typography.headlineSmall)
                } else {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true)
                }
            }
            item {
                if (item.imageUrls.isEmpty()) {
                    Button(onClick = { pickNew.launch("image/*") }) { Text("新增图片") }
                } else if (item.imageUrls.size == 1) {
                    val url = item.imageUrls.first()
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(12.dp))) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clickable { previewUrl = url }
                        )
                        Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { deleteCandidateUrl = url }) { Icon(Icons.Filled.Close, contentDescription = null) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { pickNew.launch("image/*") }) { Text("新增图片") }
                } else {
                    val listState = rememberLazyListState()
                    LazyRow(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(item.imageUrls) { url ->
                            Box(modifier = Modifier.width(240.dp).height(240.dp).clip(RoundedCornerShape(12.dp))) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize().clickable { previewUrl = url }
                                )
                                Row(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { deleteCandidateUrl = url }) { Icon(Icons.Filled.Close, contentDescription = null) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { pickNew.launch("image/*") }) { Text("新增图片") }
                }
            }
            if (!editing) {
                item { FieldCard(label = "类别", value = item.category) }
                item { FieldCard(label = "售价", value = item.price?.let { "¥" + String.format("%.2f", it) } ?: "¥0.00") }
                item { FieldCard(label = "销量", value = item.sold.toString()) }
                item { FieldCard(label = "库存", value = if (item.stock == null) "无限" else item.stock.toString()) }
                item { FieldCard(label = "简介", value = item.brief) }
                item { FieldCard(label = "描述", value = item.description) }
                item { FieldCard(label = "规格", value = item.specs) }
            } else {
                item { OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("类别") }, singleLine = true) }
                item { OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("售价") }, singleLine = true) }
                item { OutlinedTextField(value = soldText, onValueChange = { soldText = it }, label = { Text("销量") }, singleLine = true) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = unlimited, onCheckedChange = { unlimited = it })
                        Text("无限库存")
                    }
                }
                if (!unlimited) {
                    item { OutlinedTextField(value = stockText, onValueChange = { stockText = it }, label = { Text("库存") }, singleLine = true) }
                }
                item { OutlinedTextField(value = brief, onValueChange = { brief = it }, label = { Text("简介") }) }
                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") }) }
                item { OutlinedTextField(value = specs, onValueChange = { specs = it }, label = { Text("规格") }) }
            }
            item {
                Button(onClick = { showDeleteConfirm = true }) { Text("删除商品") }
            }
        }
    }
    if (previewUrl != null) {
        Dialog(onDismissRequest = { previewUrl = null }) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                AsyncImage(model = previewUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)))
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { previewUrl?.let { download(it, (item.name.ifBlank { "image" }) + ".jpg") } }) { Icon(Icons.Filled.Download, contentDescription = null) }
                    IconButton(onClick = { previewUrl = null }) { Icon(Icons.Filled.Close, contentDescription = null) }
                }
            }
        }
    }
    if (showUploadDialog) {
        Dialog(onDismissRequest = { if (!uploading) { showUploadDialog = false } }) {
            Card(elevation = CardDefaults.cardElevation()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("正在上传图片")
                    LinearProgressIndicator(progress = uploadProgress, modifier = Modifier.fillMaxWidth())
                    Text(((uploadProgress * 100).toInt()).toString() + "%")
                    if (uploadError != null) {
                        Text(uploadError!!, color = MaterialTheme.colorScheme.error)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { showUploadDialog = false }) { Text("关闭") }
                        }
                    }
                }
            }
        }
    }
    if (deleteCandidateUrl != null) {
        Dialog(onDismissRequest = { if (!uploading) { deleteCandidateUrl = null } }) {
            Card(elevation = CardDefaults.cardElevation()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("确认删除该图片？")
                    var deleting by remember { mutableStateOf(false) }
                    var deleteProgress by remember { mutableStateOf(0f) }
                    var deleteError by remember { mutableStateOf<String?>(null) }
                    if (!deleting) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { deleteCandidateUrl = null }) { Text("取消") }
                            Button(onClick = {
                                deleting = true
                                deleteError = null
                                deleteProgress = 0f
                                onDeleteImageWithProgress(deleteCandidateUrl!!, { p ->
                                    deleteProgress = p.coerceIn(0f, 1f)
                                }, { ok, err ->
                                    deleting = false
                                    if (ok) {
                                        deleteCandidateUrl = null
                                    } else {
                                        deleteError = err ?: "删除失败"
                                    }
                                })
                            }) { Text("确定") }
                        }
                    } else {
                        LinearProgressIndicator(progress = deleteProgress, modifier = Modifier.fillMaxWidth())
                        Text(((deleteProgress * 100).toInt()).toString() + "%")
                        if (deleteError != null) {
                            Text(deleteError!!, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { deleteCandidateUrl = null }) { Text("关闭") }
                        }
                    }
                }
            }
        }
    }
    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Card(elevation = CardDefaults.cardElevation()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("确认删除该商品？")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { showDeleteConfirm = false }) { Text("取消") }
                        Button(onClick = {
                            onDelete(item.id) { ok ->
                                showDeleteConfirm = false
                                if (ok) onBack()
                            }
                        }) { Text("确定") }
                    }
                }
            }
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
                Icon(imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy, contentDescription = null)
            }
        }
    }
}
