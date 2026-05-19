#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#include "llama.h"

#define LOG_TAG "LlamaJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_backend_initialized = false;

struct LlamaSession {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_sampler* sampler = nullptr;
    std::vector<llama_token> tokens;
};

extern "C" JNIEXPORT void JNICALL
Java_com_drawtaxi_app_logic_ai_LlmRunner_llamaBackendInit(JNIEnv* env, jobject thiz) {
    if (!g_backend_initialized) {
        llama_backend_init();
        g_backend_initialized = true;
        LOGD("llama backend initialized");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_drawtaxi_app_logic_ai_LlmRunner_llamaBackendFree(JNIEnv* env, jobject thiz) {
    if (g_backend_initialized) {
        llama_backend_free();
        g_backend_initialized = false;
        LOGD("llama backend freed");
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_drawtaxi_app_logic_ai_LlmRunner_llamaLoadModel(JNIEnv* env, jobject thiz, jstring modelPath, jint nCtx) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    auto model_params = llama_model_default_params();
    auto* model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("Failed to load model: %s", path);
        return 0;
    }

    auto ctx_params = llama_context_default_params();
    ctx_params.n_ctx = nCtx;
    ctx_params.n_batch = nCtx;
    ctx_params.n_threads = 4;
    ctx_params.n_threads_batch = 4;

    auto* ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context");
        llama_model_free(model);
        return 0;
    }

    auto* session = new LlamaSession();
    session->model = model;
    session->ctx = ctx;
    session->sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(session->sampler, llama_sampler_init_temp(0.1f));
    llama_sampler_chain_add(session->sampler, llama_sampler_init_dist(42));

    LOGD("Model loaded successfully, n_ctx=%d", nCtx);
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT void JNICALL
Java_com_drawtaxi_app_logic_ai_LlmRunner_llamaFreeModel(JNIEnv* env, jobject thiz, jlong sessionPtr) {
    auto* session = reinterpret_cast<LlamaSession*>(sessionPtr);
    if (!session) return;

    if (session->sampler) llama_sampler_free(session->sampler);
    if (session->ctx) llama_free(session->ctx);
    if (session->model) llama_model_free(session->model);
    delete session;
    LOGD("Model and context freed");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_drawtaxi_app_logic_ai_LlmRunner_llamaRunInference(JNIEnv* env, jobject thiz, jlong sessionPtr, jstring prompt, jint maxTokens) {
    auto* session = reinterpret_cast<LlamaSession*>(sessionPtr);
    if (!session || !session->ctx || !session->model) {
        LOGE("Invalid session");
        return nullptr;
    }

    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    std::string promptCpp(promptStr);
    env->ReleaseStringUTFChars(prompt, promptStr);

    // Tokenize prompt
    const int n_vocab = llama_n_vocab(llama_get_model(session->ctx));
    std::vector<llama_token> prompt_tokens(n_vocab);
    int n_tokens = llama_tokenize(session->ctx, promptCpp.c_str(), promptCpp.size(),
                                   prompt_tokens.data(), prompt_tokens.size(), true, true);
    if (n_tokens < 0) {
        LOGE("Failed to tokenize prompt");
        return nullptr;
    }
    prompt_tokens.resize(n_tokens);

    LOGD("Prompt tokenized: %d tokens", n_tokens);

    // Decode prompt
    auto batch = llama_batch_get_one(prompt_tokens.data(), n_tokens, 0, 0);
    if (llama_decode(session->ctx, batch) != 0) {
        LOGE("Failed to decode prompt");
        return nullptr;
    }

    // Generate response
    std::string result;
    int n_cur = n_tokens;

    for (int i = 0; i < maxTokens; i++) {
        llama_token new_token_id = llama_sampler_sample(session->sampler, session->ctx, n_cur - 1);

        // Check for EOS
        const auto* vocab = llama_model_get_vocab(session->model);
        if (llama_vocab_is_eog(vocab, new_token_id)) break;

        // Convert token to text
        char buf[256];
        int n = llama_token_to_piece(session->ctx, new_token_id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            result.append(buf, n);
        } else if (n < 0) {
            // Special token, skip
        }

        // Prepare next batch
        prompt_tokens.push_back(new_token_id);
        batch = llama_batch_get_one(&prompt_tokens.back(), 1, n_cur, 0);
        n_cur += 1;

        if (llama_decode(session->ctx, batch) != 0) {
            LOGE("Failed to decode token %d", i);
            break;
        }
    }

    LOGD("Generated %d tokens, result length: %zu", maxTokens, result.size());
    return env->NewStringUTF(result.c_str());
}
