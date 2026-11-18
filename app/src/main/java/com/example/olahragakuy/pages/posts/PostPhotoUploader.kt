package com.example.olahragakuy.pages.posts

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.olahragakuy.R

object PostPhotoUploader {
    private var isInitialized = false

    private fun initIfNeeded(context: Context) {
        if (!isInitialized) {
            try {
                val cloudName = context.getString(R.string.cloudinary_cloud_name)
                val config = HashMap<String, Any>()
                config["cloud_name"] = cloudName
                MediaManager.init(context, config)
                isInitialized = true
            } catch (_: Exception) {
                isInitialized = true
            }
        }
    }

    fun upload(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onWaiting: () -> Unit
    ) {
        try {
            initIfNeeded(context.applicationContext)
            val uploadPreset = context.getString(R.string.cloudinary_upload_preset)
            val uniquePublicId = "post_${System.currentTimeMillis()}_${(1000..9999).random()}"

            MediaManager.get().upload(uri)
                .unsigned(uploadPreset)
                .option("public_id", uniquePublicId)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String ?: resultData["url"] as? String
                        if (url != null) {
                            onSuccess(url)
                        } else {
                            onError("Gagal mendapatkan URL foto dari hasil upload.")
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        onError("Upload gagal: ${error.description}")
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        onWaiting()
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            onError("Gagal mengunggah foto: ${e.localizedMessage}")
        }
    }
}
