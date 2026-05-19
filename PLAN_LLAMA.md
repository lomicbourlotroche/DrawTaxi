# Plan : Intégration llama.cpp pour IA 100% locale

## Objectif
Inférence **vraiment locale** via llama.cpp. Le modèle Llama 3.2 3B Q4_K_M (~2 Go) est chargé et exécuté **sur le device**, sans internet.

---

## Phase 1 : Compiler llama.cpp pour Android

### 1.1 Prérequis
- NDK Android installé (via Android Studio SDK Manager)
- CMake installé
- Git

### 1.2 Commandes
```bash
# Cloner llama.cpp
git clone --depth 1 https://github.com/ggerganov/llama.cpp.git
cd llama.cpp

# Compiler pour Android arm64
mkdir build-android && cd build-android
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-24 \
  -DBUILD_SHARED_LIBS=ON \
  -DLLAMA_BUILD_TESTS=OFF \
  -DLLAMA_BUILD_EXAMPLES=OFF \
  -DLLAMA_BUILD_SERVER=OFF
cmake --build . --config Release -j8

# Résultat : src/libllama.so
```

### 1.3 Copier dans le projet
```bash
cp src/libllama.so ../DrawTaxi/app/src/main/jniLibs/arm64-v8a/
```

---

## Phase 2 : Supprimer HuggingFace

| Fichier | Changement |
|---------|-----------|
| `LlmRunner.kt` | Réécrire → JNI llama.cpp |
| `AiSmsParser.kt` | Retirer `apiToken` des signatures |
| `AppSettings` | Retirer `hfApiToken` |
| `SettingsManager.kt` | Retirer `HF_API_TOKEN` |
| `SmsReceiver.kt` | Retirer `settings.hfApiToken` |
| `SmsProcessor.kt` | Retirer `settings.hfApiToken` |
| `SmsScanner.kt` | Retirer `apiToken` |
| `OvhImapService.kt` | Retirer `settings.hfApiToken` |
| `MainActivity.kt` | Retirer `settings.hfApiToken` |
| `TaxiViewModel.kt` | Retirer `settings.hfApiToken` |

---

## Phase 3 : JNI Bridge C++

### 3.1 `app/src/main/cpp/llama-jni.cpp`
Bridge JNI minimal (~150 lignes) :
- `llamaLoadModel(modelPath, nCtx)` → retourne handle (pointer)
- `llamaFreeModel(contextPtr)` → libère modèle + contexte
- `llamaRunInference(contextPtr, prompt, maxTokens, temp)` → retourne string

### 3.2 `app/src/main/cpp/CMakeLists.txt`
```cmake
cmake_minimum_required(VERSION 3.22.1)
project(llama-jni)

add_library(llama-jni SHARED llama-jni.cpp)
add_subdirectory(${CMAKE_SOURCE_DIR}/../../../../llama.cpp llama.cpp)
target_link_libraries(llama-jni llama)
```

---

## Phase 4 : LlmRunner.kt (JNI)

```kotlin
object LlmRunner {
    init { System.loadLibrary("llama-jni") }

    external fun llamaLoadModel(modelPath: String, nCtx: Int): Long
    external fun llamaFreeModel(contextPtr: Long)
    external fun llamaRunInference(contextPtr: Long, prompt: String, maxTokens: Int, temperature: Float): String?

    fun run(modelPath: String, prompt: String): String? {
        val ctx = llamaLoadModel(modelPath, nCtx = 512)
        if (ctx == 0L) return null
        return try {
            llamaRunInference(ctx, prompt, maxTokens = 256, temperature = 0.1f)
        } finally {
            llamaFreeModel(ctx)
        }
    }
}
```

---

## Phase 5 : build.gradle.kts

```kotlin
android {
    defaultConfig {
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt") }
    }
}
```

---

## Phase 6 : Vérification RAM

```kotlin
fun canRunModel(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mi = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mi)
    return mi.availMem > 2_000_000_000L // 2 Go minimum libre
}
```

---

## Phase 7 : ProGuard

```proguard
-keep class com.drawtaxi.app.logic.ai.LlmRunner { native <methods>; }
```

---

## Ordre d'exécution

1. ⬜ Compiler llama.cpp pour Android (NDK)
2. ⬜ Placer `libllama.so` dans `jniLibs/arm64-v8a/`
3. ⬜ Créer `cpp/llama-jni.cpp` + `CMakeLists.txt`
4. ⬜ Réécrire `LlmRunner.kt` avec JNI
5. ⬜ Configurer `build.gradle.kts` (CMake + NDK)
6. ⬜ Supprimer `hfApiToken` partout
7. ⬜ Simplifier `AiSmsParser`
8. ⬜ Ajouter vérification RAM
9. ⬜ ProGuard rules
10. ⬜ Build + test
