/**
 * JNI Bridge for whisper.cpp
 * 
 * This file provides the native implementation for transcribing audio files
 * using the whisper.cpp library. It exposes a single JNI function that:
 * - Loads a whisper model from a file path
 * - Reads WAV audio data from a file
 * - Runs transcription using whisper_full()
 * - Returns the transcript as a Java String
 * 
 * Audio Format Requirements:
 * - WAV file format
 * - 16-bit PCM
 * - Mono channel
 * - 16kHz sample rate
 */

#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <android/log.h>
#include "whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global whisper context (cached for performance)
static struct whisper_context* g_ctx = nullptr;
static std::string g_model_path;

/**
 * Read WAV file and extract PCM samples as float array.
 * Expects 16-bit PCM, mono, 16kHz WAV file.
 * 
 * @param path Path to the WAV file
 * @param samples Output vector for float samples (normalized to [-1, 1])
 * @return true if successful, false otherwise
 */
static bool read_wav_file(const std::string& path, std::vector<float>& samples) {
    std::ifstream file(path, std::ios::binary);
    if (!file.is_open()) {
        LOGE("Cannot open WAV file: %s", path.c_str());
        return false;
    }

    // Read WAV header
    char riff[4];
    file.read(riff, 4);
    if (std::string(riff, 4) != "RIFF") {
        LOGE("Invalid WAV file: missing RIFF header");
        return false;
    }

    uint32_t chunk_size;
    file.read(reinterpret_cast<char*>(&chunk_size), 4);

    char wave[4];
    file.read(wave, 4);
    if (std::string(wave, 4) != "WAVE") {
        LOGE("Invalid WAV file: missing WAVE header");
        return false;
    }

    // Find fmt chunk
    char fmt_id[4];
    uint32_t fmt_size;
    uint16_t audio_format, num_channels;
    uint32_t sample_rate, byte_rate;
    uint16_t block_align, bits_per_sample;

    while (file.read(fmt_id, 4)) {
        file.read(reinterpret_cast<char*>(&fmt_size), 4);
        
        if (std::string(fmt_id, 4) == "fmt ") {
            file.read(reinterpret_cast<char*>(&audio_format), 2);
            file.read(reinterpret_cast<char*>(&num_channels), 2);
            file.read(reinterpret_cast<char*>(&sample_rate), 4);
            file.read(reinterpret_cast<char*>(&byte_rate), 4);
            file.read(reinterpret_cast<char*>(&block_align), 2);
            file.read(reinterpret_cast<char*>(&bits_per_sample), 2);
            
            // Skip any extra format bytes
            if (fmt_size > 16) {
                file.seekg(fmt_size - 16, std::ios::cur);
            }
            break;
        } else {
            file.seekg(fmt_size, std::ios::cur);
        }
    }

    if (audio_format != 1) {
        LOGE("Unsupported audio format: %d (expected PCM=1)", audio_format);
        return false;
    }

    if (num_channels != 1) {
        LOGE("Unsupported channel count: %d (expected mono=1)", num_channels);
        return false;
    }

    if (bits_per_sample != 16) {
        LOGE("Unsupported bits per sample: %d (expected 16)", bits_per_sample);
        return false;
    }

    LOGI("WAV format: %d Hz, %d channels, %d bits", sample_rate, num_channels, bits_per_sample);

    // Find data chunk
    char data_id[4];
    uint32_t data_size;
    
    while (file.read(data_id, 4)) {
        file.read(reinterpret_cast<char*>(&data_size), 4);
        
        if (std::string(data_id, 4) == "data") {
            break;
        } else {
            file.seekg(data_size, std::ios::cur);
        }
    }

    if (std::string(data_id, 4) != "data") {
        LOGE("Cannot find data chunk in WAV file");
        return false;
    }

    // Read PCM data
    size_t num_samples = data_size / (bits_per_sample / 8);
    samples.resize(num_samples);

    std::vector<int16_t> pcm_data(num_samples);
    file.read(reinterpret_cast<char*>(pcm_data.data()), data_size);

    // Convert to float [-1, 1]
    for (size_t i = 0; i < num_samples; i++) {
        samples[i] = static_cast<float>(pcm_data[i]) / 32768.0f;
    }

    LOGI("Loaded %zu samples from WAV file", num_samples);
    return true;
}

