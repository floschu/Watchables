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

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.ui.watchables.WatchableContainer
import com.jakewharton.rxrelay2.PublishRelay
import com.tailoredapps.androidutil.ui.extensions.inflate
import io.reactivex.Observable
import io.reactivex.functions.Consumer

sealed class WatchablesAdapterInteraction {
    data class Options(val watchable: Watchable) : WatchablesAdapterInteraction()
    data class PhotoDetail(val url: String?) : WatchablesAdapterInteraction()
    data class ItemDetail(val watchable: Watchable) : WatchablesAdapterInteraction()
    data class Watched(val watchableId: String, val watched: Boolean) : WatchablesAdapterInteraction()
    data class WatchedEpisode(val seasonId: String, val episode: String, val watched: Boolean) : WatchablesAdapterInteraction()
    data class EpisodeOptions(val seasonId: String, val seasonIndex: String, val episodeIndex: String) : WatchablesAdapterInteraction()
}

class WatchablesAdapter : RecyclerView.Adapter<WatchableViewHolder>() {
    private val interactionRelay: PublishRelay<WatchablesAdapterInteraction> = PublishRelay.create()
    val interaction: Observable<WatchablesAdapterInteraction> = interactionRelay.hide()

    private var data: List<WatchableContainer> = emptyList()
    val dataConsumer: Consumer<Pair<List<WatchableContainer>, DiffUtil.DiffResult?>> =
        Consumer { (newData, diff) ->
            data = newData
            if (diff != null) {
                diff.dispatchUpdatesTo(this)
            } else {
                notifyDataSetChanged()
            }
        }

    private val viewPool = RecyclerView.RecycledViewPool()

    override fun getItemCount(): Int = data.count()

    override fun getItemViewType(position: Int): Int = data[position].watchable.type.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchableViewHolder = when (viewType) {
        Watchable.Type.movie.ordinal -> WatchableViewHolder.Movie(parent.inflate(R.layout.item_watchable_movie))
        else -> WatchableViewHolder.Show(parent.inflate(R.layout.item_watchable_show), viewPool)
    }

    override fun onBindViewHolder(holder: WatchableViewHolder, position: Int) =
        holder.bind(data[position], interactionRelay::accept)

    override fun onBindViewHolder(holder: WatchableViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }
        val payLoadPair = payloads.firstOrNull() as? Pair<*, *> ?: return
        val (watchableDiff, episodesDiff) = payLoadPair

        if (watchableDiff is Boolean && watchableDiff) {
            holder.bindWatchablePayload(data[position].watchable, interactionRelay::accept)
        }
        if (episodesDiff is Boolean && episodesDiff) {
            holder.bindSeasonsPayload(data[position].seasons, interactionRelay::accept)
        }
    }

    companion object {
        fun diff(
            oldData: List<WatchableContainer>,
            newData: List<WatchableContainer>
        ): DiffUtil.Callback = object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldData[oldItemPosition].watchable.id == newData[newItemPosition].watchable.id

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldData[oldItemPosition] == newData[newItemPosition]

            override fun getOldListSize(): Int = oldData.size

            override fun getNewListSize(): Int = newData.size

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                val oldItem = oldData[oldItemPosition]
                val newItem = newData[newItemPosition]
                val watchableDiff = oldItem.watchable != newItem.watchable
                val episodesDiff = oldItem.seasons != newItem.seasons
                return if (watchableDiff && episodesDiff) null else watchableDiff to episodesDiff
            }
        }
    }
}
