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

package at.florianschuster.watchables.service

import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.WatchableSeason
import at.florianschuster.watchables.model.WatchableUser
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface WatchablesDataSource {
    val watchableUser: Single<WatchableUser>
    val watchableUserObservable: Flowable<WatchableUser>
    fun createUser(user: WatchableUser): Completable

    val watchablesObservable: Flowable<List<Watchable>>
    fun watchable(id: String): Single<Watchable>
    fun watchableObservable(id: String): Flowable<Watchable>
    fun createWatchable(watchable: Watchable): Single<Watchable>
    fun updateWatchable(watchableId: String, watched: Boolean): Completable
    fun setWatchableDeleted(watchableId: String): Completable

    val watchableSeasonsObservable: Flowable<List<WatchableSeason>>
    fun season(id: String): Single<WatchableSeason>
    fun createSeason(season: WatchableSeason): Single<WatchableSeason>
    fun updateSeason(seasonId: String, watched: Boolean): Completable
    fun updateSeasonEpisode(seasonId: String, episode: String, watched: Boolean): Completable

    val watchablesToUpdate: Single<List<Watchable>>

    val watchablesToDelete: Single<List<Watchable>>
    fun deleteWatchable(watchableId: String): Completable
    val watchableSeasonsToDelete: Single<List<WatchableSeason>>
    fun deleteWatchableSeason(seasonId: String): Completable
}
