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
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import at.florianschuster.watchables.R
import at.florianschuster.watchables.WatchablesApp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.tailoredapps.androidutil.permissions.Permission
import com.tailoredapps.androidutil.ui.IntentUtil
import com.tailoredapps.androidutil.ui.extensions.toast
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import timber.log.Timber

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

fun Fragment.showLibraries() {
    LibsBuilder().apply {
        withActivityTitle(requireContext().getString(R.string.more_licenses))
        withActivityStyle(Libs.ActivityStyle.DARK)
        withCheckCachedDetection(false)
        withLicenseShown(true)
        withAutoDetect(true)
    }.start(requireContext())
}

fun FragmentActivity.openAppInPlayStore() {
    try {
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${WatchablesApp.instance.packageName}")
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
    } catch (e: ActivityNotFoundException) {
        IntentUtil.playstore(this)
    }.let(::startActivity)
}

fun Fragment.openAppInPlayStore() {
    requireActivity().openAppInPlayStore()
}

fun Fragment.openCalendarWithEvent(title: String, description: String, date: LocalDate) {
    startActivity(Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        val millis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, millis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, millis)
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
    })
}

fun RxPermissions.request(permission: Permission): Single<Boolean> {
    return request(permission.manifestPermission)
        .first(false)
        .onErrorReturn { false }
}
