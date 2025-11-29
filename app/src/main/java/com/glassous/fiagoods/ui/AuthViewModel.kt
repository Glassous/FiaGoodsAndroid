package com.glassous.fiagoods.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.fiagoods.data.SessionPrefs
import com.glassous.fiagoods.data.SupabaseApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val api = SupabaseApi()
    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _verified = MutableStateFlow(false)
    val verified: StateFlow<Boolean> = _verified

    fun updatePasswordInput(s: String) { _passwordInput.value = s }

    fun verify(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // fetchAppPassword 可能会抛出异常，也可能返回 String
                val remote = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    api.fetchAppPassword()
                }

                val input = _passwordInput.value
                if (remote == input) {
                    SessionPrefs.setVerified(context, input)
                    _verified.value = true
                } else {
                    SessionPrefs.clearVerified(context)
                    _error.value = "密码不匹配 (远程长度:${remote.length})"
                    _verified.value = false
                }
            } catch (e: Exception) {
                // 【修改点】：直接显示具体的异常信息，而不是 "网络错误"
                // 这样如果是 Gson 错误，会显示 "Parse Error: ..."
                // 如果是配置错误，会显示 "Config Error: ..."
                _error.value = "错误: ${e.message}"
                e.printStackTrace() // 在 Logcat 中打印堆栈
                _verified.value = false
            } finally {
                _loading.value = false
            }
        }
    }
}