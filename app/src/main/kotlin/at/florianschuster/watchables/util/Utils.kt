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

package at.florianschuster.watchables.util

import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import android.content.Context
import at.florianschuster.watchables.R
import at.florianschuster.watchables.WatchablesApp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.tailoredapps.androidutil.ui.IntentUtil

object Utils {
    fun rateApp(context: Context): Intent {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${WatchablesApp.instance.packageName}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        return try {
            marketIntent
        } catch (e: ActivityNotFoundException) {
            IntentUtil.playstore(context)
        }
    }

    fun showLibraries(context: Context) {
        LibsBuilder().apply {
            withFields(*Libs.toStringArray(R.string::class.java.fields))
            withVersionShown(true)
            withLicenseShown(true)
            withAutoDetect(true)
            withAboutAppName(context.getString(R.string.app_name))
            withAboutIconShown(true)
            withAboutVersionShown(true)
            withActivityTitle(context.getString(R.string.settings_licenses))
            withActivityStyle(Libs.ActivityStyle.DARK)
            withAboutDescription(context.getString(R.string.dev_info))
            withLibraries(
                    "LeakCanary", "Timber", "gson", "rxjava", "rxAndroid",
                    "glide", "SupportLibrary", "rxpaparazzo", "transformations",
                    "fastscroll", "rxbinding", "koin", "flick", "koordinator",
                    "reaktor"
            )
        }.start(context)
    }
}
