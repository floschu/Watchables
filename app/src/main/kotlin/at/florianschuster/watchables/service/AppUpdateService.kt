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

import android.content.Context
import at.florianschuster.watchables.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import io.reactivex.Flowable
import io.reactivex.Single
import timber.log.Timber
import java.util.concurrent.TimeUnit

interface AppUpdateService {
    enum class Status {
        AppUpToDate, ShouldUpdate, MustUpdate
    }

    val currentStatus: Single<Status>
    val status: Flowable<Status>
}

class PlayAppUpdateService(
    context: Context
) : AppUpdateService {
    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(context) }

    override val currentStatus: Single<AppUpdateService.Status>
        get() = appUpdateManager.appUpdateInfoSingle.map { info ->
            Timber.d("Info: (availability: ${info.updateAvailability()}, code: ${info.availableVersionCode()})")
            when {
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.availableVersionCode() > BuildConfig.VERSION_CODE + 2 -> {
                    AppUpdateService.Status.MustUpdate
                }
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                    AppUpdateService.Status.ShouldUpdate
                }
                else -> AppUpdateService.Status.AppUpToDate
            }
        }

    override val status: Flowable<AppUpdateService.Status>
        get() = Flowable.interval(5, 5, TimeUnit.SECONDS)
            // Flowable.interval(5, 600, TimeUnit.SECONDS)
            .flatMapSingle { currentStatus }

    private val AppUpdateManager.appUpdateInfoSingle: Single<AppUpdateInfo>
        get() = Single.create { emitter ->
            appUpdateInfo.addOnSuccessListener(emitter::onSuccess).addOnFailureListener(emitter::onError)
        }
}