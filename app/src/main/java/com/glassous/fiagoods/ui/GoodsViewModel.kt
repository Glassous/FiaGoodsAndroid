package com.glassous.fiagoods.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.fiagoods.data.SessionPrefs
import com.glassous.fiagoods.data.SupabaseApi
import com.glassous.fiagoods.data.model.CargoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class GoodsViewModel : ViewModel() {
    private val api = SupabaseApi()
    private val _items = MutableStateFlow<List<CargoItem>>(emptyList())
    val items: StateFlow<List<CargoItem>> = _items
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _authInvalidMessage = MutableStateFlow<String?>(null)
    val authInvalidMessage: StateFlow<String?> = _authInvalidMessage

    fun refresh(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            _authInvalidMessage.value = null
            if (!SessionPrefs.isVerified(context)) {
                _items.value = emptyList()
                _loading.value = false
                return@launch
            }
            val remote = withContext(Dispatchers.IO) { api.fetchAppPassword() }
            val stored = SessionPrefs.getPassword(context)
            if (remote == null || stored == null || remote != stored) {
                SessionPrefs.clearVerified(context)
                _authInvalidMessage.value = "密码已修改，请联系管理员"
                _items.value = emptyList()
                _loading.value = false
                return@launch
            }
            val res = withContext(Dispatchers.IO) { api.fetchCargoItems() }
            _items.value = res
            _loading.value = false
        }
    }

    fun findById(id: String): CargoItem? = _items.value.firstOrNull { it.id == id }

    fun clearAuthMessage() { _authInvalidMessage.value = null }

    fun addItem(context: Context, item: CargoItem, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            val created = withContext(Dispatchers.IO) { api.createCargoItem(item) }
            if (created != null) {
                _items.value = _items.value + created
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun updateItem(context: Context, id: String, patch: Map<String, Any?>, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            val updated = withContext(Dispatchers.IO) { api.updateCargoItem(id, patch) }
            if (updated != null) {
                _items.value = _items.value.map { if (it.id == id) updated else it }
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    fun deleteItem(context: Context, id: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            val ok = withContext(Dispatchers.IO) { api.deleteCargoItem(id) }
            if (ok) {
                _items.value = _items.value.filter { it.id != id }
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }
}