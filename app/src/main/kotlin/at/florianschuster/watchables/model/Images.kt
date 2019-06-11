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

package at.florianschuster.watchables.model

import at.florianschuster.watchables.BuildConfig

enum class Images(private val url: String?) {
    thumbnail(BuildConfig.MOVIEDB_POSTER_THUMBNAIL),
    original(BuildConfig.MOVIEDB_POSTER_ORIGINAL);

    fun from(posterPath: String?): String? = posterPath?.let { "$url$posterPath" }
}

val Watchable.thumbnailPoster: String?
    get() = Images.thumbnail.from(this.posterPath)

val Watchable.originalPoster: String?
    get() = Images.original.from(this.posterPath)

val Search.Result.thumbnailPoster: String?
    get() = Images.thumbnail.from(this.posterPath)

val Search.Result.originalPoster: String?
    get() = Images.original.from(this.posterPath)
