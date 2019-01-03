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

package at.florianschuster.watchables.service.local

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager


interface PrefRepo {
    var analyticsEnabled: Boolean
}


class SharedPrefRepo(context: Context) : PrefRepo {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override var analyticsEnabled: Boolean
        get() = prefs.getBoolean(ANALYTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(ANALYTICS_ENABLED, value).apply()

    companion object {
        private const val ANALYTICS_ENABLED = "analytics_enabled"
        private const val INTERAL = "watchables_interval"
    }
}
