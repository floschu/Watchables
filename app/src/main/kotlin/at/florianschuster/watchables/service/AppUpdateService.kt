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
import java.util.concurrent.TimeUnit

interface AppUpdateService {
    sealed class Status {
        abstract val availableVersionCode: Int

        data class UpToDate(override val availableVersionCode: Int) : Status()
        data class ShouldUpdate(override val availableVersionCode: Int) : Status()
        data class MustUpdate(override val availableVersionCode: Int) : Status()

        val updateAvailable: Boolean get() = this is ShouldUpdate || this is MustUpdate
    }

    val status: Single<Status>
    val liveStatus: Flowable<Status>
}

class PlayAppUpdateService(
    private val context: Context
) : AppUpdateService {
    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(context) }

    override val status: Single<AppUpdateService.Status>
        get() = appUpdateManager.appUpdateInfoSingle.map { info ->
            val availableVersionCode = info.availableVersionCode()
            when {
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && availableVersionCode > BuildConfig.VERSION_CODE + 2 -> {
                    AppUpdateService.Status.MustUpdate(availableVersionCode)
                }
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                    AppUpdateService.Status.ShouldUpdate(availableVersionCode)
                }
                else -> AppUpdateService.Status.UpToDate(availableVersionCode)
            }
        }

    override val liveStatus: Flowable<AppUpdateService.Status>
        get() = Flowable.interval(10, 600, TimeUnit.SECONDS).flatMapSingle { status }

    private val AppUpdateManager.appUpdateInfoSingle: Single<AppUpdateInfo>
        get() = Single.create { emitter ->
            appUpdateInfo.addOnSuccessListener(emitter::onSuccess).addOnFailureListener(emitter::onError)
        }
}
