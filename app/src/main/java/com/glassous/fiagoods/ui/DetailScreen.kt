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
import coil.compose.AsyncImage
import com.glassous.fiagoods.data.model.CargoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(item: CargoItem, onBack: () -> Unit) {
    var previewUrl by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
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
                if (item.imageUrls.isNotEmpty()) {
                    IconButton(onClick = {
                        item.imageUrls.forEachIndexed { idx, url ->
                            val name = (item.name.ifBlank { "image" }) + "_" + (idx + 1) + ".jpg"
                            download(url, name)
                        }
                    }) { Icon(Icons.Filled.Download, contentDescription = null) }
                }
            }
        )
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(item.name, style = MaterialTheme.typography.headlineSmall) }
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
            item { FieldCard(label = "类别", value = item.category) }
            item { FieldCard(label = "售价", value = item.price?.let { "¥" + String.format("%.2f", it) } ?: "¥0.00") }
            item { FieldCard(label = "销量", value = item.sold.toString()) }
            item { FieldCard(label = "库存", value = (item.stock ?: 0).toString()) }
            item { FieldCard(label = "简介", value = item.brief) }
            item { FieldCard(label = "描述", value = item.description) }
            item { FieldCard(label = "规格", value = item.specs) }
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