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
import com.orhanobut.hawk.Hawk

interface PersistenceProvider {
    enum class KEY {
        AnalyticsEnabled,
        OnboardingSnackShown,
        EnjoyingDialogShownDate,
        RatedInPlayStore,
        PreferredWatchablesContainerSortingType,
        PreferredWatchablesContainerFilterType,
        WatchableRatingsEnabled,
        LastNewUpdateDialogVersionCode
    }

    fun <T> store(key: KEY, value: T): Boolean
    fun <T> retrieve(key: KEY, default: T? = null): T
    fun delete(key: KEY): Boolean
    fun deleteAll(): Boolean
}

class HawkPersistenceProvider(context: Context) : PersistenceProvider {
    init {
        Hawk.init(context).build()
    }

    override fun <T> store(key: PersistenceProvider.KEY, value: T): Boolean = Hawk.put<T>(key.name, value)
    override fun <T> retrieve(key: PersistenceProvider.KEY, default: T?): T = Hawk.get<T>(key.name, default)
    override fun delete(key: PersistenceProvider.KEY): Boolean = Hawk.delete(key.name)
    override fun deleteAll(): Boolean = Hawk.deleteAll()
}
