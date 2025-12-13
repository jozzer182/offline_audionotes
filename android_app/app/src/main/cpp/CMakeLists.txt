# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name.
project("offlineaudionotes")

# ============================================================================
# whisper.cpp integration
# ============================================================================
# Path to whisper.cpp repository (sibling folder at repo root)
set(WHISPER_CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../whisper.cpp")

# Disable whisper.cpp extras we don't need for Android
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_SDL2 OFF CACHE BOOL "" FORCE)
set(WHISPER_CURL OFF CACHE BOOL "" FORCE)
set(WHISPER_COREML OFF CACHE BOOL "" FORCE)
set(WHISPER_OPENVINO OFF CACHE BOOL "" FORCE)

# Add whisper.cpp as a subdirectory
add_subdirectory(${WHISPER_CPP_DIR} ${CMAKE_CURRENT_BINARY_DIR}/whisper.cpp)

# ============================================================================
# Main JNI library
# ============================================================================
add_library(${CMAKE_PROJECT_NAME} SHARED
        whisper_jni.cpp)

# Include directories for whisper headers
target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        ${WHISPER_CPP_DIR}/include
        ${WHISPER_CPP_DIR}/ggml/include)

# Link libraries
target_link_libraries(${CMAKE_PROJECT_NAME}
        whisper
        android
        log)