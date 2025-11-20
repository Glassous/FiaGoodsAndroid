package com.glassous.fiagoods.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TopAppBar(title = { Text("FiaGoods 验证") })
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (message != null) { Text(message, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(value = password, onValueChange = { vm.updatePasswordInput(it) }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), isError = error != null)
                Button(onClick = { vm.verify(ctx) }, enabled = !loading) { Text("验证") }
                if (error != null) { Text(error ?: "", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}