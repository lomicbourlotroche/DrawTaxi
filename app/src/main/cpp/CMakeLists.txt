cmake_minimum_required(VERSION 3.22.1)
project(llama-jni)

set(JNI_LIBS_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

add_library(llama-jni SHARED llama-jni.cpp)

target_include_directories(llama-jni PRIVATE 
    ${CMAKE_SOURCE_DIR}/../../../../../llama.cpp/include
    ${CMAKE_SOURCE_DIR}/../../../../../llama.cpp/ggml/include
)

add_library(llama SHARED IMPORTED)
set_target_properties(llama PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/libllama.so)

add_library(ggml SHARED IMPORTED)
set_target_properties(ggml PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/libggml.so)

add_library(ggml-base SHARED IMPORTED)
set_target_properties(ggml-base PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/libggml-base.so)

add_library(ggml-cpu SHARED IMPORTED)
set_target_properties(ggml-cpu PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/libggml-cpu.so)

add_library(llama-common SHARED IMPORTED)
set_target_properties(llama-common PROPERTIES IMPORTED_LOCATION ${JNI_LIBS_DIR}/libllama-common.so)

target_link_libraries(llama-jni llama ggml ggml-base ggml-cpu llama-common log)
