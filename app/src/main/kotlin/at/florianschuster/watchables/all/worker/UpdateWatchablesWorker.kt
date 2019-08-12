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

package at.florianschuster.watchables.all.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import at.florianschuster.watchables.WatchablesApp
import at.florianschuster.watchables.service.NotificationService
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.service.WatchablesUpdateService
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import io.reactivex.Completable
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UpdateWatchablesWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams), KoinComponent {

    private val sessionService: SessionService<FirebaseUser, AuthCredential> by inject()
    private val watchablesUpdateService: WatchablesUpdateService by inject()
    private val notificationService: NotificationService by inject()

    override fun doWork(): Result =
        if (sessionService.user.ignoreElement().blockingGet() != null) {
            Result.failure()
        } else {
            val error = Completable
                .mergeArrayDelayError(updateMoviesAndShowPush(), updateShowsAndShowPush())
                .doOnError(Timber::e)
                .blockingGet()

            if (error != null) Result.failure() else Result.success()
        }

    private fun updateMoviesAndShowPush(): Completable = watchablesUpdateService.updateMovies()
        .flattenAsObservable { it }
        .doOnNext { (watchable, movie) ->
            notificationService.pushMovieUpdate(watchable, movie)
        }
        .ignoreElements()

    private fun updateShowsAndShowPush(): Completable = watchablesUpdateService.updateShows()
        .flattenAsObservable { it }
        .doOnNext { (watchable, show, season) ->
            notificationService.pushShowUpdate(watchable, show.name, season)
        }
        .ignoreElements()

    companion object {
        fun enqueue() = PeriodicWorkRequest.Builder(UpdateWatchablesWorker::class.java, 24, TimeUnit.HOURS).apply {
            val constraints = Constraints.Builder().apply {
                setRequiresCharging(true)
                setRequiredNetworkType(NetworkType.CONNECTED)
                setRequiresStorageNotLow(false)
                setRequiresBatteryNotLow(false)
            }.build()
            setConstraints(constraints)
        }.build().let {
            WorkManager.getInstance(WatchablesApp.instance)
                .enqueueUniquePeriodicWork(WORKER_NAME, ExistingPeriodicWorkPolicy.REPLACE, it)
        }

        fun once() = OneTimeWorkRequest.Builder(UpdateWatchablesWorker::class.java).apply {
            val constraints = Constraints.Builder().apply {
                setRequiredNetworkType(NetworkType.CONNECTED)
                setRequiresStorageNotLow(false)
                setRequiresBatteryNotLow(false)
            }.build()
            setConstraints(constraints)
        }.build().let { WorkManager.getInstance(WatchablesApp.instance).enqueue(it) }

        fun stop() = WorkManager.getInstance(WatchablesApp.instance).cancelUniqueWork(WORKER_NAME)

        private const val WORKER_NAME = "at.florianschuster.watchables.all.worker.UpdateWatchablesWorker"
    }
}