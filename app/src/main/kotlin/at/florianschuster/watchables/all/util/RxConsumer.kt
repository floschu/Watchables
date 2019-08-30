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

package at.florianschuster.watchables.all.util

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import io.reactivex.functions.Consumer
import java.io.File
import jp.wasabeef.glide.transformations.BlurTransformation

fun ImageView.srcFileConsumer(@DrawableRes fallback: Int? = null): Consumer<in File?> =
    Consumer {
        GlideApp.with(this)
            .load(it)
            .apply { fallback?.let(::error) }
            .transition(withCrossFade())
            .into(this)
    }

fun ImageView.srcConsumer(@DrawableRes fallback: Int? = null): Consumer<in String?> =
        Consumer {
            GlideApp.with(this)
                    .load(it)
                    .apply { fallback?.let(::error) }
                    .transition(withCrossFade())
                    .into(this)
        }

fun ImageView.srcBlurConsumer(@DrawableRes fallback: Int? = null): Consumer<in String?> =
        Consumer {
            GlideApp.with(this)
                    .load(it ?: fallback)
                    .apply { fallback?.let(::error) }
                    .transition(withCrossFade())
                    .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 5)))
                    .into(this)
        }
