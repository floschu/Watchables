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

package at.florianschuster.watchables.service

import android.content.Context
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.NetworkUnavailableError
import com.bumptech.glide.load.HttpException
import com.google.firebase.FirebaseException
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import timber.log.Timber
import java.io.IOException


class ErrorTranslationService(private val context: Context) {

    val toastConsumer: Consumer<in Throwable>
        get() = Consumer {
            Completable.fromAction { translate(it) }
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe()
        }

    private fun translate(throwable: Throwable): String =
            when (getErrorCause(throwable)) {
                ErrorCause.NETWORK_UNAVAILABLE -> {
                    Timber.w(throwable)
                    context.getString(R.string.error_network_unavailable)
                }
                ErrorCause.SERVER_UNAVAILABLE -> {
                    Timber.e(throwable)
                    context.getString(R.string.error_server_unavailable)
                }
                ErrorCause.SERVER_ERROR -> {
                    Timber.e(throwable)
                    context.getString(R.string.error_server_error, (throwable as HttpException).statusCode)
                }
                ErrorCause.PERMISSION_NOT_GRANTED -> context.getString(R.string.error_permission)
                ErrorCause.FIREBASE_ERROR -> {
                    Timber.e(throwable)
                    (throwable as FirebaseException).message
                            ?: context.getString(R.string.error_server_error_no_code)
                }
                else -> {
                    Timber.e(throwable)
                    context.getString(R.string.error_other)
                }
            }

    private fun getErrorCause(throwable: Throwable): ErrorCause = when (throwable) {
        is NetworkUnavailableError -> ErrorCause.NETWORK_UNAVAILABLE
        is IOException -> ErrorCause.SERVER_UNAVAILABLE
        is SecurityException -> ErrorCause.PERMISSION_NOT_GRANTED
        is FirebaseException -> ErrorCause.FIREBASE_ERROR
        else -> ErrorCause.OTHER
    }

    private enum class ErrorCause {
        PERMISSION_NOT_GRANTED,
        NETWORK_UNAVAILABLE,
        SERVER_UNAVAILABLE,
        SERVER_ERROR,
        FIREBASE_ERROR,
        OTHER
    }
}
