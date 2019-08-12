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
package at.florianschuster.watchables.all.util

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.WatchableTimestamp
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import io.reactivex.Maybe
import io.reactivex.MaybeEmitter
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File

interface QrCodeService {
    fun generate(content: String): Maybe<File>
}

class ZXingQrCodeService(
    private val context: Context
) : QrCodeService {
    override fun generate(content: String): Maybe<File> = Maybe
        .create { emitter: MaybeEmitter<File> ->
            val metrics = context.resources.displayMetrics
            val size = when {
                metrics.widthPixels < metrics.heightPixels -> metrics.widthPixels * 0.4
                else -> metrics.heightPixels * 0.4
            }.toInt()

            val bitmap = generate(content, size, size)
            if (bitmap == null) {
                emitter.onComplete()
            } else {
                val file = File.createTempFile("$PREFIX${WatchableTimestamp.now}", EXTENSION, context.cacheDir)
                file.outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                emitter.onSuccess(file)
            }
        }
        .subscribeOn(Schedulers.io())

    private fun generate(content: String, width: Int, height: Int): Bitmap? {
        return try {
            val bitMatrix = QRCodeWriter()
                .encode(content, BarcodeFormat.QR_CODE, width, height)

            val qrWidth = bitMatrix.width
            val qrHeight = bitMatrix.height
            val pixels = IntArray(qrWidth * qrHeight)

            for (y in 0 until qrHeight) {
                val offset = y * qrWidth
                for (x in 0 until qrWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) {
                        ContextCompat.getColor(context, R.color.black)
                    } else {
                        ContextCompat.getColor(context, R.color.white_opacity_75)
                    }
                }
            }

            Bitmap.createBitmap(qrWidth, qrHeight, Bitmap.Config.ARGB_8888).also {
                it.setPixels(pixels, 0, width, 0, 0, qrWidth, qrHeight)
            }
        } catch (e: WriterException) {
            Timber.e(e)
            null
        }
    }

    companion object {
        private const val PREFIX = "qr_"
        private const val EXTENSION = ".png"
    }
}