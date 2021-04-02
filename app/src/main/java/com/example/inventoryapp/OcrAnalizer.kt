package com.example.inventoryapp

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.inventoryapp.barcode.ScanningResultListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition

class OcrAnalizer (private val listener: ScanningResultListener) : ImageAnalysis.Analyzer {

    private var isScanning: Boolean = false

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isScanning) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            // Pass image to an ML Kit Vision API
            // ...
            isScanning = true
            // [START get_detector_default]
            val recognizer = TextRecognition.getClient()
            // [END get_detector_default]

            // [START run_detector]
            val result = recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Task completed successfully
                    // [START_EXCLUDE]
                    // [START get_text]
                    for (block in visionText.textBlocks) {
                        val boundingBox = block.boundingBox
                        val cornerPoints = block.cornerPoints
                        val text = block.text

                        Log.e("OCR", text);
                        for (line in block.lines) {
                            // ...
                            for (element in line.elements) {
                                // ...
                            }
                        }
                    }

                    isScanning = false
                    imageProxy.close()
                    // [END get_text]
                    // [END_EXCLUDE]
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    // ...
                    isScanning = false
                    imageProxy.close()
                }
            // [END run_detector]
        }
    }
}