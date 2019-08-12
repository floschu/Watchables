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

import android.annotation.SuppressLint
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tailoredapps.androidutil.ui.IntentUtil
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import at.florianschuster.watchables.R
import com.tailoredapps.androidutil.async.Async
import com.tailoredapps.androidutil.permissions.Permission
import com.tailoredapps.androidutil.ui.extensions.toast
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.annotations.Experimental

@SuppressLint("CheckResult")
fun main(afterMillis: Long = 0, block: () -> Unit) {
    Completable.timer(afterMillis, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(block)
}

fun FragmentActivity.openChromeTab(url: String) {
    if (url.isEmpty()) return
    try {
        val tabsIntent = CustomTabsIntent.Builder().apply {
            setToolbarColor(ContextCompat.getColor(this@openChromeTab, R.color.colorPrimary))
            setShowTitle(true)
            enableUrlBarHiding()
        }.build()
        tabsIntent.launchUrl(this, Uri.parse(url))
    } catch (throwable: Throwable) {
        Timber.w(throwable)
        try {
            startActivity(IntentUtil.web(url)) // fallback of chrome not installed -> open default browser
        } catch (anotherOne: Throwable) {
            Timber.w(anotherOne)
            toast(R.string.error_other)
        }
    }
}

fun Fragment.openChromeTab(url: String) {
    requireActivity().openChromeTab(url)
}

fun RxPermissions.request(permission: Permission): Single<Boolean> {
    return request(permission.manifestPermission)
        .first(false)
        .onErrorReturn { false }
}

@Deprecated(message="Replace with AndroidAppUtil.Async if v16 is available.")
@Experimental
@CheckReturnValue
fun <T : Any> Maybe<T>.mapToAsync(onComplete: () -> Async<T> = { Async.Error(NoSuchElementException()) }): Single<Async<T>> {
    return materialize()
        .flatMap {
            val value = it.value
            val error = it.error
            when {
                it.isOnNext && value != null -> Single.just(Async.Success(value))
                it.isOnError && error != null -> Single.just(Async.Error(error))
                it.isOnComplete -> Single.just(onComplete())
                else -> Single.never()
            }
        }
}