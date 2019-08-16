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

package at.florianschuster.watchables.service

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import at.florianschuster.watchables.R
import at.florianschuster.watchables.all.PersistenceProvider
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.service.local.PrefRepo
import com.google.firebase.analytics.FirebaseAnalytics

interface AnalyticsService {
    var analyticsEnabled: Boolean

    fun logWatchableAdd(watchable: Watchable)
    fun logWatchableDelete(watchable: Watchable)
    fun logWatchableWatched(watchableId: String, watched: Boolean)
    fun logDeleteWorker(result: Boolean)
}

class FirebaseAnalyticsService(
    private val context: Context,
    private val persistenceProvider: PersistenceProvider
) : AnalyticsService {
    private val analytics = FirebaseAnalytics.getInstance(context)

    override var analyticsEnabled: Boolean
        get() = persistenceProvider.retrieve(PersistenceProvider.KEY.AnalyticsEnabled, true)
        set(value) {
            analytics.setAnalyticsCollectionEnabled(value)
            persistenceProvider.store(PersistenceProvider.KEY.AnalyticsEnabled, value)
        }

    override fun logWatchableAdd(watchable: Watchable) {
        if (!analyticsEnabled) return
        analytics.logEvent(context.getString(R.string.analytics_watchable_add_event), getWatchableBundle(watchable))
    }

    override fun logWatchableDelete(watchable: Watchable) {
        if (!analyticsEnabled) return
        analytics.logEvent(context.getString(R.string.analytics_watchable_delete_event), getWatchableBundle(watchable))
    }

    override fun logWatchableWatched(watchableId: String, watched: Boolean) {
        if (!analyticsEnabled) return
        val bundle = bundleOf(FirebaseAnalytics.Param.ITEM_ID to watchableId, "watched" to watched)
        analytics.logEvent(context.getString(R.string.analytics_watchable_watched_event), bundle)
    }

    override fun logDeleteWorker(result: Boolean) {
        if (!analyticsEnabled) return
        val bundle = bundleOf("result" to result)
        analytics.logEvent(context.getString(R.string.analytics_delete_worker), bundle)
    }

    private fun getWatchableBundle(watchable: Watchable): Bundle = bundleOf(
        FirebaseAnalytics.Param.ITEM_ID to watchable.id,
        FirebaseAnalytics.Param.ITEM_VARIANT to watchable.type.name
    )
}