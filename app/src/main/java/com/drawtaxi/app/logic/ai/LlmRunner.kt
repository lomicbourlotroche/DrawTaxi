package com.drawtaxi.app.logic.ai

import android.util.Log

object LlmRunner {

    private const val TAG = "LlmRunner"

    init {
        try {
            System.loadLibrary("llama-jni")
            llamaBackendInit()
            Log.d(TAG, "llama native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load llama native library: ${e.message}")
        }
    }

    // JNI native methods
    external fun llamaBackendInit()
    external fun llamaBackendFree()
    external fun llamaLoadModel(modelPath: String, nCtx: Int): Long
    external fun llamaFreeModel(contextPtr: Long)
    external fun llamaRunInference(contextPtr: Long, prompt: String, maxTokens: Int): String?

    fun run(modelPath: String, prompt: String): String? {
        val ctx = llamaLoadModel(modelPath, nCtx = 512)
        if (ctx == 0L) {
            Log.e(TAG, "Failed to load model: $modelPath")
            return null
        }
        return try {
            Log.d(TAG, "Running local inference with llama.cpp")
            llamaRunInference(ctx, prompt, maxTokens = 256)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            null
        } finally {
            llamaFreeModel(ctx)
        }
    }

    fun cleanup() {
        llamaBackendFree()
    }
}
