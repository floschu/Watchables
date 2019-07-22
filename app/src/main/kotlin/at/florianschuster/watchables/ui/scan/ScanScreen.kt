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

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.android.koin.reactor
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.watchables.R
import at.florianschuster.watchables.service.DeepLinkService
import at.florianschuster.watchables.all.util.extensions.request
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.base.BaseReactor
import at.florianschuster.watchables.ui.scan.qr.decode
import com.jakewharton.rxbinding3.view.clicks
import com.tailoredapps.androidutil.async.Async
import com.tailoredapps.androidutil.async.mapToAsync
import com.tailoredapps.androidutil.permissions.Permission
import com.tailoredapps.androidutil.permissions.queryState
import com.tailoredapps.androidutil.ui.extensions.afterMeasured
import com.tailoredapps.androidutil.ui.extensions.toast
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.fragment_scan.*
import kotlinx.android.synthetic.main.fragment_scan_permission.*
import kotlinx.android.synthetic.main.fragment_scan_toolbar.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit

class ScanScreen : BaseFragment(R.layout.fragment_scan), ReactorView<ScanReactor> {
    override val reactor: ScanReactor by reactor()

    private val rxPermissions: RxPermissions by inject { parametersOf(this) }

    private val Context.flashAvailable: Boolean
        get() = applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    private var torchOn = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnTorch.isVisible = context?.flashAvailable ?: false

        btnBack.clicks().bind { navController.navigateUp() }.addTo(disposables)

        btnQueryPermission.clicks()
            .flatMapSingle { rxPermissions.request(Permission.Camera) }
            .filter { it }
            .bind(::handleCameraPermission)
            .addTo(disposables)

        btnTorch.clicks()
            .map { !torchOn }
            .bind {
                torchOn = it
                scanView.setTorch(it)

                btnTorch.setIconResource(if (it) R.drawable.ic_flash_off else R.drawable.ic_flash_on)
                btnTorch.setText(if (it) R.string.scan_flash_off else R.string.scan_flash_on)
                btnTorch.isSelected = it
            }
            .addTo(disposables)

        bind(reactor)

        Permission.Camera.queryState(this).granted.let(::handleCameraPermission)
    }

    override fun bind(reactor: ScanReactor) {
        scanView.decode()
            .throttleFirst(1000, TimeUnit.MILLISECONDS)
            .map { ScanReactor.Action.Decode(it.text) }
            .bind(to = reactor.action)
            .addTo(disposables)

        reactor.state.changesFrom { it.detectionResult }
            .bind { scanDataAsync ->
                loading.isVisible = scanDataAsync.loading
                when (scanDataAsync) {
                    is Async.Success -> {
                        ScanScreenDirections.actionScanToDetail(
                            scanDataAsync.element.id,
                            scanDataAsync.element.type
                        ).let(navController::navigate)
                    }
                    is Async.Error -> toast(R.string.scan_error)
                }
            }
            .addTo(disposables)
    }

    private fun handleCameraPermission(granted: Boolean) {
        layoutPermission.isVisible = !granted
        if (granted) scanView.resume()
    }

    override fun onStop() {
        super.onStop()
        scanView.pause()
    }
}

class ScanReactor(
    private val deepLinkService: DeepLinkService
) : BaseReactor<ScanReactor.Action, ScanReactor.Mutation, ScanReactor.State>(
    initialState = State()
) {
    sealed class Action {
        data class Decode(val code: String) : Action()
    }

    sealed class Mutation {
        data class SetDetectionResult(val data: Async<DeepLinkService.Link.ToWatchable>) : Mutation()
    }

    data class State(
        val detectionResult: Async<DeepLinkService.Link.ToWatchable> = Async.Uninitialized
    )

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.Decode -> {
            deepLinkService.parseDeepLink(action.code)
                .ofType<DeepLinkService.Link.ToWatchable>()
                .toSingle()
                .mapToAsync()
                .map { Mutation.SetDetectionResult(it) }
                .toObservable()
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetDetectionResult -> {
            previousState.copy(detectionResult = mutation.data)
        }
    }
}