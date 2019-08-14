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

package at.florianschuster.watchables.ui.scan.qr

import androidx.annotation.CheckResult
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

enum class BarCodeScanType {
    SINGLE, CONTINUOUS
}

@CheckResult
fun BarcodeView.decode(): Observable<BarcodeResult> {
    return BarcodeViewDecodeObservable(this, BarCodeScanType.CONTINUOUS)
}

internal class BarcodeViewDecodeObservable(
    private val view: BarcodeView,
    private val type: BarCodeScanType
) : Observable<BarcodeResult>() {
    override fun subscribeActual(observer: Observer<in BarcodeResult>) {
        val listener = Listener(view, type, observer)
        observer.onSubscribe(listener)
        if (type == BarCodeScanType.CONTINUOUS) view.decodeContinuous(listener.callback)
        else view.decodeSingle(listener.callback)
    }

    private inner class Listener(
        private val view: BarcodeView,
        private val type: BarCodeScanType,
        observer: Observer<in BarcodeResult>
    ) : MainThreadDisposable() {
        internal val callback: BarcodeCallback

        init {
            callback = object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult) {
                    observer.onNext(result)
                    if (type == BarCodeScanType.SINGLE) observer.onComplete()
                }

                override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
            }
        }

        override fun onDispose() {
            view.stopDecoding()
        }
    }
}