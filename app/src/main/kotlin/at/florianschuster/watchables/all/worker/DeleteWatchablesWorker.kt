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
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.service.WatchablesDataSource
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import io.reactivex.rxkotlin.toFlowable
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DeleteWatchablesWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams), KoinComponent {
    private val sessionService: SessionService<FirebaseUser, AuthCredential> by inject()
    private val watchablesDataSource: WatchablesDataSource by inject()
    private val analyticsService: AnalyticsService by inject()

    override fun doWork(): Result =
            if (sessionService.user.ignoreElement().blockingGet() != null) {
                Result.failure()
            } else {
                val errorWatchablesDelete = watchablesDataSource.watchablesToDelete
                        .flatMapPublisher { it.toFlowable() }
                        .map { it.id }
                        .flatMapCompletable(watchablesDataSource::deleteWatchable)
                        .doOnError(Timber::e)
                        .blockingGet()

                val errorWatchableSeasonsDelete = watchablesDataSource.watchableSeasonsToDelete
                        .flatMapPublisher { it.toFlowable() }
                        .map { it.id }
                        .flatMapCompletable(watchablesDataSource::deleteWatchableSeason)
                        .doOnError(Timber::e)
                        .blockingGet()

                analyticsService.logDeleteWorker(errorWatchablesDelete != null && errorWatchableSeasonsDelete != null)
                if (errorWatchablesDelete != null || errorWatchableSeasonsDelete != null) Result.failure()
                else Result.success()
            }

    companion object {
        fun start() = PeriodicWorkRequest.Builder(DeleteWatchablesWorker::class.java, 24, TimeUnit.HOURS).apply {
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

        private const val WORKER_NAME = "at.florianschuster.watchables.all.worker.DeleteWatchablesWorker"
    }
}