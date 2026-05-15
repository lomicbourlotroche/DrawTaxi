package com.drawtaxi.app.logic

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object LlmRunner {

    private const val TAG = "LlmRunner"
    private const val HF_API_URL = "https://api-inference.huggingface.co/models/microsoft/Phi-3-mini-4k-instruct"
    private const val TIMEOUT_SECONDS = 90L

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun run(modelPath: String, prompt: String): String? {
        return try {
            Log.d(TAG, "Running inference via HuggingFace API (model: $modelPath)")
            runHuggingFaceInference(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "HuggingFace API failed: ${e.message}")
            null
        }
    }

    private fun runHuggingFaceInference(prompt: String): String? {
        val body = JSONObject().apply {
            put("inputs", prompt)
            put("parameters", JSONObject().apply {
                put("max_new_tokens", 512)
                put("temperature", 0.1)
                put("return_full_text", false)
                put("do_sample", true)
            })
        }

        val request = Request.Builder()
            .url(HF_API_URL)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.w(TAG, "HF API error: ${response.code} - $errorBody")

                if (response.code == 503) {
                    Log.w(TAG, "Model is loading, retrying in 20s...")
                    Thread.sleep(20000)
                    return runHuggingFaceInference(prompt)
                }
                return null
            }

            val responseBody = response.body?.string() ?: return null
            Log.d(TAG, "HF API response length: ${responseBody.length}")

            return parseHuggingFaceResponse(responseBody)
        }
    }

    private fun parseHuggingFaceResponse(responseBody: String): String? {
        return try {
            val json = JSONArray(responseBody)
            if (json.length() > 0) {
                val first = json.getJSONObject(0)
                if (first.has("generated_text")) {
                    first.getString("generated_text").trim()
                } else {
                    null
                }
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse HF response: ${e.message}")
            try {
                val json = JSONObject(responseBody)
                if (json.has("error")) {
                    Log.w(TAG, "HF API error: ${json.getString("error")}")
                }
                null
            } catch (e2: Exception) {
                null
            }
        }
    }
}
