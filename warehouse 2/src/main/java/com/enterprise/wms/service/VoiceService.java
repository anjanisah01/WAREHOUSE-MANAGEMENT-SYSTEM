package com.enterprise.wms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

/**
 * Speech-to-Text (STT) and Text-to-Speech (TTS) service.
 * Supports multiple providers: Google STT, Vosk (offline), custom endpoints, and simulated fallback.
 * TTS goes through OpenAI or a custom endpoint.
 */
@Service
public class VoiceService {

    private final OkHttpClient httpClient = new OkHttpClient();   // shared HTTP client for API calls
    private final ObjectMapper objectMapper = new ObjectMapper();  // JSON parser
    private volatile Model voskModel;                              // lazily loaded Vosk offline model

    // ── STT configuration (injected from application.yml) ──
    @Value("${wms.voice.stt-provider:simulated}") private String sttProvider;       // which STT provider to use
    @Value("${wms.voice.stt-url:}")               private String sttUrl;            // custom STT endpoint URL
    @Value("${wms.voice.vosk-model-path:}")        private String voskModelPath;     // filesystem path to Vosk model
    @Value("${wms.voice.vosk-sample-rate:16000}")  private Float  voskSampleRate;    // audio sample rate for Vosk
    @Value("${wms.voice.google-stt-api-key:}")     private String googleSttApiKey;   // Google Cloud STT API key
    @Value("${wms.voice.google-stt-language:en-US}") private String googleSttLanguage;
    @Value("${wms.voice.google-stt-sample-rate:16000}") private Integer googleSttSampleRate;
    @Value("${wms.voice.google-stt-encoding:LINEAR16}") private String googleSttEncoding;

    // ── TTS configuration ──
    @Value("${wms.voice.tts-url:}")                    private String ttsUrl;          // custom TTS endpoint URL
    @Value("${wms.voice.api-key:}")                    private String voiceApiKey;      // API key for custom voice endpoints
    @Value("${wms.openai.api-key:}")                   private String openAiApiKey;     // OpenAI API key (shared with LLM)
    @Value("${wms.voice.openai-tts-model:gpt-4o-mini-tts}") private String openAiTtsModel;
    @Value("${wms.voice.openai-tts-voice:alloy}")      private String openAiTtsVoice;
    @Value("${wms.voice.openai-tts-format:mp3}")       private String openAiTtsFormat;

    private static final String SIMULATED = "simulated voice command: show products expiring in 7 days";

    // ────────────── Speech-to-Text ──────────────

