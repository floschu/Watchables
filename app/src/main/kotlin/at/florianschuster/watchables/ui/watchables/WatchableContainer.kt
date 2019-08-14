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

package at.florianschuster.watchables.ui.watchables

import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.WatchableSeason
import at.florianschuster.watchables.service.WatchablesDataSource
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables

data class WatchableContainer(val watchable: Watchable, val seasons: List<WatchableSeason>?)

fun Watchable.convertToWatchableContainer(seasons: List<WatchableSeason>): WatchableContainer = when {
    type == Watchable.Type.movie || seasons.isEmpty() -> WatchableContainer(this, null)
    else -> {
        val seasonsWatched = seasons.all { it.episodes.all(Map.Entry<String, Boolean>::value) }
        if (watched == seasonsWatched) WatchableContainer(this, seasons)
        else WatchableContainer(this.apply { watched = seasonsWatched }, seasons)
    }
}

val WatchablesDataSource.watchableContainerObservable: Flowable<List<WatchableContainer>>
    get() {
        val watchablesObservable = watchablesObservable
                .startWith(emptyList<Watchable>())
        val watchableSeasonsObservable = watchableSeasonsObservable
                .startWith(emptyList<WatchableSeason>())

        return Flowables.combineLatest(watchablesObservable, watchableSeasonsObservable)
                .map { (watchables, seasons) ->
                    watchables.map { watchable ->
                        val watchableSeasons = seasons.asSequence()
                                .filter { it.watchableId == watchable.id }
                                .sortedBy { it.index }
                                .toList()
                        watchable.convertToWatchableContainer(watchableSeasons)
                    }
                }
                .skip(1)
    }