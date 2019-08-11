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
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

interface QrCodeService {
    fun generateFullScreen(content: String): Bitmap?
    fun generate(content: String, width: Int, height: Int): Bitmap?
}

class ZXingQrCodeService(
    private val context: Context
) : QrCodeService {
    override fun generateFullScreen(content: String): Bitmap? {
        val metrics = context.resources.displayMetrics
        val size = when {
            metrics.widthPixels < metrics.heightPixels -> metrics.widthPixels * 0.4
            else -> metrics.heightPixels * 0.4
        }.toInt()
        return generate(content, size, size)
    }

    override fun generate(content: String, width: Int, height: Int): Bitmap? {
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
                        ContextCompat.getColor(context, R.color.white_opacity_90)
                    }
                }
            }

            Bitmap.createBitmap(qrWidth, qrHeight, Bitmap.Config.ARGB_8888).also {
                it.setPixels(pixels, 0, width, 0, 0, qrWidth, qrHeight)
            }
        } catch (e: WriterException) {
            null
        }
    }
}