    /** Converts raw audio bytes to text using the configured STT provider. */
    public String speechToText(byte[] audio) {
        String provider = sttProvider == null ? "simulated" : sttProvider.trim().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "google" -> googleStt(audio);
            case "custom" -> customStt(audio);
            case "vosk"   -> voskStt(audio);
            default       -> SIMULATED; // no real provider configured
        };
    }

    /** Sends audio to a custom STT endpoint as base64 JSON. */
    private String customStt(byte[] audio) {
        if (sttUrl == null || sttUrl.isBlank()) return SIMULATED;
        try {
            String json = "{\"audioBase64\":\"" + Base64.getEncoder().encodeToString(audio) + "\"}";
            Request req = new Request.Builder()
                    .url(sttUrl)
                    .addHeader("Authorization", "Bearer " + voiceApiKey)
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();
            try (Response res = httpClient.newCall(req).execute()) {
                return (res.isSuccessful() && res.body() != null) ? res.body().string() : SIMULATED;
            }
        } catch (IOException e) { return SIMULATED; }
    }

    /** Calls Google Cloud Speech-to-Text REST API. */
    private String googleStt(byte[] audio) {
        if (googleSttApiKey == null || googleSttApiKey.isBlank()) return SIMULATED;
        try {
            // Build the request payload with config + base64-encoded audio
            var payload = objectMapper.createObjectNode();
            payload.set("config", objectMapper.createObjectNode()
                    .put("encoding", googleSttEncoding)
                    .put("sampleRateHertz", googleSttSampleRate)
                    .put("languageCode", googleSttLanguage));
            payload.set("audio", objectMapper.createObjectNode()
                    .put("content", Base64.getEncoder().encodeToString(audio)));

            Request req = new Request.Builder()
                    .url("https://speech.googleapis.com/v1/speech:recognize?key=" + googleSttApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.parse("application/json")))
                    .build();

            try (Response res = httpClient.newCall(req).execute()) {
                if (!res.isSuccessful() || res.body() == null) return SIMULATED;
                // Extract the transcript from the first alternative
                JsonNode transcript = objectMapper.readTree(res.body().string())
                        .path("results").path(0).path("alternatives").path(0).path("transcript");
                return (transcript.isTextual() && !transcript.asText().isBlank()) ? transcript.asText() : SIMULATED;
            }
        } catch (IOException e) { return SIMULATED; }
    }

    /** Runs Vosk offline speech recognition against the loaded model. */
    private String voskStt(byte[] audio) {
        try {
            Model model = getOrLoadVoskModel();
            if (model == null) return SIMULATED;
            try (Recognizer rec = new Recognizer(model, voskSampleRate)) {
                rec.acceptWaveForm(audio, audio.length);            // feed the audio data
                String text = objectMapper.readTree(rec.getFinalResult()).path("text").asText("");
                return text.isBlank() ? SIMULATED : text;
            }
        } catch (Exception | UnsatisfiedLinkError e) { return SIMULATED; }
    }

    /** Lazy-loads the Vosk model from disk (double-checked locking). */
    private Model getOrLoadVoskModel() throws IOException {
        if (voskModel != null) return voskModel;
        synchronized (this) {
            if (voskModel != null) return voskModel;
            if (voskModelPath == null || voskModelPath.isBlank()) return null;
            voskModel = new Model(voskModelPath);
            return voskModel;
        }
    }

    // ────────────── Text-to-Speech ──────────────

    /** Converts text to audio bytes (default format). */
    public byte[] textToSpeech(String text) { return textToSpeech(text, null); }

    /** Converts text to audio bytes in the requested format. Falls back to OpenAI TTS if no custom URL is set. */
    public byte[] textToSpeech(String text, String formatOverride) {
        String fmt = resolveFormat(formatOverride);
        // Try custom TTS endpoint first, fall back to OpenAI
        if (ttsUrl == null || ttsUrl.isBlank()) return openAiTts(text, fmt);
        try {
            String json = "{\"text\":\"" + text.replace("\"", "'") + "\"}";
            Request req = new Request.Builder()
                    .url(ttsUrl)
                    .addHeader("Authorization", "Bearer " + voiceApiKey)
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();
            try (Response res = httpClient.newCall(req).execute()) {
                return (res.isSuccessful() && res.body() != null) ? res.body().bytes() : fallback(text);
            }
        } catch (IOException e) { return fallback(text); }
    }

    /** Returns the MIME type for the TTS audio output. */
    public String ttsContentType()                    { return ttsContentType(null); }
    public String ttsContentType(String formatOverride) {
        return switch (resolveFormat(formatOverride)) {
            case "mp3"  -> "audio/mpeg";
            case "wav"  -> "audio/wav";
            case "opus" -> "audio/opus";
            case "flac" -> "audio/flac";
            case "aac"  -> "audio/aac";
            default     -> "application/octet-stream";
        };
    }

    /** Calls the OpenAI TTS API. */
    private byte[] openAiTts(String text, String format) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) return fallback(text);
        try {
            String json = """
                    {"model":"%s","voice":"%s","input":"%s","response_format":"%s"}
                    """.formatted(openAiTtsModel, openAiTtsVoice, text.replace("\"", "\\\""), format);
            Request req = new Request.Builder()
                    .url("https://api.openai.com/v1/audio/speech")
                    .addHeader("Authorization", "Bearer " + openAiApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();
            try (Response res = httpClient.newCall(req).execute()) {
                return (res.isSuccessful() && res.body() != null) ? res.body().bytes() : fallback(text);
            }
        } catch (IOException e) { return fallback(text); }
    }

    /** Normalises the audio format string, defaulting to mp3. */
    private String resolveFormat(String override) {
        String f = (override == null || override.isBlank()) ? openAiTtsFormat : override;
        String norm = (f == null ? "" : f).trim().toLowerCase();
        return switch (norm) { case "mp3","wav","opus","flac","aac" -> norm; default -> "mp3"; };
    }

    /** Simple text fallback when no TTS provider is available. */
    private static byte[] fallback(String text) { return ("SPOKEN:" + text).getBytes(); }
}