/**
 * Initialize or get the whisper context.
 * Caches the context for reuse if the model path hasn't changed.
 */
static struct whisper_context* get_whisper_context(const std::string& model_path) {
    if (g_ctx != nullptr && g_model_path == model_path) {
        return g_ctx;
    }

    // Free existing context if model changed
    if (g_ctx != nullptr) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
    }

    LOGI("Loading whisper model from: %s", model_path.c_str());

    struct whisper_context_params cparams = whisper_context_default_params();
    g_ctx = whisper_init_from_file_with_params(model_path.c_str(), cparams);

    if (g_ctx == nullptr) {
        LOGE("Failed to load whisper model");
        return nullptr;
    }

    g_model_path = model_path;
    LOGI("Whisper model loaded successfully");
    return g_ctx;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_zarabandajose_offlineaudionotes_stt_WhisperBridge_transcribeFile(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jstring audioPath,
        jstring language) {

    // Convert Java strings to C++ strings
    const char* model_path_cstr = env->GetStringUTFChars(modelPath, nullptr);
    const char* audio_path_cstr = env->GetStringUTFChars(audioPath, nullptr);
    const char* language_cstr = env->GetStringUTFChars(language, nullptr);

    std::string model_path(model_path_cstr);
    std::string audio_path(audio_path_cstr);
    std::string lang(language_cstr);

    env->ReleaseStringUTFChars(modelPath, model_path_cstr);
    env->ReleaseStringUTFChars(audioPath, audio_path_cstr);
    env->ReleaseStringUTFChars(language, language_cstr);

    LOGI("Transcribe request - model: %s, audio: %s, lang: %s",
         model_path.c_str(), audio_path.c_str(), lang.c_str());

    // Get whisper context
    struct whisper_context* ctx = get_whisper_context(model_path);
    if (ctx == nullptr) {
        return env->NewStringUTF("ERROR: Failed to load whisper model");
    }

    // Read audio file
    std::vector<float> samples;
    if (!read_wav_file(audio_path, samples)) {
        return env->NewStringUTF("ERROR: Failed to read audio file");
    }

    if (samples.empty()) {
        return env->NewStringUTF("ERROR: Audio file is empty");
    }

    // Configure whisper parameters
    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = lang.c_str();
    params.n_threads = 4;
    params.offset_ms = 0;
    params.no_context = true;
    params.single_segment = false;

    LOGI("Running whisper transcription with %zu samples...", samples.size());

    // Run transcription
    int result = whisper_full(ctx, params, samples.data(), static_cast<int>(samples.size()));
    if (result != 0) {
        LOGE("Whisper transcription failed with code: %d", result);
        return env->NewStringUTF("ERROR: Transcription failed");
    }

    // Collect transcript from all segments
    std::string transcript;
    int n_segments = whisper_full_n_segments(ctx);
    LOGI("Transcription complete: %d segments", n_segments);

    for (int i = 0; i < n_segments; i++) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        if (text != nullptr) {
            transcript += text;
        }
    }

    // Trim leading/trailing whitespace
    size_t start = transcript.find_first_not_of(" \t\n\r");
    size_t end = transcript.find_last_not_of(" \t\n\r");
    if (start != std::string::npos && end != std::string::npos) {
        transcript = transcript.substr(start, end - start + 1);
    }

    LOGI("Transcript: %s", transcript.c_str());
    return env->NewStringUTF(transcript.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_zarabandajose_offlineaudionotes_stt_WhisperBridge_releaseModel(
        JNIEnv* env,
        jobject /* this */) {
    if (g_ctx != nullptr) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
        g_model_path.clear();
        LOGI("Whisper model released");
    }
}
