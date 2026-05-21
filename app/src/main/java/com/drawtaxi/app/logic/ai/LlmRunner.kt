package com.drawtaxi.app.logic.ai

import android.util.Log
import java.io.File

object LlmRunner {

    private const val TAG = "LlmRunner"

    private var nativeLibraryLoaded = false
    private var _isLoaded = false

    init {
        try {
            System.loadLibrary("llama-jni")
            llamaBackendInit()
            nativeLibraryLoaded = true
            Log.d(TAG, "llama native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load llama native library: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during library init: ${e.message}")
        }
    }

    // JNI native methods
    external fun llamaBackendInit()
    external fun llamaBackendFree()
    external fun llamaLoadModel(modelPath: String, nCtx: Int): Long
    external fun llamaFreeModel(contextPtr: Long)
    external fun llamaRunInference(contextPtr: Long, prompt: String, maxTokens: Int): String?

    fun isLoaded(): Boolean = _isLoaded

    fun isNativeLibraryAvailable(): Boolean = nativeLibraryLoaded

    @Synchronized
    fun run(modelPath: String, prompt: String): String? {
        if (!nativeLibraryLoaded) {
            Log.e(TAG, "Native library not loaded, cannot run inference")
            return null
        }
        if (prompt.isBlank()) {
            Log.e(TAG, "Empty prompt provided")
            return null
        }
        val modelFile = File(modelPath)
        if (!modelFile.exists()) {
            Log.e(TAG, "Model file not found: $modelPath")
            return null
        }
        if (!modelFile.canRead()) {
            Log.e(TAG, "Model file not readable: $modelPath")
            return null
        }

        Log.d(TAG, "Loading model from: $modelPath (${modelFile.length() / 1_000_000} MB)")

        val ctx = llamaLoadModel(modelPath, nCtx = 512)
        if (ctx == 0L) {
            Log.e(TAG, "Failed to load model: $modelPath")
            return null
        }

        _isLoaded = true
        try {
            Log.d(TAG, "Running local inference with llama.cpp, prompt length=${prompt.length}")
            return llamaRunInference(ctx, prompt, maxTokens = 256)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            return null
        } finally {
            try {
                llamaFreeModel(ctx)
            } catch (e: Exception) {
                Log.e(TAG, "Error freeing model: ${e.message}")
            }
            _isLoaded = false
        }
    }

    @Synchronized
    fun cleanup() {
        try {
            if (nativeLibraryLoaded) {
                llamaBackendFree()
                nativeLibraryLoaded = false
            }
            _isLoaded = false
            Log.d(TAG, "Cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
}
