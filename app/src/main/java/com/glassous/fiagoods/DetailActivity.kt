package com.glassous.fiagoods

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glassous.fiagoods.data.SessionPrefs
import com.glassous.fiagoods.ui.DetailScreen
import com.glassous.fiagoods.ui.GoodsViewModel
import com.glassous.fiagoods.ui.theme.FiaGoodsTheme

class DetailActivity : ComponentActivity() {
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

        val id = intent.getStringExtra("id").orEmpty()
        val itemJson = intent.getStringExtra("item")

        setContent {
            val mode = remember { SessionPrefs.getThemeMode(this) }
            FiaGoodsTheme(
                darkTheme = when (mode) {
                    "system" -> isSystemInDarkTheme()
                    "dark" -> true
                    else -> false
                },
                dynamicColor = true
            ) {
                val vm: GoodsViewModel = viewModel()
                val snackbarHostState = remember { SnackbarHostState() }

                val parsed = remember(itemJson) { itemJson?.let { com.google.gson.Gson().fromJson(it, com.glassous.fiagoods.data.model.CargoItem::class.java) } }
                LaunchedEffect(parsed) {
                    if (parsed != null) {
                        vm.seedItems(listOf(parsed))
                        vm.refresh(this@DetailActivity)
                    } else {
                        vm.refresh(this@DetailActivity)
                    }
                }

                val items by vm.items.collectAsState()
                val item = items.firstOrNull { it.id == id }

                val opMsg by vm.operationMessage.collectAsState()
                LaunchedEffect(opMsg) {
                    val m = opMsg
                    if (m != null) {
                        snackbarHostState.showSnackbar(m)
                        vm.clearOperationMessage()
                    }
                }

                val authMsg by vm.authInvalidMessage.collectAsState()
                LaunchedEffect(authMsg) {
                    if (authMsg != null) {
                        finish()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbarHostState) }, contentWindowInsets = WindowInsets(0)) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        if (item != null) {
                            DetailScreen(
                                item = item,
                                onBack = { finish() },
                                onSave = { targetId, patch, done -> vm.updateItem(this@DetailActivity, targetId, patch) { ok -> done(ok); if (ok) sendBroadcast(android.content.Intent("com.glassous.fiagoods.REFRESH").setPackage(BuildConfig.APPLICATION_ID)) } },
                                onDelete = { targetId, cb -> vm.deleteItem(this@DetailActivity, targetId) { ok -> cb(ok); if (ok) { sendBroadcast(android.content.Intent("com.glassous.fiagoods.REFRESH").setPackage(BuildConfig.APPLICATION_ID)); finish() } } },
                                onAddImage = { uri -> vm.addImage(this@DetailActivity, item.id, uri) { } },
                                onDeleteImage = { url -> vm.deleteImage(this@DetailActivity, item.id, url) { } },
                                onAddImageWithProgress = { uri, onProgress, onDone -> vm.addImageWithProgress(this@DetailActivity, item.id, uri, onProgress, onDone) },
                                onDeleteImageWithProgress = { url, onProgress, onDone -> vm.deleteImageWithProgress(this@DetailActivity, item.id, url, onProgress, onDone) },
                                onAddImageUrlsDirect = { urls, done -> vm.addImageUrlsDirect(this@DetailActivity, item.id, urls) { ok -> done(ok) } },
                                groupOptions = items.flatMap { it.groupNames }.filter { it.isNotBlank() }.distinct().sorted(),
                                categoryOptions = items.flatMap { it.categories }.filter { it.isNotBlank() }.distinct().sorted()
                            )
                        }
                    }
                }
            }
        }
    }
}
