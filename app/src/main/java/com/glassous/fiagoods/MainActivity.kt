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
                        composable("auth") {
                            val authVm: AuthViewModel = viewModel()
                            AuthScreen(vm = authVm, onVerified = {
                                navController.navigate("home") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            })
                        }
                        composable("home") {
                            val items by vm.items.collectAsState()
                            val loading by vm.loading.collectAsState()
                            LaunchedEffect(Unit) { vm.refresh(this@MainActivity) }
                            val msg by vm.authInvalidMessage.collectAsState()
                            LaunchedEffect(msg) {
                                if (msg != null) {
                                    snackbarHostState.showSnackbar(msg!!)
                                    navController.navigate("auth") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            }
                            HomeScreen(items = items, loading = loading) { id ->
                                navController.navigate("detail/$id")
                            }
                        }
                        composable(
                            route = "detail/{id}"
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: ""
                            val item = vm.findById(id)
                            if (item != null) {
                                DetailScreen(item)
                            }
                        }
                    }
                }
            }
        }
    }
}