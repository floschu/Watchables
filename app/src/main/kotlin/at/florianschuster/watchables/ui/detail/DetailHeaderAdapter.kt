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
import at.florianschuster.watchables.all.util.srcConsumer
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import at.florianschuster.watchables.model.Videos
import at.florianschuster.watchables.all.util.photodetail.photoDetailConsumer
import com.tailoredapps.androidutil.ui.extensions.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_detail_poster.*
import kotlinx.android.synthetic.main.item_detail_qr.*
import kotlinx.android.synthetic.main.item_detail_video.*
import com.tailoredapps.androidutil.ui.extensions.afterMeasured
import androidx.core.view.isVisible
import at.florianschuster.watchables.all.util.GlideApp
import at.florianschuster.watchables.all.util.QrCodeService
import at.florianschuster.watchables.all.util.photodetail.bitmapDetailConsumer
import org.koin.core.KoinComponent
import org.koin.core.inject

sealed class DetailHeaderItem(
    open val id: String,
    @LayoutRes val layout: Int,
    private val ordinal: Int
) : Comparable<DetailHeaderItem> {
    data class QRCode(val deepLink: String) : DetailHeaderItem("-1", R.layout.item_detail_qr, 0)

    data class Poster(
        val thumbnail: String?,
        val original: String?
    ) : DetailHeaderItem("$thumbnail$original", R.layout.item_detail_poster, 1)

    data class YoutubeVideo(
        override val id: String,
        val name: String,
        val key: String,
        val type: Videos.Video.Type?
    ) : DetailHeaderItem(id, R.layout.item_detail_video, 2) {
        @get:StringRes
        val typeResource: Int
            get() = when (type) {
                Videos.Video.Type.teaser -> R.string.video_type_teaser
                Videos.Video.Type.clip -> R.string.video_type_clip
                Videos.Video.Type.featurette -> R.string.video_type_featurette
                Videos.Video.Type.openingCredits -> R.string.video_type_openingCredits
                else -> R.string.video_type_trailer
            }
    }

    override fun compareTo(other: DetailHeaderItem): Int = this.ordinal.compareTo(other.ordinal)
}

class DetailHeaderAdapter : ListAdapter<DetailHeaderItem, DetailHeaderViewHolder>(detailHeaderItemDiff) {

    override fun getItemViewType(position: Int): Int = getItem(position).layout

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailHeaderViewHolder = when (viewType) {
        R.layout.item_detail_poster -> DetailHeaderViewHolder.Poster(parent.inflate(viewType))
        R.layout.item_detail_qr -> DetailHeaderViewHolder.QRCode(parent.inflate(viewType))
        else -> DetailHeaderViewHolder.Video(parent.inflate(viewType))
    }

    override fun onBindViewHolder(holder: DetailHeaderViewHolder, position: Int) = when (holder) {
        is DetailHeaderViewHolder.Poster -> holder.bind(getItem(position) as DetailHeaderItem.Poster)
        is DetailHeaderViewHolder.Video -> holder.bind(getItem(position) as DetailHeaderItem.YoutubeVideo)
        is DetailHeaderViewHolder.QRCode -> holder.bind(getItem(position) as DetailHeaderItem.QRCode)
    }
}

private val detailHeaderItemDiff = object : DiffUtil.ItemCallback<DetailHeaderItem>() {
    override fun areItemsTheSame(oldItem: DetailHeaderItem, newItem: DetailHeaderItem): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: DetailHeaderItem, newItem: DetailHeaderItem): Boolean = oldItem == newItem
}

sealed class DetailHeaderViewHolder(final override val containerView: View) :
    RecyclerView.ViewHolder(containerView), LayoutContainer, KoinComponent {

    protected val qrCodeService: QrCodeService by inject()

    enum class FocusState(val alpha: Float) {
        Highlighted(1f),
        None(0.33f)
    }

    init {
        apply(FocusState.None)
    }

    fun apply(focusState: FocusState) {
        containerView.alpha = focusState.alpha
    }

    class QRCode(containerView: View) : DetailHeaderViewHolder(containerView) {
        fun bind(qr: DetailHeaderItem.QRCode) {
            ivQR.clipToOutline = true
            ivQR.afterMeasured {
                val bitmap = qrCodeService.generate(qr.deepLink, width, height)

                if (bitmap != null) {
                    GlideApp.with(ivQR).load(bitmap).into(ivQR)
                    ivQR.setOnClickListener {
                        containerView.context.bitmapDetailConsumer.accept(bitmap)
                    }
                } else {
                    containerView.isVisible = false
                }
            }
        }
    }

    class Poster(containerView: View) : DetailHeaderViewHolder(containerView) {
        fun bind(poster: DetailHeaderItem.Poster) {
            ivImage.clipToOutline = true
            ivImage.srcConsumer(R.drawable.ic_logo).accept(poster.thumbnail)
            poster.original?.let { originalPoster ->
                ivImage.setOnClickListener {
                    containerView.context.photoDetailConsumer.accept(originalPoster)
                }
            }
        }
    }

    class Video(containerView: View) : DetailHeaderViewHolder(containerView) {
        fun bind(video: DetailHeaderItem.YoutubeVideo) {
            val resources = containerView.context
            ivThumbnail.clipToOutline = true
            ivThumbnail.srcConsumer().accept(resources.getString(R.string.youtube_thumbnail, video.key))
            ivThumbnail.setOnClickListener {
                val url = resources.getString(R.string.youtube_url, video.key)
                containerView.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            videoType.setText(video.typeResource)
            fadeView.clipToOutline = true
        }
    }
}