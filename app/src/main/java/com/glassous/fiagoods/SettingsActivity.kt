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
import androidx.compose.runtime.Composable
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
import com.glassous.fiagoods.data.SessionPrefs
import com.glassous.fiagoods.ui.theme.FiaGoodsTheme

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
                SettingsScreen(
                    mode = mode,
                    density = density,
                    onBack = { finish() },
                    onModeChange = {
                        mode = it
                        SessionPrefs.setThemeMode(this, it)
                    },
                    onDensityChange = {
                        density = it.coerceIn(0, 10)
                        SessionPrefs.setCardDensity(this, density)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(mode: String, density: Int, onBack: () -> Unit, onModeChange: (String) -> Unit, onDensityChange: (Int) -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = WindowInsets(0)) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
            Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { onDensityChange((density - 1).coerceAtLeast(0)) }, modifier = Modifier.weight(1f)) { Text("-") }
                            Text(density.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Button(onClick = { onDensityChange((density + 1).coerceAtMost(10)) }, modifier = Modifier.weight(1f)) { Text("+") }
                        }
                    }
                }
            }
        }
    }
}
