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

import at.florianschuster.watchables.ui.watchables.filter.WatchableContainerFilterType
import at.florianschuster.watchables.ui.watchables.filter.WatchableContainerSortingType
import org.threeten.bp.LocalDate

class PrefRepo(
    private val persistenceProvider: PersistenceProvider
) {

    var onboardingSnackShown: Boolean
        get() = persistenceProvider.retrieve(PersistenceProvider.KEY.OnboardingSnackShown, false)
        set(value) {
            persistenceProvider.store(PersistenceProvider.KEY.OnboardingSnackShown, value)
        }

    var enjoyingAppDialogShownDate: LocalDate
        get() = LocalDate.ofEpochDay(persistenceProvider.retrieve(PersistenceProvider.KEY.EnjoyingDialogShownDate, LocalDate.now().plusDays(7).toEpochDay()))
        set(value) {
            persistenceProvider.store(PersistenceProvider.KEY.EnjoyingDialogShownDate, value.toEpochDay())
        }

    var rated: Boolean
        get() = persistenceProvider.retrieve(PersistenceProvider.KEY.RatedInPlayStore, false)
        set(value) {
            persistenceProvider.store(PersistenceProvider.KEY.RatedInPlayStore, value)
        }

    var watchableContainerSortingType: WatchableContainerSortingType
        get() {
            val stored = persistenceProvider.retrieve(PersistenceProvider.KEY.PreferredWatchablesContainerSortingType, 0)
            return when {
                stored < WatchableContainerSortingType.values().size -> {
                    WatchableContainerSortingType.values()[stored]
                }
                else -> WatchableContainerSortingType.ByWatched
            }
        }
        set(value) {
            persistenceProvider.store(PersistenceProvider.KEY.PreferredWatchablesContainerSortingType, value.ordinal)
        }

    var watchableContainerFilterType: WatchableContainerFilterType
        get() {
            val stored = persistenceProvider.retrieve(PersistenceProvider.KEY.PreferredWatchablesContainerFilterType, 0)
            return when {
                stored < WatchableContainerFilterType.values().size -> {
                    WatchableContainerFilterType.values()[stored]
                }
                else -> WatchableContainerFilterType.All
            }
        }
        set(value) {
            persistenceProvider.store(PersistenceProvider.KEY.PreferredWatchablesContainerFilterType, value.ordinal)
        }

    var watchableRatingsEnabled: Boolean
        get() = persistenceProvider.retrieve(PersistenceProvider.KEY.WatchableRatingsEnabled, true)
        set(value) {
            persistenceProvider.store(PersistenceProvider.KEY.WatchableRatingsEnabled, value)
        }

    var lastNewUpdateDialogVersionCode: Int
        get() = persistenceProvider.retrieve(PersistenceProvider.KEY.LastNewUpdateDialogVersionCode, 0)
        set(value) {
            persistenceProvider.store(PersistenceProvider.KEY.LastNewUpdateDialogVersionCode, value)
        }
}
