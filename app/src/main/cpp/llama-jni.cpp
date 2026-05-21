#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <android/log.h>

#include "llama.h"

#define LOG_TAG "LlamaJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_backend_initialized = false;
static std::mutex g_backend_mutex;
static std::mutex g_inference_mutex;

struct LlamaSession {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_sampler* sampler = nullptr;
    llama_batch batch;
    bool batch_initialized = false;
};

static void throwJniException(JNIEnv* env, const char* message) {
    jclass exClass = env->FindClass("java/lang/RuntimeException");
    if (exClass) {
        env->ThrowNew(exClass, message);
    }
}

static void freeSession(LlamaSession* session) {
    if (!session) return;
    if (session->batch_initialized) {
        llama_batch_free(session->batch);
        session->batch_initialized = false;
    }
    if (session->sampler) {
        llama_sampler_free(session->sampler);
        session->sampler = nullptr;
    }
    if (session->ctx) {
        llama_free(session->ctx);
        session->ctx = nullptr;
    }
    if (session->model) {
        llama_model_free(session->model);
        session->model = nullptr;
    }
    delete session;
}

extern "C" JNIEXPORT void JNICALL
Java_com_drawtaxi_app_logic_ai_LlmRunner_llamaBackendInit(JNIEnv* env, jobject thiz) {
    std::lock_guard<std::mutex> lock(g_backend_mutex);
    if (!g_backend_initialized) {
        llama_backend_init();
        g_backend_initialized = true;
        LOGD("llama backend initialized");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_drawtaxi_app_logic_ai_LlmRunner_llamaBackendFree(JNIEnv* env, jobject thiz) {
    std::lock_guard<std::mutex> lock(g_backend_mutex);
    if (g_backend_initialized) {
        llama_backend_free();
        g_backend_initialized = false;
        LOGD("llama backend freed");
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_drawtaxi_app_logic_ai_LlmRunner_llamaLoadModel(JNIEnv* env, jobject thiz, jstring modelPath, jint nCtx) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (!path) {
        LOGE("Failed to get model path string");
        return 0;
    }

    LOGD("Loading model from: %s with n_ctx=%d", path, nCtx);

    auto model_params = llama_model_default_params();
    auto* model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("Failed to load model from path");
        throwJniException(env, "Failed to load model - file may be corrupt or incompatible");
        return 0;
    }

    auto ctx_params = llama_context_default_params();
    ctx_params.n_ctx = nCtx;
    ctx_params.n_batch = nCtx;
    ctx_params.n_threads = 4;
    ctx_params.n_threads_batch = 4;

    auto* ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context from model");
        llama_model_free(model);
        throwJniException(env, "Failed to create inference context");
        return 0;
    }

    auto* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (!sampler) {
        LOGE("Failed to create sampler chain");
        llama_free(ctx);
        llama_model_free(model);
        throwJniException(env, "Failed to create sampler");
        return 0;
    }

    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.1f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(42));

    auto* session = new LlamaSession();
    session->model = model;
    session->ctx = ctx;
    session->sampler = sampler;

    LOGD("Model loaded successfully, n_ctx=%d", nCtx);
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT void JNICALL
Java_com_drawtaxi_app_logic_ai_LlmRunner_llamaFreeModel(JNIEnv* env, jobject thiz, jlong sessionPtr) {
    auto* session = reinterpret_cast<LlamaSession*>(sessionPtr);
    if (!session) {
        LOGE("llamaFreeModel: null session pointer");
        return;
    }

    LOGD("Freeing model session");
    freeSession(session);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_drawtaxi_app_logic_ai_LlmRunner_llamaRunInference(JNIEnv* env, jobject thiz, jlong sessionPtr, jstring prompt, jint maxTokens) {
    auto* session = reinterpret_cast<LlamaSession*>(sessionPtr);
    if (!session || !session->ctx || !session->model || !session->sampler) {
        LOGE("Invalid session: null pointer");
        throwJniException(env, "Invalid session - model not loaded");
        return nullptr;
    }

    // Lock for thread safety
    std::lock_guard<std::mutex> lock(g_inference_mutex);

    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    if (!promptStr) {
        LOGE("Failed to get prompt string");
        return nullptr;
    }
    std::string promptCpp(promptStr);
    env->ReleaseStringUTFChars(prompt, promptStr);

    if (promptCpp.empty()) {
        LOGE("Empty prompt");
        throwJniException(env, "Empty prompt");
        return nullptr;
    }

    // Get vocab
    const auto* vocab = llama_model_get_vocab(session->model);
    if (!vocab) {
        LOGE("Failed to get vocabulary");
        throwJniException(env, "Failed to get vocabulary");
        return nullptr;
    }

    // Tokenize prompt: first call to get required size, then allocate
    int n_tokens = llama_tokenize(vocab, promptCpp.c_str(), promptCpp.size(),
                                   nullptr, 0, true, true);
    if (n_tokens <= 0) {
        LOGE("Failed to tokenize prompt, n_tokens=%d", n_tokens);
        return nullptr;
    }

    std::vector<llama_token> prompt_tokens(n_tokens);
    n_tokens = llama_tokenize(vocab, promptCpp.c_str(), promptCpp.size(),
                               prompt_tokens.data(), prompt_tokens.size(), true, true);
    if (n_tokens < 0) {
        LOGE("Failed to tokenize prompt on second call");
        return nullptr;
    }
    prompt_tokens.resize(n_tokens);

    LOGD("Prompt tokenized: %d tokens, prompt length: %zu chars", n_tokens, promptCpp.size());

    // Initialize batch if needed
    if (!session->batch_initialized) {
        session->batch = llama_batch_init(n_tokens + maxTokens, 0, 1);
        session->batch_initialized = true;
    }

    // Prepare batch with prompt tokens at sequential positions
    session->batch.n_tokens = 0;
    for (int i = 0; i < n_tokens; i++) {
        session->batch.token[i] = prompt_tokens[i];
        session->batch.pos[i] = i;
        session->batch.n_seq_id[i] = 1;
        session->batch.seq_id[i] = nullptr;
        session->batch.logits[i] = (i == n_tokens - 1);
        session->batch.n_tokens++;
    }

    if (llama_decode(session->ctx, session->batch) != 0) {
        LOGE("Failed to decode prompt batch");
        return nullptr;
    }

    // Generate response tokens
    std::string result;
    int n_cur = n_tokens;

    for (int i = 0; i < maxTokens; i++) {
        // Sample next token from the last position
        llama_token new_token_id = llama_sampler_sample(session->sampler, session->ctx, n_cur - 1);

        // Check for end of generation
        if (llama_vocab_is_eog(vocab, new_token_id)) {
            LOGD("EOS token reached at token %d", i);
            break;
        }

        // Convert token to text
        char buf[256];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            result.append(buf, n);
        }

        // Feed token back for next iteration
        session->batch.n_tokens = 0;
        session->batch.token[0] = new_token_id;
        session->batch.pos[0] = n_cur;
        session->batch.n_seq_id[0] = 1;
        session->batch.seq_id[0] = nullptr;
        session->batch.logits[0] = true;
        session->batch.n_tokens = 1;
        n_cur++;

        if (llama_decode(session->ctx, session->batch) != 0) {
            LOGE("Failed to decode token %d at position %d", i, n_cur - 1);
            break;
        }
    }

    LOGD("Generated %d tokens, result length: %zu chars", n_cur - n_tokens, result.size());
    return env->NewStringUTF(result.c_str());
}
