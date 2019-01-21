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

package at.florianschuster.watchables.ui.watchables

import android.view.View
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.*
import at.florianschuster.watchables.util.srcBlurConsumer
import at.florianschuster.watchables.util.srcConsumer
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.item_watchable.view.*
import kotlinx.android.synthetic.main.item_watchable_movie.view.*
import kotlinx.android.synthetic.main.item_watchable_show.view.*


sealed class WatchableViewHolder(itemView: View, protected val clickConsumer: Consumer<ItemClickType>) : RecyclerView.ViewHolder(itemView) {

    @CallSuper
    open fun bind(watchableContainer: WatchableContainer) {
        bindWatchablePayload(watchableContainer.watchable)
        bindSeasonsPayload(watchableContainer.seasons)
    }

    @CallSuper
    open fun bindWatchablePayload(watchable: Watchable) {
        itemView.setOnClickListener { clickConsumer.accept(ItemClickType.ItemDetail(watchable)) }
        itemView.setOnLongClickListener { clickConsumer.accept(ItemClickType.Options(watchable)); true }
        itemView.tvTitle.text = watchable.name
        itemView.tvType.setText(if (watchable.type == Watchable.Type.movie) R.string.display_name_movie else R.string.display_name_show)
        itemView.ivImage.clipToOutline = true
        itemView.ivImage.srcConsumer(R.drawable.ic_logo).accept(watchable.thumbnail)
        itemView.ivImage.setOnClickListener { clickConsumer.accept(ItemClickType.PhotoDetail(watchable.original)) }
    }

    open fun bindSeasonsPayload(seasons: List<WatchableSeason>?) {}

    class Movie(
            itemView: View,
            clickConsumer: Consumer<ItemClickType>
    ) : WatchableViewHolder(itemView, clickConsumer) {

        override fun bindWatchablePayload(watchable: Watchable) {
            super.bindWatchablePayload(watchable)
            itemView.ivBackgroundMovie.srcBlurConsumer(R.drawable.ic_logo).accept(watchable.thumbnail)
            itemView.tvWatched.setText(if (watchable.watched) R.string.watchable_watched else R.string.watchable_not_watched)
            itemView.ivWatched.isVisible = watchable.watched
            itemView.tvWatched.setOnClickListener { clickConsumer.accept(ItemClickType.Watched(watchable.id, !watchable.watched)) }
        }
    }

    class Show(
            itemView: View,
            clickConsumer: Consumer<ItemClickType>,
            private val viewPool: RecyclerView.RecycledViewPool
    ) : WatchableViewHolder(itemView, clickConsumer) {
        private val episodesAdapter = WatchableEpisodeAdapter(clickConsumer::accept)

        private var initialScroll = false

        init {
            itemView.rvEpisodes.apply {
                adapter = episodesAdapter
                setRecycledViewPool(viewPool)
            }
        }

        override fun bindWatchablePayload(watchable: Watchable) {
            super.bindWatchablePayload(watchable)
            itemView.ivBackgroundShow.srcBlurConsumer(R.drawable.ic_logo).accept(watchable.thumbnail)
        }

        override fun bindSeasonsPayload(seasons: List<WatchableSeason>?) {
            val episodes = seasons
                    ?.flatMap { season ->
                        season.episodes.map { WatchableEpisode(season.id, season.index.toString(), it.key, it.value) }
                    }
                    ?.sortedWith(compareBy({ it.seasonIndex.toInt() }, { it.episode.toInt() }))
                    .also(episodesAdapter::submitList)

            initialScroll.takeIf { !it }
                    ?.let { episodes }
                    ?.indexOfFirst { !it.watched }
                    ?.takeIf { it >= 0 }
                    ?.let { if (it - 1 >= 0) it - 1 else it }
                    ?.let(itemView.rvEpisodes::scrollToPosition)
                    ?.also { initialScroll = true }
        }
    }
}