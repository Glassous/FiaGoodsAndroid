package com.glassous.fiagoods.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.glassous.fiagoods.BuildConfig
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.DeleteObjectRequest
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.model.PutObjectResult

class OssApi(private val context: Context) {
    private val endpoint = BuildConfig.OSS_ENDPOINT
    private val secureEndpoint: String = run {
        val e = endpoint.trim()
        when {
            e.startsWith("https://") -> e
            e.startsWith("http://") -> "https://" + e.removePrefix("http://")
            e.isNotEmpty() -> "https://" + e
            else -> e
        }
    }
    private val bucket = BuildConfig.OSS_BUCKET
    private val baseUrl = BuildConfig.OSS_PUBLIC_BASE_URL
    private val client by lazy {
        val provider = OSSPlainTextAKSKCredentialProvider(BuildConfig.OSS_ACCESS_KEY_ID, BuildConfig.OSS_ACCESS_KEY_SECRET)
        OSSClient(context, secureEndpoint, provider)
    }

    fun uploadUri(objectKey: String, uri: Uri): String? {
        return try {
            val req = PutObjectRequest(bucket, objectKey, uri)
            client.putObject(req)
            buildUrl(objectKey)
        } catch (e: Exception) {
            null
        }
    }

    fun uploadUriAsync(objectKey: String, uri: Uri, onProgress: (Long, Long) -> Unit, onComplete: (String?, String?) -> Unit) {
        try {
            val req = PutObjectRequest(bucket, objectKey, uri)
            req.progressCallback = OSSProgressCallback<PutObjectRequest> { _, currentSize, totalSize ->
                onProgress(currentSize, totalSize)
            }
            client.asyncPutObject(req, object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                override fun onSuccess(request: PutObjectRequest, result: PutObjectResult) {
                    onComplete(buildUrl(objectKey), null)
                }
                override fun onFailure(request: PutObjectRequest, clientException: ClientException?, serviceException: ServiceException?) {
                    val msg = clientException?.message ?: serviceException?.rawMessage ?: serviceException?.errorCode ?: "上传失败"
                    onComplete(null, msg)
                }
            })
        } catch (e: Exception) {
            onComplete(null, e.message ?: "上传异常")
        }
    }

    fun deleteUrl(url: String): Boolean {
        return try {
            val key = parseKey(url) ?: return false
            val req = DeleteObjectRequest(bucket, key)
            client.deleteObject(req)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun buildKey(prefix: String, fileName: String): String = listOf(prefix.trim('/'), fileName).joinToString("/")

    private fun buildUrl(objectKey: String): String {
        val normalized = if (baseUrl.startsWith("http")) baseUrl.trimEnd('/') else "https://" + baseUrl.trimEnd('/')
        return normalized + "/" + objectKey
    }

    private fun parseKey(url: String): String? {
        val base1 = baseUrl.trimEnd('/') + "/"
        val base2 = if (baseUrl.startsWith("http")) base1 else "https://" + base1
        return when {
            url.startsWith(base1) -> url.removePrefix(base1)
            url.startsWith(base2) -> url.removePrefix(base2)
            else -> null
        }
    }

    private fun readBytes(cr: ContentResolver, uri: Uri): ByteArray? {
        return try {
            cr.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
}
