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
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import at.florianschuster.watchables.model.Movie
import at.florianschuster.watchables.model.Search
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.convertToWatchable
import at.florianschuster.watchables.model.convertToWatchableSeason
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.NotificationService
import at.florianschuster.watchables.service.remote.MovieDatabaseApi
import at.florianschuster.watchables.service.remote.WatchablesApi
import io.reactivex.Completable
import io.reactivex.rxkotlin.toFlowable
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class AddWatchableWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams), KoinComponent {
    private val movieDatabaseApi: MovieDatabaseApi by inject()
    private val watchablesApi: WatchablesApi by inject()
    private val analyticsService: AnalyticsService by inject()
    private val notificationService: NotificationService by inject()

    override fun doWork(): Result {
        val id = inputData.getInt(EXTRA_ID, -1)
        val type = inputData.getInt(EXTRA_TYPE, -1)
        val name = inputData.getString(EXTRA_NAME)

        return if (id == -1 || type !in Search.SearchItem.Type.values().map { it.ordinal }) {
            Timber.e("InputData failure.")
            Result.failure()
        } else {
            val searchType = Search.SearchItem.Type.values()[type]
            val error = watchablesApi.watchable("$id")
                    .flatMapCompletable {
                        if (it.deleted || it.status.needToUpdate) create(searchType, id)
                        else Completable.complete()
                    }
                    .onErrorResumeNext {
                        if (it is NoSuchElementException) create(searchType, id)
                        else Completable.error(it)
                    }
                    .doOnError { notificationService.addWatchableError(id, name) }
                    .doOnError(Timber::e)
                    .blockingGet()

            if (error != null) Result.failure() else Result.success()
        }
    }

    private fun create(type: Search.SearchItem.Type, id: Int) = when (type) {
        Search.SearchItem.Type.movie -> addMovie(id)
        Search.SearchItem.Type.tv -> addShow(id)
    }

    private fun addMovie(id: Int): Completable = movieDatabaseApi.movie(id)
            .map(Movie::convertToWatchable)
            .flatMap(watchablesApi::createWatchable)
            .doOnSuccess(analyticsService::logWatchableAdd)
            .ignoreElement()

    private fun addShow(id: Int): Completable = movieDatabaseApi.show(id)
            .map { it to it.convertToWatchable() }
            .flatMapCompletable { (show, watchable) ->
                watchablesApi.createWatchable(watchable)
                        .doOnSuccess(analyticsService::logWatchableAdd)
                        .ignoreElement()
                        .andThen((1..show.seasons).toFlowable())
                        .flatMapSingle { movieDatabaseApi.season(show.id, it) }
                        .map { it.convertToWatchableSeason(watchable.id) }
                        .flatMapSingle(watchablesApi::createSeason)
                        .ignoreElements()
                        .onErrorResumeNext {
                            watchablesApi.setWatchableDeleted(watchable.id)
                                    .onErrorComplete()
                                    .andThen(Completable.error(it))
                        }
            }

    companion object {
        fun start(item: Search.SearchItem): Operation = start(item.id, item.type, item.title)

        fun start(watchable: Watchable): Operation = start(
                watchable.id.toInt(),
                if (watchable.type == Watchable.Type.movie) Search.SearchItem.Type.movie else Search.SearchItem.Type.tv,
                watchable.name
        )

        private fun start(id: Int, type: Search.SearchItem.Type, title: String): Operation =
                OneTimeWorkRequest.Builder(AddWatchableWorker::class.java).apply {
                    val constraints = Constraints.Builder().apply {
                        setRequiresCharging(false)
                        setRequiredNetworkType(NetworkType.CONNECTED)
                        setRequiresStorageNotLow(false)
                        setRequiresBatteryNotLow(false)
                    }.build()
                    setConstraints(constraints)

                    val inputData = Data.Builder().apply {
                        putInt(EXTRA_ID, id)
                        putInt(EXTRA_TYPE, type.ordinal)
                        putString(EXTRA_NAME, title)
                    }.build()
                    setInputData(inputData)
                }.build().let(WorkManager.getInstance()::enqueue)

        private const val EXTRA_ID = "AddWatchableWorker.EXTRA_ID"
        private const val EXTRA_TYPE = "AddWatchableWorker.EXTRA_TYPE"
        private const val EXTRA_NAME = "AddWatchableWorker.EXTRA_NAME"
    }
}
