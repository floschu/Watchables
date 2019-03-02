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

package at.florianschuster.watchables.worker

import android.content.Context
import androidx.work.*
import at.florianschuster.watchables.model.*
import at.florianschuster.watchables.service.FirebaseUserSessionService
import at.florianschuster.watchables.service.NotificationService
import at.florianschuster.watchables.service.remote.MovieDatabaseApi
import at.florianschuster.watchables.service.remote.WatchablesApi
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toFlowable
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit


class UpdateWatchablesWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams), KoinComponent {
    private val userSessionService: FirebaseUserSessionService by inject()
    private val notificationService: NotificationService by inject()
    private val movieDatabaseApi: MovieDatabaseApi by inject()
    private val watchablesApi: WatchablesApi by inject()

    override fun doWork(): Result =
            if (userSessionService.user.ignoreElement().blockingGet() != null) {
                Result.failure()
            } else {
                val error = watchablesApi.watchablesToUpdate
                        .toFlowable()
                        .flatMapIterable { it }
                        .flatMapCompletable {
                            if (it.type == Watchable.Type.movie) updateMovie(it)
                            else updateShowSeasons(it)
                        }
                        .doOnError(Timber::e)
                        .blockingGet()

                if (error != null) Result.failure() else Result.success()
            }

    private fun updateMovie(watchable: Watchable): Completable = movieDatabaseApi
            .movie(watchable.id.toInt())
            .doOnSuccess { notificationService.movieUpdate(watchable, it) }
            .map(Movie::convertToWatchable)
            .map { it.apply { watched = watchable.watched } }
            .flatMap(watchablesApi::createWatchable)
            .ignoreElement()

    private fun updateShowSeasons(watchable: Watchable): Completable = movieDatabaseApi
            .show(watchable.id.toInt())
            .map { it to it.convertToWatchable() }
            .flatMapCompletable { (show, updatedWatchable) ->
                watchablesApi.createWatchable(updatedWatchable)
                        .ignoreElement()
                        .andThen((1..show.seasons).reversed().toFlowable())
                        .flatMapSingle { movieDatabaseApi.season(show.id, it) }
                        .flatMapCompletable { season ->
                            watchablesApi.season("${season.id}")
                                    .ignoreElement()
                                    .onErrorResumeNext {
                                        if (it is NoSuchElementException) addSeason(updatedWatchable.id, season)
                                                .doOnComplete { notificationService.showUpdate(updatedWatchable, show.name, season) }
                                        else Completable.error(it)
                                    }
                        }
            }

    private fun addSeason(watchableId: String, season: Season): Completable =
            Single.just(season.convertToWatchableSeason(watchableId))
                    .flatMap(watchablesApi::createSeason)
                    .ignoreElement()


    companion object {
        fun start() = PeriodicWorkRequest.Builder(UpdateWatchablesWorker::class.java, 24, TimeUnit.HOURS).apply {
            val constraints = Constraints.Builder().apply {
                setRequiresCharging(true)
                setRequiredNetworkType(NetworkType.CONNECTED)
                setRequiresStorageNotLow(false)
                setRequiresBatteryNotLow(false)
            }.build()
            setConstraints(constraints)
        }.build().let {
            WorkManager.getInstance().enqueueUniquePeriodicWork(WORKER_NAME, ExistingPeriodicWorkPolicy.REPLACE, it)
        }

        fun stop() = WorkManager.getInstance().cancelUniqueWork(WORKER_NAME)

        private const val WORKER_NAME = "at.florianschuster.watchables.worker.UpdateWatchablesWorker"
    }
}