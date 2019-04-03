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

package at.florianschuster.watchables.util.extensions

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import at.florianschuster.watchables.R
import com.tailoredapps.androidutil.ui.IntentUtil
import timber.log.Timber

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