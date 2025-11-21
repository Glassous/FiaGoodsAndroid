package com.glassous.fiagoods.ui.global

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object UploadState {
    private val _uploading = MutableStateFlow(false)
    private val _progress = MutableStateFlow(0f)
    private val _background = MutableStateFlow(false)
    private val _label = MutableStateFlow<String?>(null)

    val uploading: StateFlow<Boolean> = _uploading
    val progress: StateFlow<Float> = _progress
    val background: StateFlow<Boolean> = _background
    val label: StateFlow<String?> = _label

    fun start(labelText: String? = null) {
        _uploading.value = true
        _progress.value = 0f
        _label.value = labelText
    }

    fun update(p: Float, labelText: String? = null) {
        _progress.value = p.coerceIn(0f, 1f)
        if (labelText != null) _label.value = labelText
    }

    fun setBackground(enabled: Boolean) {
        _background.value = enabled
    }

    fun finish() {
        _uploading.value = false
        _progress.value = 1f
        _background.value = false
        _label.value = null
    }
}

