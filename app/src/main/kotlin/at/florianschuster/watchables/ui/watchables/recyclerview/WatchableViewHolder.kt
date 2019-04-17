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

import android.view.View
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.WatchableContainer
import at.florianschuster.watchables.model.WatchableSeason
import at.florianschuster.watchables.model.originalPoster
import at.florianschuster.watchables.all.util.srcBlurConsumer
import at.florianschuster.watchables.all.util.srcConsumer
import at.florianschuster.watchables.model.thumbnailPoster
import io.reactivex.functions.Consumer
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_watchable.*
import kotlinx.android.synthetic.main.item_watchable_movie.*
import kotlinx.android.synthetic.main.item_watchable_show.*

sealed class WatchableViewHolder(override val containerView: View, protected val clickConsumer: Consumer<ItemClickType>) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    @CallSuper
    open fun bind(watchableContainer: WatchableContainer) {
        bindWatchablePayload(watchableContainer.watchable)
        bindSeasonsPayload(watchableContainer.seasons)
    }

    @CallSuper
    open fun bindWatchablePayload(watchable: Watchable) {
        containerView.setOnClickListener { clickConsumer.accept(ItemClickType.ItemDetail(watchable)) }
        containerView.setOnLongClickListener { clickConsumer.accept(ItemClickType.Options(watchable)); true }
        tvTitle.text = watchable.name
        tvType.setText(if (watchable.type == Watchable.Type.movie) R.string.display_name_movie else R.string.display_name_show)
        ivImage.clipToOutline = true
        ivImage.srcConsumer(R.drawable.ic_logo).accept(watchable.thumbnailPoster)
        ivImage.setOnClickListener { clickConsumer.accept(ItemClickType.PhotoDetail(watchable.originalPoster)) }
    }

    open fun bindSeasonsPayload(seasons: List<WatchableSeason>?) {}

    class Movie(
        containerView: View,
        clickConsumer: Consumer<ItemClickType>
    ) : WatchableViewHolder(containerView, clickConsumer) {

        override fun bindWatchablePayload(watchable: Watchable) {
            super.bindWatchablePayload(watchable)
            ivBackgroundMovie.srcBlurConsumer(R.drawable.ic_logo).accept(watchable.thumbnailPoster)
            tvWatched.setText(if (watchable.watched) R.string.watchable_watched else R.string.watchable_not_watched)
            ivWatched.isVisible = watchable.watched
            tvWatched.setOnClickListener { clickConsumer.accept(ItemClickType.Watched(watchable.id, !watchable.watched)) }
        }
    }

    class Show(
        containerView: View,
        clickConsumer: Consumer<ItemClickType>,
        private val viewPool: RecyclerView.RecycledViewPool
    ) : WatchableViewHolder(containerView, clickConsumer) {
        private val episodesAdapter = WatchableEpisodeAdapter(clickConsumer::accept)

        private var initialScroll = false

        init {
            rvEpisodes.apply {
                adapter = episodesAdapter
                setRecycledViewPool(viewPool)
            }
        }

        override fun bindWatchablePayload(watchable: Watchable) {
            super.bindWatchablePayload(watchable)
            ivBackgroundShow.srcBlurConsumer(R.drawable.ic_logo).accept(watchable.thumbnailPoster)
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
                    ?.let(rvEpisodes::scrollToPosition)
                    ?.also { initialScroll = true }
        }
    }
}