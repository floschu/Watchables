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

package at.florianschuster.watchables.ui.detail

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import at.florianschuster.watchables.util.extensions.inflate
import at.florianschuster.watchables.util.srcConsumer
import kotlinx.android.synthetic.main.item_detail_video.view.*
import android.content.Intent
import android.net.Uri
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import at.florianschuster.watchables.model.Videos
import at.florianschuster.watchables.util.photodetail.photoDetailConsumer
import kotlinx.android.synthetic.main.item_detail_poster.view.*


sealed class DetailMediaItem(open val id: String, @LayoutRes val layout: Int) {
    data class Poster(val thumbnail: String?, val original: String?) : DetailMediaItem("$thumbnail$original", R.layout.item_detail_poster)

    data class YoutubeVideo(override val id: String, val name: String, val key: String, val type: Videos.Video.Type) : DetailMediaItem(id, R.layout.item_detail_video) {
        @get:StringRes
        val typeResource: Int
            get() = when (type) {
                Videos.Video.Type.trailer -> R.string.video_type_trailer
                Videos.Video.Type.teaser -> R.string.video_type_teaser
                Videos.Video.Type.clip -> R.string.video_type_clip
                Videos.Video.Type.featurette -> R.string.video_type_featurette
                Videos.Video.Type.openingCredits -> R.string.video_type_openingCredits
            }
    }
}


class DetailMediaAdapter : ListAdapter<DetailMediaItem, DetailMediaAdapter.DetailViewHolder>(detailMediaItemDiff) {

    override fun getItemViewType(position: Int): Int = getItem(position).layout

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder = when (viewType) {
        R.layout.item_detail_poster -> DetailViewHolder.Poster(parent.inflate(viewType))
        else -> DetailViewHolder.Video(parent.inflate(viewType))
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) = when (holder) {
        is DetailViewHolder.Poster -> holder.bind(getItem(position) as DetailMediaItem.Poster)
        is DetailViewHolder.Video -> holder.bind(getItem(position) as DetailMediaItem.YoutubeVideo)
    }

    sealed class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class Poster(itemView: View) : DetailViewHolder(itemView) {
            fun bind(poster: DetailMediaItem.Poster) {
                itemView.ivImage.clipToOutline = true
                itemView.ivImage.srcConsumer(R.drawable.ic_logo).accept(poster.thumbnail)
                itemView.ivImage.setOnClickListener { itemView.context.photoDetailConsumer.accept(poster.original) }
            }
        }

        class Video(itemView: View) : DetailViewHolder(itemView) {
            fun bind(video: DetailMediaItem.YoutubeVideo) {
                val resources = itemView.context
                itemView.ivThumbnail.clipToOutline = true
                itemView.ivThumbnail.srcConsumer().accept(resources.getString(R.string.youtube_thumbnail, video.key))
                itemView.ivThumbnail.setOnClickListener {
                    val url = resources.getString(R.string.youtube_url, video.key)
                    itemView.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                itemView.videoType.setText(video.typeResource)
                itemView.fadeView.clipToOutline = true
            }
        }
    }
}


private val detailMediaItemDiff = object : DiffUtil.ItemCallback<DetailMediaItem>() {
    override fun areItemsTheSame(oldItem: DetailMediaItem, newItem: DetailMediaItem): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: DetailMediaItem, newItem: DetailMediaItem): Boolean = oldItem == newItem
}