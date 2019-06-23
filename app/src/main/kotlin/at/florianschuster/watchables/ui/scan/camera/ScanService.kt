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

import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.util.Rational
import android.util.Size
import android.view.TextureView
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysisConfig
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.lifecycle.LifecycleOwner
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.util.concurrent.TimeUnit

interface ScanService {
    sealed class Output {
        data class Data(val barcodes: List<String>) : Output()
        data class Error(val error: Throwable) : Output()
        object None : Output()
    }

    val initialized: Boolean

    var torch: Boolean
    val output: Flowable<Output>

    fun startOutput(cameraPreview: TextureView)
    fun stopOutput()
}

class FirebaseScanService(
    private val lifecycleOwner: LifecycleOwner,
    private val resources: Resources
) : ScanService {
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    override val initialized: Boolean
        get() = previewUseCase != null && analysisUseCase != null

    private val outputRelay: BehaviorRelay<ScanService.Output> =
        BehaviorRelay.createDefault(ScanService.Output.None)

    override val output: Flowable<ScanService.Output>
        get() = outputRelay.distinctUntilChanged().toFlowable(BackpressureStrategy.LATEST)

    override var torch: Boolean
        get() = previewUseCase?.isTorchOn ?: false
        set(value) {
            previewUseCase?.enableTorch(value)
        }

    override fun startOutput(cameraPreview: TextureView) {
        stopOutput()
        val metrics = resources.displayMetrics.widthPixels to resources.displayMetrics.heightPixels

        cameraPreview.post {
            val previewConfig = PreviewConfig.Builder().apply {
                setLensFacing(CameraX.LensFacing.BACK)
                setTargetAspectRatio(Rational(metrics.first, metrics.second))
                setTargetRotation(cameraPreview.display.rotation)
            }.build()

            previewUseCase = AutoFitPreviewBuilder.build(previewConfig, cameraPreview)

            val analyzerConfig = ImageAnalysisConfig.Builder().apply {
                setLensFacing(CameraX.LensFacing.BACK)
                val analyzerThread = HandlerThread("CameraServiceAnalyzer").apply {
                    start()
                }
                setCallbackHandler(Handler(analyzerThread.looper))
                setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                setTargetResolution(Size(metrics.first, metrics.second))
            }.build()

            analysisUseCase = ImageAnalysis(analyzerConfig).also {
                it.analyzer = FirebaseImageAnalyzer(callback = outputRelay::accept)
            }

            CameraX.bindToLifecycle(lifecycleOwner, previewUseCase, analysisUseCase)
        }
    }

    override fun stopOutput() {
        CameraX.unbind(*listOfNotNull(previewUseCase, analysisUseCase).toTypedArray())
        outputRelay.accept(ScanService.Output.None)
    }
}
