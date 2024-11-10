package com.WALKR.core.geospatial

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SpeechRecognizerHelper(
    private val context: Context,
    private val listener: SpeechRecognitionListener,
    apiKey: String
) : RecognitionListener {

    interface SpeechRecognitionListener {
        fun onRecognizedText(text: String)
        fun onRecognitionError(errorMessage: String)
    }

    private lateinit var speechService: SpeechService
    private val placeFinderHelper = PlaceFinderHelper(apiKey)

    fun initializeRecognizer(customPhrases: List<String>) {
        val modelDir = File(context.filesDir, "vosk-model-small-en-us-0.15")
        if (!modelDir.exists()) {
            copyModelFromAssets("vosk-model-small-en-us-0.15", modelDir)
        }

        try {
            val model = Model(modelDir.absolutePath)
            val recognizer = Recognizer(model, 16000.0f, customPhrases.joinToString(prefix = "[", postfix = "]") { "\"$it\"" })
            speechService = SpeechService(recognizer, 16000.0f)
            speechService.startListening(this)
        } catch (e: IOException) {
            listener.onRecognitionError("Failed to initialize model.")
        }
    }

    private fun copyModelFromAssets(assetPath: String, destDir: File) {
        destDir.mkdirs()
        context.assets.list(assetPath)?.forEach { file ->
            val srcPath = "$assetPath/$file"
            val outFile = File(destDir, file)

            if (context.assets.list(srcPath)?.isNotEmpty() == true) {
                copyModelFromAssets(srcPath, outFile) // Recursively copy directories
            } else {
                context.assets.open(srcPath).use { inputStream ->
                    FileOutputStream(outFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }
        Log.d("SpeechRecognizerHelper", "Model copied to internal storage.")
    }

    private fun stopListening() {
        if (this::speechService.isInitialized) {
            speechService.stop()
            Log.d("SpeechRecognizerHelper", "Stopped listening.")
        }
    }

    override fun onResult(hypothesis: String?) {
        hypothesis?.let {
            val recognizedText = JSONObject(it).optString("text").lowercase()

            when {
                recognizedText.contains("hibachi station") && recognizedText.contains("accessible") -> {
                    listener.onRecognizedText("hibachi station accessible")
                    stopListening()
                    fetchPlaceDetails("hibachi station")
                }
                recognizedText.contains("hibachi station") -> {
                    listener.onRecognizedText("hibachi station")
                    stopListening()
                    fetchPlaceDetails("hibachi station")
                }
                recognizedText.contains("pothole") && recognizedText.contains("report") -> {
                    listener.onRecognizedText("report pothole")
                }
                else -> {
                    listener.onRecognizedText("Unrecognized phrase")
                }
            }
        }
    }

    private fun fetchPlaceDetails(locationName: String) {
        placeFinderHelper.getPlaceAddress(locationName, object : PlaceFinderHelper.PlaceFinderCallback {
            override fun onResult(address: String?, coordinates: Map<String, Double>?) {
                Log.d("SpeechRecognizerHelper", "Fetched Address: $address, Coordinates: $coordinates")
                // Handle the fetched address and coordinates here as needed
            }
        })
    }

    override fun onPartialResult(hypothesis: String?) {}

    override fun onFinalResult(hypothesis: String?) {}

    override fun onTimeout() {}

    override fun onError(e: Exception?) {
        listener.onRecognitionError(e?.message ?: "Unknown error")
    }
}
