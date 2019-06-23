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

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.android.koin.reactor
import at.florianschuster.watchables.R
import at.florianschuster.watchables.all.util.extensions.request
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.base.BaseReactor
import at.florianschuster.watchables.ui.scan.camera.ScanService
import com.jakewharton.rxbinding3.view.clicks
import com.tailoredapps.androidutil.permissions.Permission
import com.tailoredapps.androidutil.permissions.queryState
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.fragment_scan.*
import kotlinx.android.synthetic.main.fragment_scan_permission.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class ScanScreen : BaseFragment(R.layout.fragment_scan), ReactorView<ScanReactor> {
    override val reactor: ScanReactor by reactor()

    private val rxPermissions: RxPermissions by inject { parametersOf(this) }
    private val scanService: ScanService by inject { parametersOf(this) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnQueryPermission.clicks()
            .flatMapSingle { rxPermissions.request(Permission.Camera) }
            .filter { it }
            .bind(::handleCameraPermission)
            .addTo(disposables)

        btnTorch.clicks()
            .filter { scanService.initialized }
            .bind {
                scanService.torch = !scanService.torch
                updateTorchImage()
            }
            .addTo(disposables)

        bind(reactor)

        Permission.Camera.queryState(this).granted.let(::handleCameraPermission)
    }

    override fun bind(reactor: ScanReactor) {
        val scanOutput = scanService.output.publish()

        scanOutput.firstElement()
            .subscribe { updateTorchImage() }
            .addTo(disposables)

        scanOutput.ofType<ScanService.Output.Data>()
            .map { ScanReactor.Action.DataDetected(it.barcodes) }
            .bind(to = reactor.action)
            .addTo(disposables)
    }

    private fun updateTorchImage() {
        when {
            scanService.torch -> R.drawable.ic_flash_off
            else -> R.drawable.ic_flash_on
        }.let(btnTorch::setImageResource)
    }

    private fun handleCameraPermission(granted: Boolean) {
        layoutPermission.isVisible = !granted
        if (granted) scanService.startOutput(cameraPreview)
    }
}

class ScanReactor(
) : BaseReactor<ScanReactor.Action, ScanReactor.Mutation, ScanReactor.State>(
    initialState = State()
) {
    sealed class Action {
        data class DataDetected(val barcodes: List<String>) : Action()
    }

    sealed class Mutation {
    }

    data class State(
        val result: List<String> = emptyList()
    )
}