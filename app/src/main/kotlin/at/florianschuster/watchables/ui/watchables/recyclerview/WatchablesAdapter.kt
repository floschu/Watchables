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

package at.florianschuster.watchables.ui.watchables.recyclerview

import android.os.Bundle
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.ui.watchables.WatchableContainer
import com.jakewharton.rxrelay2.PublishRelay
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.tailoredapps.androidutil.ui.extensions.inflate

sealed class WatchablesAdapterInteraction {
    data class Options(val watchable: Watchable) : WatchablesAdapterInteraction()
    data class PhotoDetail(val url: String?) : WatchablesAdapterInteraction()
    data class ItemDetail(val watchable: Watchable) : WatchablesAdapterInteraction()
    data class Watched(val watchableId: String, val watched: Boolean) : WatchablesAdapterInteraction()
    data class WatchedEpisode(val seasonId: String, val episode: String, val watched: Boolean) : WatchablesAdapterInteraction()
    data class EpisodeOptions(val seasonId: String, val seasonIndex: String, val episodeIndex: String) : WatchablesAdapterInteraction()
}

class WatchablesAdapter : RecyclerView.Adapter<WatchableViewHolder>(), FastScrollRecyclerView.SectionedAdapter {
    val interaction: PublishRelay<WatchablesAdapterInteraction> = PublishRelay.create()
    var data: List<WatchableContainer> = emptyList()

    private val viewPool = RecyclerView.RecycledViewPool()

    override fun getItemCount(): Int = data.count()

    override fun getItemViewType(position: Int): Int = data[position].watchable.type.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchableViewHolder = when (viewType) {
        Watchable.Type.movie.ordinal -> WatchableViewHolder.Movie(parent.inflate(R.layout.item_watchable_movie))
        else -> WatchableViewHolder.Show(parent.inflate(R.layout.item_watchable_show), viewPool)
    }

    override fun onBindViewHolder(holder: WatchableViewHolder, position: Int) = holder.bind(data[position], interaction::accept)

    override fun onBindViewHolder(holder: WatchableViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) return onBindViewHolder(holder, position)
        (payloads[0] as? Bundle)?.let { bundle ->
            bundle.keySet()?.forEach {
                when {
                    it == DIFF_WATCHABLE && bundle.getBoolean(it) -> {
                        holder.bindWatchablePayload(data[position].watchable, interaction::accept)
                    }
                    it == DIFF_SEASONS && bundle.getBoolean(it) -> {
                        holder.bindSeasonsPayload(data[position].seasons, interaction::accept)
                    }
                }
            }
        }
    }

    override fun getSectionName(position: Int): String =
            "${data[position].watchable.name.firstOrNull()}"
}

private const val DIFF_WATCHABLE = "watchable"
private const val DIFF_SEASONS = "seasons"

infix fun WatchablesAdapter.calculateDiff(newData: List<WatchableContainer>): DiffUtil.DiffResult {
    return DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                data[oldItemPosition].watchable.id == newData[newItemPosition].watchable.id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                data[oldItemPosition] == newData[newItemPosition]

        override fun getOldListSize(): Int = data.size

        override fun getNewListSize(): Int = newData.size

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = data[oldItemPosition]
            val newItem = newData[newItemPosition]
            val watchableDiff = oldItem.watchable != newItem.watchable
            val episodesDiff = oldItem.seasons != newItem.seasons
            return if (watchableDiff && episodesDiff) null
            else bundleOf(DIFF_WATCHABLE to watchableDiff, DIFF_SEASONS to episodesDiff)
        }
    })
}