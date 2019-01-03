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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import at.florianschuster.watchables.util.extensions.inflate
import kotlinx.android.synthetic.main.item_option.view.*
import kotlinx.android.synthetic.main.item_option_toggle.view.*


sealed class Option(val title: Int, val icon: Int, val layout: Int) {
    class Action(@StringRes title: Int, @DrawableRes icon: Int, val action: () -> Unit) :
            Option(title, icon, R.layout.item_option)

    class Toggle(@StringRes title: Int, @DrawableRes icon: Int, val isToggled: Boolean, val toggled: (Boolean) -> Unit) :
            Option(title, icon, R.layout.item_option_toggle)
}


class OptionsAdapter : ListAdapter<Option, OptionsAdapter.MoreOptionViewHolder>(optionDiff) {

    override fun getItemViewType(position: Int): Int = getItem(position).layout

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoreOptionViewHolder = when (viewType) {
        R.layout.item_option -> MoreOptionViewHolder.Action(parent.inflate(viewType))
        else -> MoreOptionViewHolder.Toggle(parent.inflate(viewType))
    }

    override fun onBindViewHolder(holder: MoreOptionViewHolder, position: Int) {
        when (holder) {
            is MoreOptionViewHolder.Action -> holder.bind(getItem(position) as Option.Action)
            is MoreOptionViewHolder.Toggle -> holder.bind(getItem(position) as Option.Toggle)
        }
    }

    sealed class MoreOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class Action(itemView: View) : MoreOptionViewHolder(itemView) {
            fun bind(option: Option.Action) {
                itemView.ivIcon.setImageResource(option.icon)
                itemView.tvTitle.setText(option.title)
                itemView.setOnClickListener { option.action() }
            }
        }

        class Toggle(itemView: View) : MoreOptionViewHolder(itemView) {
            fun bind(option: Option.Toggle) {
                itemView.ivIconToggle.setImageResource(option.icon)
                itemView.sw.setText(option.title)
                itemView.sw.isChecked = option.isToggled
                itemView.sw.setOnCheckedChangeListener { _, checked -> option.toggled(checked) }
            }
        }
    }
}


private val optionDiff = object : DiffUtil.ItemCallback<Option>() {
    override fun areItemsTheSame(oldItem: Option, newItem: Option): Boolean = oldItem.title == newItem.title
    override fun areContentsTheSame(oldItem: Option, newItem: Option): Boolean =
            oldItem.title == newItem.title && oldItem.icon == newItem.icon && oldItem.layout == newItem.layout
}