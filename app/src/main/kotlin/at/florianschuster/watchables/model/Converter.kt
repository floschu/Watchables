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

fun Search.Result.toWatchableType(): Watchable.Type = when (this) {
    is Search.Result.Movie -> Watchable.Type.movie
    is Search.Result.Show -> Watchable.Type.show
}

fun Movie.convertToWatchable(): Watchable = Watchable(
        false,
        name,
        Watchable.Type.movie,
        image,
        durationInMinutes,
        watchableStatus()
).apply { id = "${this@convertToWatchable.id}" }

private fun Movie.watchableStatus(): Watchable.Status = when (status) {
    Movie.Status.rumored -> Watchable.Status.running
    Movie.Status.planned -> Watchable.Status.running
    Movie.Status.inProduction -> if (releaseDate == null) Watchable.Status.running else Watchable.Status.finished
    Movie.Status.postProduction -> if (releaseDate == null) Watchable.Status.running else Watchable.Status.finished
    Movie.Status.released -> Watchable.Status.finished
    Movie.Status.canceled -> Watchable.Status.finished
}

fun Show.convertToWatchable(): Watchable = Watchable(
        false,
        name,
        Watchable.Type.show,
        image,
        if (episodeRuntimes.isEmpty()) null else episodeRuntimes.average().toLong(),
        watchableStatus()
).apply { id = "${this@convertToWatchable.id}" }

private fun Show.watchableStatus(): Watchable.Status = when (status) {
    Show.Status.returningSeries -> Watchable.Status.running
    Show.Status.planned -> Watchable.Status.running
    Show.Status.inProduction -> Watchable.Status.running
    Show.Status.ended -> Watchable.Status.finished
    Show.Status.canceled -> Watchable.Status.finished
    Show.Status.pilot -> Watchable.Status.running
}

fun Season.convertToWatchableSeason(watchableId: String): WatchableSeason = WatchableSeason(
        watchableId,
        index,
        image,
        episodes.sortedBy { it.episodeIndex }.associate { "${it.episodeIndex}" to false }
).apply { id = "${this@convertToWatchableSeason.id}" }