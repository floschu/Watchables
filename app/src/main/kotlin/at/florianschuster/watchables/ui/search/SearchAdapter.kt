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

package at.florianschuster.watchables.ui.search

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.Search
import at.florianschuster.watchables.model.original
import at.florianschuster.watchables.model.thumbnail
import at.florianschuster.watchables.util.srcBlurConsumer
import at.florianschuster.watchables.util.srcConsumer
import com.jakewharton.rxrelay2.PublishRelay
import com.tailoredapps.androidutil.core.extensions.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_search.*

class SearchAdapter : ListAdapter<Search.SearchItem, SearchAdapter.SearchViewHolder>(searchDiff) {
    val addClick = PublishRelay.create<Search.SearchItem>()
    val imageClick = PublishRelay.create<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder = SearchViewHolder(parent.inflate(R.layout.item_search))
    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) = holder.bind(getItem(position))

    inner class SearchViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: Search.SearchItem) {
            containerView.setOnClickListener { addClick.accept(item) }
            tvTitle.text = item.title

            tvType.setText(if (item.type == Search.SearchItem.Type.movie) R.string.display_name_movie else R.string.display_name_show)

            ivImage.clipToOutline = true
            ivImage.srcConsumer(R.drawable.ic_logo).accept(item.thumbnail)
            ivBackground.srcBlurConsumer(R.drawable.ic_logo).accept(item.thumbnail)

            ivImage.setOnClickListener { imageClick.accept(item.original) }

            ivAdd.setImageResource(if (item.added) R.drawable.ic_check else R.drawable.ic_add)
            val color = ContextCompat.getColor(containerView.context, if (item.added) R.color.colorAccent else android.R.color.white)
            ivAdd.setColorFilter(color)
        }
    }
}

private val searchDiff = object : DiffUtil.ItemCallback<Search.SearchItem>() {
    override fun areItemsTheSame(oldItem: Search.SearchItem, newItem: Search.SearchItem): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Search.SearchItem, newItem: Search.SearchItem): Boolean = oldItem == newItem
}