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

package at.florianschuster.watchables.service.remote

import at.florianschuster.watchables.model.WatchableUser
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.WatchableSeason
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.all.util.extensions.RxTasks
import at.florianschuster.watchables.all.util.extensions.createObject
import at.florianschuster.watchables.all.util.extensions.localObject
import at.florianschuster.watchables.all.util.extensions.localObjectList
import at.florianschuster.watchables.all.util.extensions.localObjectListObservable
import at.florianschuster.watchables.all.util.extensions.localObjectObservable
import at.florianschuster.watchables.all.util.extensions.updateField
import at.florianschuster.watchables.all.util.extensions.updateNestedField
import at.florianschuster.watchables.all.util.extensions.updateObject
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers

interface WatchablesApi {
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

class FirebaseWatchablesApi(
        private val sessionService: SessionService<FirebaseUser, AuthCredential>
) : WatchablesApi {
    private val fireStore = FirebaseFirestore.getInstance()

    private val dbVersion = "v2"
    private fun FirebaseFirestore.users(): CollectionReference =
            collection("$dbVersion/database/users")

    private fun FirebaseFirestore.user(userId: String): DocumentReference =
            document("$dbVersion/database/users/$userId")

    private fun FirebaseFirestore.watchables(userId: String): CollectionReference =
            collection("$dbVersion/database/users/$userId/watchables")

    private fun FirebaseFirestore.seasons(userId: String): CollectionReference =
            collection("$dbVersion/database/users/$userId/seasons")

    override val watchableUser: Single<WatchableUser>
        get() = sessionService.user
                .map { fireStore.user(it.uid) }
                .flatMap { it.localObject<WatchableUser>() }
                .subscribeOn(Schedulers.io())

    override val watchableUserObservable: Flowable<WatchableUser>
        get() = sessionService.user
                .map { fireStore.user(it.uid) }
                .flatMapPublisher { it.localObjectObservable<WatchableUser>() }
                .subscribeOn(Schedulers.io())

    override fun createUser(user: WatchableUser): Completable = sessionService.user
            .map { fireStore.users() }
            .flatMapCompletable { it.createObject(user) }
            .subscribeOn(Schedulers.io())

    override val watchablesObservable: Flowable<List<Watchable>> = sessionService.user
            .map { fireStore.watchables(it.uid).whereEqualTo(Watchable::deleted.name, false) }
            .flatMapPublisher { it.localObjectListObservable<Watchable>() }
            .subscribeOn(Schedulers.io())

    override fun watchable(id: String): Single<Watchable> = sessionService.user
            .map { fireStore.watchables(it.uid).document(id) }
            .flatMap { it.localObject<Watchable>() }
            .subscribeOn(Schedulers.io())

    override fun watchableObservable(id: String): Flowable<Watchable> = sessionService.user
            .map { fireStore.watchables(it.uid).document(id) }
            .flatMapPublisher { it.localObjectObservable<Watchable>() }
            .subscribeOn(Schedulers.io())

    override fun createWatchable(watchable: Watchable): Single<Watchable> = sessionService.user
            .map { fireStore.watchables(it.uid) }
            .flatMapCompletable { it.createObject(watchable) }
            .toSingleDefault(watchable)
            .subscribeOn(Schedulers.io())

    override fun updateWatchable(watchableId: String, watched: Boolean): Completable = sessionService.user
            .map { fireStore.watchables(it.uid).document(watchableId) }
            .flatMapCompletable { it.updateField(Watchable::watched.name, watched) }
            .subscribeOn(Schedulers.io())

    override fun setWatchableDeleted(watchableId: String): Completable = sessionService.user
            .map { fireStore.watchables(it.uid).document(watchableId) }
            .flatMapCompletable { it.updateField(Watchable::deleted.name, true) }
            .andThen(deleteSeasonsOf(watchableId))
            .subscribeOn(Schedulers.io())

    private fun deleteSeasonsOf(watchableId: String): Completable = sessionService.user.map { it.uid }
            .flatMapCompletable { userId ->
                RxTasks.single { fireStore.seasons(userId).whereEqualTo(WatchableSeason::watchableId.name, watchableId).get() }
                        .map { it.documents.map(DocumentSnapshot::getId) }
                        .flatMapObservable { it.map(fireStore.seasons(userId)::document).toObservable() }
                        .flatMapCompletable { it.updateField(WatchableSeason::deleted.name, true) }
            }
            .subscribeOn(Schedulers.io())

    override val watchableSeasonsObservable: Flowable<List<WatchableSeason>> = sessionService.user
            .map { fireStore.seasons(it.uid).whereEqualTo(WatchableSeason::deleted.name, false) }
            .flatMapPublisher { it.localObjectListObservable<WatchableSeason>() }
            .subscribeOn(Schedulers.io())

    override fun season(id: String): Single<WatchableSeason> = sessionService.user
            .map { fireStore.seasons(it.uid).document(id) }
            .flatMap { it.localObject<WatchableSeason>() }
            .subscribeOn(Schedulers.io())

    override fun createSeason(season: WatchableSeason): Single<WatchableSeason> = sessionService.user
            .map { fireStore.seasons(it.uid) }
            .flatMapCompletable { it.createObject(season) }
            .toSingleDefault(season)
            .subscribeOn(Schedulers.io())

    override fun updateSeason(seasonId: String, watched: Boolean): Completable {
        return sessionService.user
                .map { fireStore.seasons(it.uid).document(seasonId) }
                .flatMapCompletable { seasonDocument ->
                    seasonDocument.localObject<WatchableSeason>()
                            .map { it.copy(episodes = it.episodes.mapValues { watched }) }
                            .flatMapCompletable(seasonDocument::updateObject)
                }
                .subscribeOn(Schedulers.io())
    }

    override fun updateSeasonEpisode(seasonId: String, episode: String, watched: Boolean): Completable = sessionService.user
            .map { fireStore.seasons(it.uid).document(seasonId) }
            .flatMapCompletable { it.updateNestedField(WatchableSeason::episodes.name, episode to watched) }
            .subscribeOn(Schedulers.io())

    override val watchablesToUpdate: Single<List<Watchable>>
        get() = sessionService.user
                .map {
                    fireStore.watchables(it.uid)
                            .whereEqualTo(Watchable::status.name, Watchable.Status.running.name)
                            .whereEqualTo(Watchable::deleted.name, false)
                }
                .flatMap { it.localObjectList<Watchable>() }
                .subscribeOn(Schedulers.io())

    override val watchablesToDelete: Single<List<Watchable>>
        get() = sessionService.user
                .map { fireStore.watchables(it.uid).whereEqualTo(Watchable::deleted.name, true) }
                .flatMap { it.localObjectList<Watchable>() }
                .subscribeOn(Schedulers.io())

    override fun deleteWatchable(watchableId: String): Completable = sessionService.user
            .map { fireStore.watchables(it.uid).document(watchableId) }
            .flatMapCompletable { RxTasks.completable { it.delete() } }
            .subscribeOn(Schedulers.io())

    override val watchableSeasonsToDelete: Single<List<WatchableSeason>>
        get() = sessionService.user
                .map { fireStore.seasons(it.uid).whereEqualTo(WatchableSeason::deleted.name, true) }
                .flatMap { it.localObjectList<WatchableSeason>() }
                .subscribeOn(Schedulers.io())

    override fun deleteWatchableSeason(seasonId: String): Completable = sessionService.user
            .map { fireStore.seasons(it.uid).document(seasonId) }
            .flatMapCompletable { RxTasks.completable { it.delete() } }
            .subscribeOn(Schedulers.io())
}