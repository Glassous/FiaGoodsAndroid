package com.glassous.fiagoods

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.glassous.fiagoods.ui.theme.FiaGoodsTheme
import com.glassous.fiagoods.ui.DetailScreen
import com.glassous.fiagoods.ui.GoodsViewModel
import com.glassous.fiagoods.ui.HomeScreen
import com.glassous.fiagoods.data.SessionPrefs
import com.glassous.fiagoods.ui.AuthScreen
import com.glassous.fiagoods.ui.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FiaGoodsTheme {
                val navController = rememberNavController()
                val vm: GoodsViewModel = viewModel()
                val snackbarHostState = remember { SnackbarHostState() }
                val startDest = if (SessionPrefs.isVerified(this)) "home" else "auth"
                Scaffold(modifier = Modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                    NavHost(navController = navController, startDestination = startDest, modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        composable(
                            route = "auth?msg={msg}",
                            arguments = listOf(navArgument("msg") { type = NavType.StringType; defaultValue = "" })
                        ) { backStackEntry ->
                            val authVm: AuthViewModel = viewModel()
                            val msg = backStackEntry.arguments?.getString("msg").orEmpty()
                            val displayMsg = msg.takeIf { it.isNotBlank() }
                            AuthScreen(vm = authVm, onVerified = {
                                navController.navigate("home") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }, message = displayMsg)
                        }
                        composable("home") {
                            val items by vm.items.collectAsState()
                            val loading by vm.loading.collectAsState()
                            LaunchedEffect(Unit) { vm.refresh(this@MainActivity) }
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
                                if (opMsg != null) {
                                    snackbarHostState.showSnackbar(opMsg!!)
                                    vm.clearOperationMessage()
                                }
                            }
                            HomeScreen(items = items, loading = loading, onItemClick = { id ->
                                navController.navigate("detail/$id")
                            }, onAddItem = { item ->
                                vm.addItem(this@MainActivity, item) { }
                            })
                        }
                        composable(
                            route = "detail/{id}"
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: ""
                        val items by vm.items.collectAsState()
                        val item = items.firstOrNull { it.id == id }
                            if (item != null) {
                                DetailScreen(
                                    item = item,
                                    onBack = { navController.popBackStack() },
                                    onSave = { id, patch -> vm.updateItem(this@MainActivity, id, patch) { } },
                                    onDelete = { id, cb -> vm.deleteItem(this@MainActivity, id) { ok -> cb(ok) } },
                                    onAddImage = { uri -> vm.addImage(this@MainActivity, item.id, uri) { } },
                                    onDeleteImage = { url -> vm.deleteImage(this@MainActivity, item.id, url) { } },
                                    onAddImageWithProgress = { uri, onProgress, onDone -> vm.addImageWithProgress(this@MainActivity, item.id, uri, onProgress, onDone) },
                                    onDeleteImageWithProgress = { url, onProgress, onDone -> vm.deleteImageWithProgress(this@MainActivity, item.id, url, onProgress, onDone) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
