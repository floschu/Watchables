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
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import com.jakewharton.rxbinding3.view.visibility
import com.tailoredapps.androidutil.ui.extensions.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_watchable_show_episode.*

data class WatchableEpisode(val seasonId: String, val seasonIndex: String, val episode: String, val watched: Boolean) {
    val id = "${seasonId}S${seasonIndex}E$episode"
}

class WatchableEpisodeAdapter(
    private val itemClick: (ItemClickType) -> Unit
) : ListAdapter<WatchableEpisode, WatchableEpisodeAdapter.WatchableEpisodeViewHolder>(episodesDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchableEpisodeViewHolder = WatchableEpisodeViewHolder(parent.inflate(R.layout.item_watchable_show_episode))
    override fun onBindViewHolder(holder: WatchableEpisodeViewHolder, position: Int) = holder.bind(getItem(position))

    inner class WatchableEpisodeViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(watchableEpisode: WatchableEpisode) {
            tvEpisode.text = containerView.resources.getString(R.string.episode_name, watchableEpisode.seasonIndex, watchableEpisode.episode)
            tvEpisode.setOnClickListener {
                itemClick.invoke(ItemClickType.WatchedEpisode(watchableEpisode.seasonId, watchableEpisode.episode, !watchableEpisode.watched))
            }
            tvEpisode.setOnLongClickListener {
                itemClick.invoke(ItemClickType.EpisodeOptions(watchableEpisode.seasonId, watchableEpisode.seasonIndex, watchableEpisode.episode))
                true
            }
            ivWatched.visibility(View.INVISIBLE).accept(watchableEpisode.watched)
        }
    }
}

private val episodesDiff = object : DiffUtil.ItemCallback<WatchableEpisode>() {
    override fun areItemsTheSame(oldItem: WatchableEpisode, newItem: WatchableEpisode): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: WatchableEpisode, newItem: WatchableEpisode): Boolean = oldItem == newItem
}