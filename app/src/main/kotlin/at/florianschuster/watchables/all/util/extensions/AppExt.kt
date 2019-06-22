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
import com.tailoredapps.androidutil.permissions.Permission
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Single

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
        startActivity(IntentUtil.web(url)) // fallback of chrome not installed -> open default browser
    }
}

fun Fragment.openChromeTab(url: String) {
    val activity = this.activity
        ?: throw RuntimeException("No Activity attached to Fragment. Cannot show Dialog.")
    activity.openChromeTab(url)
}

fun RxPermissions.request(permission: Permission): Single<Boolean> {
    return request(permission.manifestPermission)
        .first(false)
        .onErrorReturn { false }
}