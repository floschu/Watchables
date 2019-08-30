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

package at.florianschuster.watchables.all.util.extensions

import android.content.res.Resources
import androidx.annotation.StringRes
import at.florianschuster.watchables.BuildConfig
import at.florianschuster.watchables.R
import com.tailoredapps.androidutil.network.NetworkUnavailableException
import retrofit2.HttpException

fun Throwable.asCause(@StringRes customOnOtherStringResource: Int? = null): Cause {
    return Cause.fromThrowable(this, customOnOtherStringResource)
}

fun Throwable.asCauseTranslation(resources: Resources, @StringRes customOnOtherStringResource: Int? = null): String {
    return asCause(customOnOtherStringResource).translation(resources).let { translation ->
        when {
            BuildConfig.DEBUG -> "$translation - ${this::class.java.simpleName}"
            else -> translation
        }
    }
}

sealed class Cause(@StringRes val default: Int = R.string.error_other) {
    data class Other(private val otherRes: Int?) : Cause() {
        override fun translation(resources: Resources): String = when {
            otherRes != null -> resources.getString(otherRes)
            else -> super.translation(resources)
        }
    }

    object NetworkUnavailable : Cause(R.string.error_network_unavailable)

    class Http(private val code: Int) : Cause() {
        override fun translation(resources: Resources): String = when (code) {
            401 -> resources.getString(R.string.error_not_authenticated)
            else -> resources.getString(R.string.error_server_error, code)
        }
    }

    object Permission : Cause(R.string.error_permission)

    // override this to show different translation per Cause
    open fun translation(resources: Resources): String = resources.getString(default)

    companion object {
        fun fromThrowable(throwable: Throwable, @StringRes otherRes: Int? = null): Cause = when (throwable) {
            is NetworkUnavailableException -> NetworkUnavailable
            is HttpException -> Http(throwable.code())
            is SecurityException -> Permission
            else -> Other(otherRes)
        }
    }
}
