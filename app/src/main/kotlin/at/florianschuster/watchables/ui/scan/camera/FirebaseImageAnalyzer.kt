/*
 * Copyright 2019 Florian Schuster. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.florianschuster.watchables.ui.scan.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import timber.log.Timber

class FirebaseImageAnalyzer(
    private val intervalInMillis: Long = 1000,
    private val callback: (ScanService.Output) -> Unit
) : ImageAnalysis.Analyzer {
    private var lastAnalyzedTimestamp = System.currentTimeMillis()

    override fun analyze(proxy: ImageProxy?, rotationDegrees: Int) {
        val image = proxy?.image ?: return

        val timestamp = System.currentTimeMillis()
        if (timestamp - lastAnalyzedTimestamp < intervalInMillis) return

        val visionImage = FirebaseVisionImage.fromMediaImage(
            image,
            getOrientationFromRotation(rotationDegrees)
        )

        FirebaseVision.getInstance()
            .visionBarcodeDetector
            .detectInImage(visionImage)
            .addOnSuccessListener {
                val rawValues = it.mapNotNull(FirebaseVisionBarcode::getRawValue)
                callback(ScanService.Output.Data(rawValues))
            }
            .addOnFailureListener { callback(ScanService.Output.Error(it)) }

        lastAnalyzedTimestamp = timestamp
    }

    private fun getOrientationFromRotation(rotationDegrees: Int): Int {
        return when (rotationDegrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> FirebaseVisionImageMetadata.ROTATION_90
        }
    }
}