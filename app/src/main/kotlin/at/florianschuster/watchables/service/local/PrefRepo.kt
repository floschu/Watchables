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
import org.threeten.bp.LocalDate

interface PrefRepo {
    var analyticsEnabled: Boolean
    var onboardingSnackShown: Boolean
    var enjoyingAppDialogShownDate: LocalDate
}

class SharedPrefRepo(context: Context) : PrefRepo {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override var analyticsEnabled: Boolean
        get() = prefs.getBoolean(ANALYTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(ANALYTICS_ENABLED, value).apply()

    override var onboardingSnackShown: Boolean
        get() = prefs.getBoolean(ONBOARDING_SNACK, false)
        set(value) = prefs.edit().putBoolean(ONBOARDING_SNACK, value).apply()

    override var enjoyingAppDialogShownDate: LocalDate
        get() = LocalDate.ofEpochDay(prefs.getLong(ENJOYING_DIALOG_DATE, LocalDate.now().plusDays(7).toEpochDay()))
        set(value) = prefs.edit().putLong(ENJOYING_DIALOG_DATE, value.toEpochDay()).apply()

    companion object {
        private const val ANALYTICS_ENABLED = "analytics_enabled"
        private const val ONBOARDING_SNACK = "onboarding_snack"
        private const val ENJOYING_DIALOG_DATE = "enjoying_dialog_date"
    }
}
