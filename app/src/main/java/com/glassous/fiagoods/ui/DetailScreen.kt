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
import coil.compose.AsyncImage
import com.glassous.fiagoods.data.model.CargoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(item: CargoItem, onBack: () -> Unit, onSave: (String, Map<String, Any?>) -> Unit, onDelete: (String, (Boolean) -> Unit) -> Unit) {
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
                if (item.imageUrls.size <= 1) {
                    val url = item.imageUrls.firstOrNull()
                    if (url != null) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(12.dp)).clickable { previewUrl = url }
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    LazyRow(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(item.imageUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(240.dp).height(240.dp).clip(RoundedCornerShape(12.dp)).clickable { previewUrl = url }
                            )
                        }
                    }
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