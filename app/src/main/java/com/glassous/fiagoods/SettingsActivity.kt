package com.glassous.fiagoods

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt
import com.glassous.fiagoods.data.SessionPrefs
import com.glassous.fiagoods.data.UpdateApi
import com.glassous.fiagoods.BuildConfig
import com.glassous.fiagoods.ui.theme.FiaGoodsTheme
import com.glassous.fiagoods.data.model.CargoItem
import com.glassous.fiagoods.data.SupabaseApi
import com.google.gson.Gson

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightNavigationBars = false
        var flags = window.decorView.systemUiVisibility
        flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = flags
        setContent {
            var mode by remember { mutableStateOf(SessionPrefs.getThemeMode(this)) }
            var density by remember { mutableStateOf(SessionPrefs.getCardDensity(this)) }
            FiaGoodsTheme(
                darkTheme = when (mode) {
                    "system" -> isSystemInDarkTheme()
                    "dark" -> true
                    else -> false
                },
                dynamicColor = true
            ) {
                var titleMaxLen by remember { mutableStateOf(SessionPrefs.getTitleMaxLen(this)) }
                SettingsScreen(
                    mode = mode,
                    density = density,
                    titleMaxLen = titleMaxLen,
                    onBack = { finish() },
                    onModeChange = {
                        mode = it
                        SessionPrefs.setThemeMode(this, it)
                    },
                    onDensityChange = {
                        density = it.coerceIn(0, 10)
                        SessionPrefs.setCardDensity(this, density)
                    },
                    onTitleLenChange = {
                        titleMaxLen = it.coerceAtLeast(0)
                        SessionPrefs.setTitleMaxLen(this, titleMaxLen)
                    }
                )
            }
        }
    }
}
// SettingsScreen composable ... (保持原样，省略)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(mode: String, density: Int, titleMaxLen: Int, onBack: () -> Unit, onModeChange: (String) -> Unit, onDensityChange: (Int) -> Unit, onTitleLenChange: (Int) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    var goodsCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val json = SessionPrefs.getItemsCache(ctx)
            if (!json.isNullOrBlank()) {
                try {
                    val list = Gson().fromJson<List<CargoItem>>(json, SupabaseApi.TYPE_LIST_CARGO)
                    goodsCount = list?.size ?: 0
                } catch (_: Exception) { }
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0), snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
            Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp, end = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("主题模式", style = MaterialTheme.typography.titleMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onModeChange("system") }, modifier = Modifier.weight(1f)) {
                                if (mode == "system") { Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(6.dp)) }
                                Text("系统")
                            }
                            Button(onClick = { onModeChange("light") }, modifier = Modifier.weight(1f)) {
                                if (mode == "light") { Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(6.dp)) }
                                Text("浅色")
                            }
                            Button(onClick = { onModeChange("dark") }, modifier = Modifier.weight(1f)) {
                                if (mode == "dark") { Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(6.dp)) }
                                Text("深色")
                            }
                        }
                    }
                }
                ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("主页卡片密度", style = MaterialTheme.typography.titleMedium)
                        var densityDisplay by remember { mutableStateOf(density.toFloat()) }
                        androidx.compose.material3.Slider(
                            value = densityDisplay,
                            onValueChange = { densityDisplay = it.coerceIn(0f, 10f) },
                            onValueChangeFinished = {
                                val v = densityDisplay.roundToInt().coerceIn(0, 10)
                                onDensityChange(v)
                                // 【优化】：移除广播，MainActivity 会通过 ON_RESUME 自动更新
                            },
                            valueRange = 0f..10f,
                            steps = 0
                        )
                        Text(densityDisplay.roundToInt().toString(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
                ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("外显字数长度", style = MaterialTheme.typography.titleMedium)
                        val ctx = LocalContext.current
                        var unlimited by remember { mutableStateOf(titleMaxLen >= Int.MAX_VALUE / 2) }
                        var lastLimitedLen by remember { mutableStateOf(if (unlimited) SessionPrefs.getTitleMaxLenLimited(ctx).coerceIn(0, 50) else titleMaxLen.coerceIn(0, 50)) }
                        var sliderDisplay by remember { mutableStateOf(lastLimitedLen.toFloat()) }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Slider(
                                value = sliderDisplay,
                                onValueChange = { if (!unlimited) sliderDisplay = it.coerceIn(0f, 50f) },
                                onValueChangeFinished = {
                                    if (!unlimited) {
                                        val v = sliderDisplay.roundToInt().coerceIn(0, 50)
                                        lastLimitedLen = v
                                        onTitleLenChange(v)
                                        SessionPrefs.setTitleMaxLenLimited(ctx, v)
                                        // 【优化】：移除广播，MainActivity 会通过 ON_RESUME 自动更新
                                    }
                                },
                                valueRange = 0f..50f,
                                steps = 0,
                                modifier = Modifier.weight(1f),
                                enabled = !unlimited
                            )
                            androidx.compose.material3.IconButton(onClick = {
                                unlimited = !unlimited
                                if (unlimited) {
                                    lastLimitedLen = sliderDisplay.roundToInt().coerceIn(0, 50)
                                    SessionPrefs.setTitleMaxLenLimited(ctx, lastLimitedLen)
                                    onTitleLenChange(Int.MAX_VALUE)
                                } else {
                                    lastLimitedLen = SessionPrefs.getTitleMaxLenLimited(ctx).coerceIn(0, 50)
                                    sliderDisplay = lastLimitedLen.toFloat()
                                    onTitleLenChange(lastLimitedLen)
                                }
                                // 【优化】：移除广播，MainActivity 会通过 ON_RESUME 自动更新
                            }) {
                                androidx.compose.material3.Text("∞", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        androidx.compose.runtime.DisposableEffect(Unit) {
                            onDispose {
                                if (unlimited) {
                                    SessionPrefs.setTitleMaxLenLimited(ctx, lastLimitedLen)
                                } else {
                                    val v = sliderDisplay.roundToInt().coerceIn(0, 50)
                                    SessionPrefs.setTitleMaxLenLimited(ctx, v)
                                }
                            }
                        }
                        Text(if (unlimited) "∞" else sliderDisplay.roundToInt().toString(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
                ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("商品总数", style = MaterialTheme.typography.titleMedium)
                        Text(goodsCount.toString(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
                ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
                    val ctx = LocalContext.current
                    val scope = rememberCoroutineScope()
                    var checking by remember { mutableStateOf(false) }
                    var latest by remember { mutableStateOf<UpdateApi.VersionInfo?>(null) }
                    var error by remember { mutableStateOf<String?>(null) }
                    var errorDetail by remember { mutableStateOf<String?>(null) }
                    val api = remember { UpdateApi() }
                    var showUpdateDialog by remember { mutableStateOf(false) }
                    var showNoUpdateDialog by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("版本更新", style = MaterialTheme.typography.titleMedium)
                        Text("当前版本：" + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                SessionPrefs.clearIgnoredVersions(ctx) // Clear ignored versions on manual check
                                val blank = BuildConfig.APP_VERSION_JSON_URL.trim().isBlank()
                                if (blank) {
                                    error = "APP_VERSION_JSON_URL is blank"
                                    scope.launch { snackbarHostState.showSnackbar(error!!) }
                                } else {
                                    scope.launch {
                                        checking = true
                                        error = null
                                        errorDetail = null
                                        latest = null
                                        val result = api.fetchLatestVersionInfoVerbose()
                                        checking = false
                                        if (result.error != null) {
                                            error = result.error
                                            val code = result.httpCode?.toString() ?: ""
                                            val body = result.bodyPreview ?: ""
                                            val u = result.url
                                            val detail = listOf(
                                                if (code.isNotBlank()) "HTTP Code: " + code else "",
                                                if (u.isNotBlank()) "URL: " + u else "",
                                                if (body.isNotBlank()) "Body preview: " + body else ""
                                            ).filter { it.isNotBlank() }.joinToString("\n")
                                            errorDetail = if (detail.isNotBlank()) detail else null
                                            snackbarHostState.showSnackbar(result.error!!)
                                        } else {
                                            latest = result.info
                                            val hasUpdate = result.info?.versionCode?.let { it > BuildConfig.VERSION_CODE } ?: false
                                            if (hasUpdate) {
                                                showUpdateDialog = true
                                            } else {
                                                showNoUpdateDialog = true
                                            }
                                        }
                                    }
                                }
                            }, modifier = Modifier.weight(1f)) { Text("检查更新") }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("自动检查更新", style = MaterialTheme.typography.bodyMedium)
                            var autoCheck by remember { mutableStateOf(SessionPrefs.isAutoUpdateEnabled(ctx)) }
                            Switch(checked = autoCheck, onCheckedChange = {
                                autoCheck = it
                                SessionPrefs.setAutoUpdateEnabled(ctx, it)
                            })
                        }
                        if (checking) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        if (error != null) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            if (errorDetail != null) {
                                Text(errorDetail!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (showUpdateDialog && latest != null) {
                            val l = latest!!
                            AlertDialog(
                                onDismissRequest = { showUpdateDialog = false },
                                title = { Text("有新版本：" + l.versionName) },
                                text = {
                                    val notes = l.releaseNotes ?: emptyList()
                                    val content = if (notes.isNotEmpty()) notes.joinToString("\n") else ""
                                    Text(content)
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val baseBlank = BuildConfig.APP_DOWNLOAD_BASE_URL.trim().isBlank()
                                        if (baseBlank) {
                                            error = "未配置下载基地址"
                                            scope.launch { snackbarHostState.showSnackbar(error!!) }
                                        } else {
                                            try {
                                                val url = api.buildApkDownloadUrl(l)
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                ctx.startActivity(intent)
                                            } catch (e: Exception) {
                                                error = "无法打开下载链接"
                                                scope.launch { snackbarHostState.showSnackbar(error!!) }
                                            }
                                        }
                                    }) { Text("下载") }
                                },
                                dismissButton = { TextButton(onClick = { showUpdateDialog = false }) { Text("取消") } }
                            )
                        }
                        if (showNoUpdateDialog) {
                            AlertDialog(
                                onDismissRequest = { showNoUpdateDialog = false },
                                title = { Text("当前已是最新版本") },
                                text = {},
                                confirmButton = {
                                    TextButton(onClick = { showNoUpdateDialog = false }) { Text("确定") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}