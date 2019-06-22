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
package at.florianschuster.watchables.ui.scan

import android.graphics.Matrix
import android.os.Bundle
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.core.view.isVisible
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.android.koin.reactor
import at.florianschuster.watchables.R
import at.florianschuster.watchables.all.util.extensions.request
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.base.BaseReactor
import com.jakewharton.rxbinding3.view.clicks
import com.tailoredapps.androidutil.permissions.Permission
import com.tailoredapps.androidutil.permissions.queryState
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_scan.*
import kotlinx.android.synthetic.main.fragment_scan_permission.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class ScanScreen : BaseFragment(R.layout.fragment_scan), ReactorView<ScanReactor> {
    override val reactor: ScanReactor by reactor()

    private val rxPermissions: RxPermissions by inject { parametersOf(this) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind(reactor)
    }

    override fun bind(reactor: ScanReactor) {
        Permission.Camera.queryState(this).granted.let {
            layoutPermission.isVisible = !it
            if (it) cameraPreview.post { startCamera() }
        }

        btnQueryPermission.clicks()
            .flatMapSingle { rxPermissions.request(Permission.Camera) }
            .filter { it }
            .bind {
                layoutPermission.isVisible = false
                cameraPreview.post { startCamera() }
            }
            .addTo(disposables)

        btnCapture.clicks()
            .subscribe()
            .addTo(disposables)
    }

    private fun startCamera() {
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(Rational(1, 1))
            setTargetResolution(
                Size(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
            )
        }.build()

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            val parent = cameraPreview.parent as ViewGroup
            parent.removeView(cameraPreview)
            parent.addView(cameraPreview, 0)

            cameraPreview.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        CameraX.bindToLifecycle(this, preview)
    }

    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = cameraPreview.width / 2f
        val centerY = cameraPreview.height / 2f
        val rotationDegrees = when (cameraPreview.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
        cameraPreview.setTransform(matrix)
    }
}

class ScanReactor(

) : BaseReactor<ScanReactor.Action, ScanReactor.Mutation, ScanReactor.State>(
    initialState = State()
) {
    sealed class Action {

    }

    sealed class Mutation {

    }

    data class State(
        val model: Unit = Unit
    )

    override fun mutate(action: Action): Observable<out Mutation> {
        return super.mutate(action)
    }

    override fun reduce(previousState: State, mutation: Mutation): State {
        return super.reduce(previousState, mutation)
    }
}