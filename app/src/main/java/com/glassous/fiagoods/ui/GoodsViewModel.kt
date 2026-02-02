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
import android.net.Uri
import com.glassous.fiagoods.data.OssApi
import com.glassous.fiagoods.BuildConfig
import com.google.gson.Gson

class GoodsViewModel : ViewModel() {
    private val api = SupabaseApi()
    private val gson = Gson()
    private val _items = MutableStateFlow<List<CargoItem>>(emptyList())
    val items: StateFlow<List<CargoItem>> = _items
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _authInvalidMessage = MutableStateFlow<String?>(null)
    val authInvalidMessage: StateFlow<String?> = _authInvalidMessage
    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    fun refresh(context: Context, clearBeforeLoad: Boolean = false, verifyPassword: Boolean = false) {
        viewModelScope.launch {
            if (clearBeforeLoad) { _items.value = emptyList() }
            _loading.value = true
            _authInvalidMessage.value = null

            if (!SessionPrefs.isVerified(context)) {
                _items.value = emptyList()
                _loading.value = false
                return@launch
            }

            if (verifyPassword) {
                try {
                    val remote = withContext(Dispatchers.IO) { api.fetchAppPassword() }
                    val stored = SessionPrefs.getPassword(context)
                    if (remote != stored) {
                        SessionPrefs.clearVerified(context)
                        _authInvalidMessage.value = "密码已修改，请联系管理员"
                        _items.value = emptyList()
                        _loading.value = false
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore check error
                }
            }

            try {
                val ossCfg = withContext(Dispatchers.IO) { api.fetchOssConfig() }
                SessionPrefs.setOssConfig(
                    context,
                    ossCfg["oss_endpoint"],
                    ossCfg["oss_bucket"],
                    ossCfg["oss_access_key_id"],
                    ossCfg["oss_access_key_secret"],
                    ossCfg["oss_public_base_url"]
                )

                val res = withContext(Dispatchers.IO) { api.fetchCargoItems() }
                val fixed = res.map { it.copy(groupNames = it.groupNames ?: emptyList(), categories = it.categories ?: emptyList()) }
                _items.value = fixed
                _favorites.value = fixed.filter { it.isFavorite }.map { it.id }.toSet()

                try {
                    val json = gson.toJson(fixed)
                    SessionPrefs.setItemsCache(context, json)
                } catch (_: Exception) { }
            } catch (e: Exception) {
                // error
            } finally {
                _loading.value = false
            }
        }
    }

    // ... 其他中间方法保持不变，此处省略以节省篇幅，请直接保留原有的 addItem, updateItem, deleteItem 等方法 ...

    fun findById(id: String): CargoItem? = _items.value.firstOrNull { it.id == id }
    fun clearAuthMessage() { _authInvalidMessage.value = null }
    fun clearOperationMessage() { _operationMessage.value = null }

    fun addItem(context: Context, item: CargoItem, onDone: (Boolean, CargoItem?) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false, null); return@launch }
            try {
                val created = withContext(Dispatchers.IO) { api.createCargoItem(item) }
                if (created != null) {
                    _items.value = _items.value + created
                    _operationMessage.value = "新增商品成功"
                    onDone(true, created)
                } else {
                    _operationMessage.value = "新增商品失败"
                    onDone(false, null)
                }
            } catch (e: Exception) {
                _operationMessage.value = "新增商品异常"
                onDone(false, null)
            }
        }
    }

    fun updateItem(context: Context, id: String, patch: Map<String, Any?>, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            try {
                val updated = withContext(Dispatchers.IO) { api.updateCargoItem(id, patch) }
                if (updated != null) {
                    _items.value = _items.value.map { if (it.id == id) updated else it }
                    _operationMessage.value = "保存成功"
                    onDone(true)
                } else {
                    _operationMessage.value = "保存失败"
                    onDone(false)
                }
            } catch (e: Exception) {
                _operationMessage.value = "保存异常"
                onDone(false)
            }
        }
    }

    fun deleteItem(context: Context, id: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            try {
                val ok = withContext(Dispatchers.IO) { api.deleteCargoItem(id) }
                if (ok) {
                    _items.value = _items.value.filter { it.id != id }
                    _operationMessage.value = "删除成功"
                    onDone(true)
                } else {
                    _operationMessage.value = "删除失败"
                    onDone(false)
                }
            } catch (e: Exception) {
                _operationMessage.value = "删除异常"
                onDone(false)
            }
        }
    }

    fun addImage(context: Context, id: String, uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            val item = findById(id) ?: run { onDone(false); return@launch }
            if (!hasOssConfig(context)) {
                _operationMessage.value = "OSS 配置缺失"
                onDone(false)
                return@launch
            }
            try {
                val oss = OssApi(context)
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when (mime.lowercase()) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val fileName = System.currentTimeMillis().toString() + "." + ext
                val key = oss.buildKey("cargo/$id", fileName)
                val url = withContext(Dispatchers.IO) { oss.uploadUri(key, uri) }
                if (url != null) {
                    val newList = item.imageUrls + url
                    val updated = withContext(Dispatchers.IO) { api.updateCargoItem(id, mapOf("image_urls" to newList)) }
                    if (updated != null) {
                        _items.value = _items.value.map { if (it.id == id) updated else it }
                        _operationMessage.value = "图片上传成功"
                        onDone(true)
                    } else {
                        _operationMessage.value = "图片保存失败"
                        onDone(false)
                    }
                } else {
                    _operationMessage.value = "图片上传失败"
                    onDone(false)
                }
            } catch (e: Exception) {
                _operationMessage.value = "图片上传异常"
                onDone(false)
            }
        }
    }

    fun addImages(context: Context, id: String, uris: List<Uri>, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            var item = findById(id) ?: run { onDone(false); return@launch }
            if (!hasOssConfig(context)) {
                _operationMessage.value = "OSS 配置缺失"
                onDone(false)
                return@launch
            }
            try {
                val oss = OssApi(context)
                for (uri in uris) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val ext = when (mime.lowercase()) {
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        else -> "jpg"
                    }
                    val fileName = System.currentTimeMillis().toString() + "." + ext
                    val key = oss.buildKey("cargo/$id", fileName)
                    val url = withContext(Dispatchers.IO) { oss.uploadUri(key, uri) }
                    if (url == null) { _operationMessage.value = "图片上传失败"; onDone(false); return@launch }
                    val newList = item.imageUrls + url
                    val updated = withContext(Dispatchers.IO) { api.updateCargoItem(id, mapOf("image_urls" to newList)) }
                    if (updated == null) { _operationMessage.value = "图片保存失败"; onDone(false); return@launch }
                    _items.value = _items.value.map { if (it.id == id) updated else it }
                    item = updated
                }
                _operationMessage.value = "图片上传成功"
                onDone(true)
            } catch (e: Exception) {
                _operationMessage.value = "图片上传异常"
                onDone(false)
            }
        }
    }

    fun addImageUrlDirect(context: Context, id: String, url: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            val item = findById(id) ?: run { onDone(false); return@launch }
            val normalized = url.trim()
            if (normalized.isBlank()) { onDone(false); return@launch }
            try {
                val newList = item.imageUrls + normalized
                val updated = withContext(Dispatchers.IO) { api.updateCargoItem(id, mapOf("image_urls" to newList)) }
                if (updated != null) {
                    _items.value = _items.value.map { if (it.id == id) updated else it }
                    _operationMessage.value = "图片URL添加成功"
                    onDone(true)
                } else {
                    _operationMessage.value = "图片保存失败"
                    onDone(false)
                }
            } catch (e: Exception) {
                _operationMessage.value = "图片保存异常"
                onDone(false)
            }
        }
    }

    fun addImageUrlsDirect(context: Context, id: String, urls: List<String>, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            var item = findById(id) ?: run { onDone(false); return@launch }
            val list = urls.map { it.trim() }.filter { it.isNotBlank() }
            if (list.isEmpty()) { onDone(true); return@launch }
            try {
                var current = item
                for (u in list) {
                    val newList = current.imageUrls + u
                    val updated = withContext(Dispatchers.IO) { api.updateCargoItem(id, mapOf("image_urls" to newList)) }
                    if (updated == null) { _operationMessage.value = "图片保存失败"; onDone(false); return@launch }
                    _items.value = _items.value.map { if (it.id == id) updated else it }
                    current = updated
                }
                _operationMessage.value = "图片URL添加成功"
                onDone(true)
            } catch (e: Exception) {
                _operationMessage.value = "图片保存异常"
                onDone(false)
            }
        }
    }

    fun addImageWithProgress(context: Context, id: String, uri: Uri, onProgress: (Long, Long) -> Unit, onDone: (Boolean, String?) -> Unit) {
        if (!SessionPrefs.isVerified(context)) { onDone(false, "未验证") ; return }
        val item = findById(id) ?: run { onDone(false, "商品不存在"); return }
        if (!hasOssConfig(context)) {
            onDone(false, "OSS 配置缺失")
            return
        }
        val oss = OssApi(context)
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mime.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val fileName = System.currentTimeMillis().toString() + "." + ext
        val key = oss.buildKey("cargo/$id", fileName)
        oss.uploadUriAsync(key, uri, onProgress) { url, error ->
            if (url != null) {
                viewModelScope.launch {
                    val newList = item.imageUrls + url
                    val updated = withContext(Dispatchers.IO) { api.updateCargoItem(id, mapOf("image_urls" to newList)) }
                    if (updated != null) {
                        _items.value = _items.value.map { if (it.id == id) updated else it }
                        onDone(true, null)
                    } else {
                        onDone(false, "图片保存失败")
                    }
                }
            } else {
                onDone(false, error ?: "上传失败")
            }
        }
    }

    fun replaceImage(context: Context, id: String, oldUrl: String, uri: Uri, onDone: (Boolean) -> Unit) {
        // 移除修改图片功能
    }

    fun deleteImageWithProgress(context: Context, id: String, url: String, onProgress: (Float) -> Unit, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false, "未验证"); return@launch }
            val item = findById(id) ?: run { onDone(false, "商品不存在"); return@launch }
            if (!hasOssConfig(context)) {
                onDone(false, "OSS 配置缺失")
                return@launch
            }
            try {
                val oss = OssApi(context)
                onProgress(0.3f)
                val okDel = withContext(Dispatchers.IO) { oss.deleteUrl(url) }
                if (!okDel) { onDone(false, "删除 OSS 对象失败"); return@launch }
                onProgress(0.6f)
                val newList = item.imageUrls.filter { it != url }
                val updated = withContext(Dispatchers.IO) { api.updateCargoItem(id, mapOf("image_urls" to newList)) }
                if (updated != null) {
                    _items.value = _items.value.map { if (it.id == id) updated else it }
                    onProgress(1.0f)
                    onDone(true, null)
                } else {
                    onDone(false, "更新数据库失败")
                }
            } catch (e: Exception) {
                onDone(false, e.message ?: "删除异常")
            }
        }
    }

    fun deleteImage(context: Context, id: String, url: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            val item = findById(id) ?: run { onDone(false); return@launch }
            if (!hasOssConfig(context)) {
                _operationMessage.value = "OSS 配置缺失"
                onDone(false)
                return@launch
            }
            try {
                val oss = OssApi(context)
                val okDel = withContext(Dispatchers.IO) { oss.deleteUrl(url) }
                if (!okDel) { _operationMessage.value = "图片删除失败"; onDone(false); return@launch }
                val newList = item.imageUrls.filter { it != url }
                val updated = withContext(Dispatchers.IO) { api.updateCargoItem(id, mapOf("image_urls" to newList)) }
                if (updated != null) {
                    _items.value = _items.value.map { if (it.id == id) updated else it }
                    _operationMessage.value = "图片删除成功"
                    onDone(true)
                } else {
                    _operationMessage.value = "图片保存失败"
                    onDone(false)
                }
            } catch (e: Exception) {
                _operationMessage.value = "图片删除异常"
                onDone(false)
            }
        }
    }

    fun toggleFavorite(context: Context, id: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!SessionPrefs.isVerified(context)) { onDone(false); return@launch }
            val isFav = _favorites.value.contains(id)
            val newFavState = !isFav
            
            // 【乐观更新】：立即更新 UI
            _items.value = _items.value.map { 
                if (it.id == id) it.copy(isFavorite = newFavState) else it 
            }
            _favorites.value = if (newFavState) {
                _favorites.value + id
            } else {
                _favorites.value - id
            }
            onDone(true)
            
            // 后台同步到服务器
            try {
                val updated = withContext(Dispatchers.IO) { 
                    api.updateCargoItem(id, mapOf("is_favorite" to newFavState)) 
                }
                if (updated == null) {
                    // 服务器更新失败，回滚 UI
                    _items.value = _items.value.map { 
                        if (it.id == id) it.copy(isFavorite = isFav) else it 
                    }
                    _favorites.value = if (isFav) {
                        _favorites.value + id
                    } else {
                        _favorites.value - id
                    }
                    _operationMessage.value = if (isFav) "取消收藏失败" else "收藏失败"
                }
            } catch (e: Exception) {
                // 网络异常，回滚 UI
                _items.value = _items.value.map { 
                    if (it.id == id) it.copy(isFavorite = isFav) else it 
                }
                _favorites.value = if (isFav) {
                    _favorites.value + id
                } else {
                    _favorites.value - id
                }
                _operationMessage.value = if (isFav) "取消收藏异常" else "收藏异常"
            }
        }
    }

    fun seedItems(items: List<CargoItem>) {
        val fixed = items.map { it.copy(groupNames = it.groupNames ?: emptyList(), categories = it.categories ?: emptyList()) }
        _items.value = fixed
        _favorites.value = fixed.filter { it.isFavorite }.map { it.id }.toSet()
    }

    private fun hasOssConfig(context: Context): Boolean {
        val e = SessionPrefs.getOssEndpoint(context) ?: BuildConfig.OSS_ENDPOINT
        val b = SessionPrefs.getOssBucket(context) ?: BuildConfig.OSS_BUCKET
        val id = SessionPrefs.getOssAccessKeyId(context) ?: BuildConfig.OSS_ACCESS_KEY_ID
        val s = SessionPrefs.getOssAccessKeySecret(context) ?: BuildConfig.OSS_ACCESS_KEY_SECRET
        val u = SessionPrefs.getOssPublicBaseUrl(context) ?: BuildConfig.OSS_PUBLIC_BASE_URL
        return listOf(e, b, id, s, u).all { it.isNotBlank() }
    }

    fun loadCache(context: Context) {
        val json = SessionPrefs.getItemsCache(context)
        if (!json.isNullOrBlank()) {
            try {
                // 【核心修改】：替换匿名内部类，使用 SupabaseApi.TYPE_LIST_CARGO
                val list = gson.fromJson<List<CargoItem>>(json, SupabaseApi.TYPE_LIST_CARGO) ?: emptyList()
                val fixed = list.map { it.copy(groupNames = it.groupNames ?: emptyList(), categories = it.categories ?: emptyList()) }
                if (fixed.isNotEmpty()) {
                    _items.value = fixed
                    _favorites.value = fixed.filter { it.isFavorite }.map { it.id }.toSet()
                }
            } catch (_: Exception) { }
        }
    }
}