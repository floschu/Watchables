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

package at.florianschuster.watchables.all

import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import com.tailoredapps.androidutil.ui.extensions.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_option.*
import kotlinx.android.synthetic.main.item_option_square.*
import kotlinx.android.synthetic.main.item_option_toggle.*
import kotlinx.android.synthetic.main.item_option_value.*

sealed class Option(val title: Int, val icon: Int?, val layout: Int) {
    val id: String get() = "$layout$title"

    class Action(@StringRes title: Int, @DrawableRes icon: Int?, val action: () -> Unit) :
        Option(title, icon, R.layout.item_option)

    class SquareAction(@StringRes title: Int, @DrawableRes icon: Int?, val action: () -> Unit) :
        Option(title, icon, R.layout.item_option_square)

    class Toggle(@StringRes title: Int, @DrawableRes icon: Int, val initialToggled: Boolean, val toggled: (Boolean) -> Unit) :
        Option(title, icon, R.layout.item_option_toggle)

    class Value(@StringRes title: Int, @DrawableRes icon: Int, val value: String, val clicked: () -> Unit) :
        Option(title, icon, R.layout.item_option_value)

    object Divider : Option(-1, -1, R.layout.item_option_divider)
}

class OptionsAdapter : ListAdapter<Option, OptionViewHolder>(optionDiff) {
    override fun getItemViewType(position: Int): Int = getItem(position).layout

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder = when (viewType) {
        R.layout.item_option -> OptionViewHolder.Action(parent.inflate(viewType))
        R.layout.item_option_square -> OptionViewHolder.SquareAction(parent.inflate(viewType))
        R.layout.item_option_toggle -> OptionViewHolder.Toggle(parent.inflate(viewType))
        R.layout.item_option_value -> OptionViewHolder.Value(parent.inflate(viewType))
        else -> OptionViewHolder.Divider(parent.inflate(viewType))
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        when (holder) {
            is OptionViewHolder.Action -> holder.bind(getItem(position) as Option.Action)
            is OptionViewHolder.Toggle -> holder.bind(getItem(position) as Option.Toggle)
            is OptionViewHolder.SquareAction -> holder.bind(getItem(position) as Option.SquareAction)
            is OptionViewHolder.Value -> holder.bind(getItem(position) as Option.Value)
        }
    }
}

private val optionDiff = object : DiffUtil.ItemCallback<Option>() {
    override fun areItemsTheSame(oldItem: Option, newItem: Option): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Option, newItem: Option): Boolean = false
}

sealed class OptionViewHolder(override val containerView: View) :
    RecyclerView.ViewHolder(containerView), LayoutContainer {

    class Action(containerView: View) : OptionViewHolder(containerView) {
        fun bind(option: Option.Action) {
            with(ivIcon) {
                visibility = if (option.icon != null) {
                    setImageResource(option.icon)
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            }
            tvTitle.setText(option.title)
            containerView.setOnClickListener { option.action() }
        }
    }

    class SquareAction(containerView: View) : OptionViewHolder(containerView) {
        fun bind(option: Option.SquareAction) {
            with(ivIconSquare) {
                visibility = if (option.icon != null) {
                    setImageResource(option.icon)
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            }
            tvTitleSquare.setText(option.title)
            containerView.setOnClickListener { option.action() }
        }
    }

    class Toggle(containerView: View) : OptionViewHolder(containerView) {
        fun bind(option: Option.Toggle) {
            with(ivIconToggle) {
                visibility = if (option.icon != null) {
                    setImageResource(option.icon)
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            }
            sw.setText(option.title)
            sw.isChecked = option.initialToggled
            sw.setOnCheckedChangeListener { _, checked -> option.toggled(checked) }
        }
    }

    class Value(containerView: View) : OptionViewHolder(containerView) {
        fun bind(option: Option.Value) {
            with(ivIconValue) {
                visibility = if (option.icon != null) {
                    setImageResource(option.icon)
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            }
            tvTitleValue.setText(option.title)
            tvValue.text = option.value
            containerView.setOnClickListener { option.clicked() }
        }
    }

    class Divider(containerView: View) : OptionViewHolder(containerView)
}
