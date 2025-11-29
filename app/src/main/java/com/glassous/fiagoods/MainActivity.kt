package com.glassous.fiagoods

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.glassous.fiagoods.ui.theme.FiaGoodsTheme
import com.glassous.fiagoods.ui.GoodsViewModel
import com.glassous.fiagoods.ui.HomeScreen
import com.glassous.fiagoods.data.SessionPrefs
import com.glassous.fiagoods.ui.AuthScreen
import com.glassous.fiagoods.ui.AuthViewModel
import com.glassous.fiagoods.data.UpdateApi
import com.glassous.fiagoods.BuildConfig
import com.glassous.fiagoods.ui.components.AppDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.isSystemInDarkTheme
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Context
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Cache
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
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
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache(
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .strongReferencesEnabled(true)
                    .build()
            )
            .diskCache(
                DiskCache.Builder()
                    .directory(java.io.File(cacheDir, "image_cache"))
                    .maxSizeBytes(200L * 1024 * 1024)
                    .build()
            )
            .respectCacheHeaders(false)
            .okHttpClient {
                OkHttpClient.Builder()
                    .cache(Cache(java.io.File(cacheDir, "okhttp-cache"), 50L * 1024 * 1024))
                    .build()
            }
            .crossfade(true)
            .build()

        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            var themeMode by remember { mutableStateOf(SessionPrefs.getThemeMode(this)) }
            var cardDensity by remember { mutableStateOf(SessionPrefs.getCardDensity(this)) }
            var titleMaxLen by remember { mutableStateOf(SessionPrefs.getTitleMaxLen(this)) }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        // 【优化点】：只有值真正改变时才更新 State，防止从设置页返回时触发不必要的重组（看起来像刷新）
                        val newMode = SessionPrefs.getThemeMode(this@MainActivity)
                        if (themeMode != newMode) themeMode = newMode

                        val newDensity = SessionPrefs.getCardDensity(this@MainActivity)
                        if (cardDensity != newDensity) cardDensity = newDensity

                        val newLen = SessionPrefs.getTitleMaxLen(this@MainActivity)
                        if (titleMaxLen != newLen) titleMaxLen = newLen
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                        // 广播接收时也做同样的优化
                        val newMode = SessionPrefs.getThemeMode(this@MainActivity)
                        if (themeMode != newMode) themeMode = newMode

                        val newDensity = SessionPrefs.getCardDensity(this@MainActivity)
                        if (cardDensity != newDensity) cardDensity = newDensity

                        val newLen = SessionPrefs.getTitleMaxLen(this@MainActivity)
                        if (titleMaxLen != newLen) titleMaxLen = newLen
                    }
                }
                registerReceiver(receiver, IntentFilter("com.glassous.fiagoods.REFRESH"), Context.RECEIVER_NOT_EXPORTED)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    unregisterReceiver(receiver)
                }
            }
            val isDark = when (themeMode) {
                "system" -> isSystemInDarkTheme()
                "dark" -> true
                else -> false
            }
            FiaGoodsTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val vm: GoodsViewModel = viewModel()
                val snackbarHostState = remember { SnackbarHostState() }
                val startDest = if (SessionPrefs.isVerified(this)) "home" else "auth?msg="
                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbarHostState) }, contentWindowInsets = WindowInsets(0)) { innerPadding ->
                        NavHost(navController = navController, startDestination = startDest, modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            composable(
                                route = "auth?msg={msg}",
                                arguments = listOf(navArgument("msg") { type = NavType.StringType; defaultValue = "" })
                            ) { backStackEntry ->
                                val authVm: AuthViewModel = viewModel()
                                val msg = backStackEntry.arguments?.getString("msg").orEmpty()
                                val displayMsg = msg.takeIf { it.isNotBlank() }
                                val updateApi = remember { UpdateApi() }
                                var showUpdateDialog by remember { mutableStateOf(false) }
                                var latest by remember { mutableStateOf<UpdateApi.VersionInfo?>(null) }
                                LaunchedEffect(Unit) {
                                    val enabled = SessionPrefs.isAutoUpdateEnabled(this@MainActivity)
                                    if (enabled) {
                                        val res = updateApi.fetchLatestVersionInfoVerbose()
                                        val info = res.info
                                        val hasUpdate = info?.versionCode?.let { it > BuildConfig.VERSION_CODE } ?: false
                                        if (hasUpdate) { latest = info; showUpdateDialog = true }
                                    }
                                }
                                AuthScreen(vm = authVm, onVerified = {
                                    navController.navigate("home") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }, message = displayMsg)
                                if (showUpdateDialog && latest != null) {
                                    val l = latest!!
                                    val notes = l.releaseNotes ?: emptyList()
                                    val content = if (notes.isNotEmpty()) notes.joinToString("\n") else ""
                                    AppDialog(onDismiss = { showUpdateDialog = false }, title = "有新版本：" + l.versionName, content = {
                                        androidx.compose.material3.Text(content)
                                    }, actions = {
                                        TextButton(onClick = {
                                            val baseBlank = BuildConfig.APP_DOWNLOAD_BASE_URL.trim().isBlank()
                                            if (!baseBlank) {
                                                try {
                                                    val url = updateApi.buildApkDownloadUrl(l)
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                    startActivity(intent)
                                                } catch (_: Exception) { }
                                            }
                                        }) { androidx.compose.material3.Text("下载") }
                                        TextButton(onClick = { showUpdateDialog = false }) { androidx.compose.material3.Text("取消") }
                                    })
                                }
                            }
                            composable("home") {
                                val items by vm.items.collectAsState()
                                val loading by vm.loading.collectAsState()
                                LaunchedEffect(Unit) { vm.loadCache(this@MainActivity) }

                                LaunchedEffect(Unit) { vm.refresh(this@MainActivity, verifyPassword = true) }

                                val updateApi = remember { UpdateApi() }
                                var showUpdateDialog by remember { mutableStateOf(false) }
                                var latest by remember { mutableStateOf<UpdateApi.VersionInfo?>(null) }
                                LaunchedEffect(Unit) {
                                    val enabled = SessionPrefs.isAutoUpdateEnabled(this@MainActivity)
                                    if (enabled) {
                                        val res = updateApi.fetchLatestVersionInfoVerbose()
                                        val info = res.info
                                        val hasUpdate = info?.versionCode?.let { it > BuildConfig.VERSION_CODE } ?: false
                                        if (hasUpdate) { latest = info; showUpdateDialog = true }
                                    }
                                }
                                DisposableEffect(Unit) {
                                    val receiver = object : BroadcastReceiver() {
                                        override fun onReceive(context: Context, intent: android.content.Intent) {
                                            vm.refresh(this@MainActivity, clearBeforeLoad = true, verifyPassword = false)
                                        }
                                    }
                                    registerReceiver(receiver, IntentFilter("com.glassous.fiagoods.REFRESH"), Context.RECEIVER_NOT_EXPORTED)
                                    onDispose { unregisterReceiver(receiver) }
                                }
                                val msg by vm.authInvalidMessage.collectAsState()
                                val opMsg by vm.operationMessage.collectAsState()
                                LaunchedEffect(msg) {
                                    if (msg != null) {
                                        val encoded = Uri.encode(msg!!)
                                        vm.clearAuthMessage()
                                        navController.navigate("auth?msg=$encoded") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                }
                                LaunchedEffect(opMsg) {
                                    val m = opMsg
                                    if (m != null) {
                                        val suppressed = setOf("图片上传成功", "图片URL添加成功", "新增商品成功")
                                        if (!suppressed.contains(m)) {
                                            snackbarHostState.showSnackbar(m)
                                        }
                                        vm.clearOperationMessage()
                                    }
                                }
                                val favorites by vm.favorites.collectAsState()
                                HomeScreen(items = items, loading = loading, onItemClick = { item ->
                                    val json = com.google.gson.Gson().toJson(item)
                                    startActivity(android.content.Intent(this@MainActivity, DetailActivity::class.java).putExtra("id", item.id).putExtra("item", json))
                                }, favorites = favorites, onToggleFavorite = { id ->
                                    vm.toggleFavorite(this@MainActivity, id) { }
                                }, onCreateItemWithImagesAndUrls = { item, uris, urls, onProgress, onDone ->
                                    vm.addItem(this@MainActivity, item) { ok, created ->
                                        if (!ok) {
                                            onDone(false)
                                        } else {
                                            val targetId = (created?.id) ?: item.id
                                            vm.addImageUrlsDirect(this@MainActivity, targetId, urls) { okUrls ->
                                                val total = uris.size
                                                if (total == 0) {
                                                    onDone(okUrls)
                                                } else {
                                                    var uploaded = 0
                                                    fun uploadNext() {
                                                        val u = uris[uploaded]
                                                        vm.addImage(this@MainActivity, targetId, u) { ok2 ->
                                                            if (!ok2) {
                                                                onDone(false)
                                                            } else {
                                                                uploaded++
                                                                onProgress(uploaded, total)
                                                                if (uploaded >= total) { onDone(true) } else { uploadNext() }
                                                            }
                                                        }
                                                    }
                                                    onProgress(0, total)
                                                    uploadNext()
                                                }
                                            }
                                        }
                                    }
                                }, onAddImageUrlsDirect = { id, urls, done ->
                                    vm.addImageUrlsDirect(this@MainActivity, id, urls) { ok -> done(ok) }
                                }, columnsPerRow = cardDensity,
                                    titleMaxLen = titleMaxLen, // 【修改点】：传入 titleMaxLen
                                    onRefresh = { vm.refresh(this@MainActivity, clearBeforeLoad = true, verifyPassword = false) })
                                if (showUpdateDialog && latest != null) {
                                    val l = latest!!
                                    val notes = l.releaseNotes ?: emptyList()
                                    val content = if (notes.isNotEmpty()) notes.joinToString("\n") else ""
                                    AppDialog(onDismiss = { showUpdateDialog = false }, title = "有新版本：" + l.versionName, content = {
                                        androidx.compose.material3.Text(content)
                                    }, actions = {
                                        TextButton(onClick = {
                                            val baseBlank = BuildConfig.APP_DOWNLOAD_BASE_URL.trim().isBlank()
                                            if (!baseBlank) {
                                                try {
                                                    val url = updateApi.buildApkDownloadUrl(l)
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                    startActivity(intent)
                                                } catch (_: Exception) { }
                                            }
                                        }) { androidx.compose.material3.Text("下载") }
                                        TextButton(onClick = { showUpdateDialog = false }) { androidx.compose.material3.Text("取消") }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}