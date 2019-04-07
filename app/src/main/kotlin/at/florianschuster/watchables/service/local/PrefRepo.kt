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
import at.florianschuster.watchables.ui.watchables.WatchableContainerSortingType
import com.orhanobut.hawk.Hawk
import org.threeten.bp.LocalDate

interface PrefRepo {
    var analyticsEnabled: Boolean
    var onboardingSnackShown: Boolean
    var enjoyingAppDialogShownDate: LocalDate
    var watchableContainerSortingType: WatchableContainerSortingType
}

class HawkPrefRepo(context: Context) : PrefRepo {
    init {
        Hawk.init(context).build()
    }

    override var analyticsEnabled: Boolean
        get() = Hawk.get(ANALYTICS_ENABLED, true)
        set(value) {
            Hawk.put(ANALYTICS_ENABLED, value)
        }

    override var onboardingSnackShown: Boolean
        get() = Hawk.get(ONBOARDING_SNACK, false)
        set(value) {
            Hawk.put(ONBOARDING_SNACK, value)
        }

    override var enjoyingAppDialogShownDate: LocalDate
        get() = LocalDate.ofEpochDay(Hawk.get(ENJOYING_DIALOG_DATE, LocalDate.now().plusDays(7).toEpochDay()))
        set(value) {
            Hawk.put(ENJOYING_DIALOG_DATE, value.toEpochDay())
        }

    override var watchableContainerSortingType: WatchableContainerSortingType
        get() = WatchableContainerSortingType.values()[Hawk.get(WATCHABLES_CONTAINER_SORTING_TYPE, 0)]
        set(value) {
            Hawk.put(WATCHABLES_CONTAINER_SORTING_TYPE, value.ordinal)
        }

    companion object {
        private const val ANALYTICS_ENABLED = "analytics_enabled"
        private const val ONBOARDING_SNACK = "onboarding_snack"
        private const val ENJOYING_DIALOG_DATE = "enjoying_dialog_date"
        private const val WATCHABLES_CONTAINER_SORTING_TYPE = "watchables_container_sorting_type"
    }
}
