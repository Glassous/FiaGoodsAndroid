package com.glassous.fiagoods.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthScreen(vm: AuthViewModel, onVerified: () -> Unit, message: String? = null) {
    val password by vm.passwordInput.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val verified by vm.verified.collectAsState()
    LaunchedEffect(verified) {
        if (verified) onVerified()
    }
    val ctx = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) { if (error != null) snackbarHostState.showSnackbar(error!!) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, contentWindowInsets = WindowInsets(0)) { innerPadding ->
        // 修改：移除 TopAppBar，将标题放入居中的 Column 中，使其整体垂直居中
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .padding(bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FiaGoods 验证",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (message != null) {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("提示", style = MaterialTheme.typography.titleMedium)
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            var show by androidx.compose.runtime.remember { mutableStateOf(false) }
            OutlinedTextField(
                value = password,
                onValueChange = { vm.updatePasswordInput(it) },
                label = { Text("密码") },
                isError = error != null,
                singleLine = true,
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                supportingText = { if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    IconButton(onClick = { show = !show }) {
                        Icon(imageVector = if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (!loading) {
                Button(onClick = { vm.verify(ctx) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("验证") }
            } else {
                ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        LoadingIndicator(color = LoadingIndicatorDefaults.indicatorColor)
                        Text("正在验证…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}