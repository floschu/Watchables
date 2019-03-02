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

package at.florianschuster.watchables.ui.splashscreen

import androidx.navigation.NavDirections
import at.florianschuster.watchables.Direction
import at.florianschuster.watchables.Director
import at.florianschuster.watchables.R
import at.florianschuster.watchables.service.FirebaseUserSessionService
import at.florianschuster.watchables.ui.base.BaseFragment
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit


sealed class SplashDirection(val navDirections: NavDirections) : Direction {
    object Login : SplashDirection(SplashScreenFragmentDirections.actionSplashscreenToLogin())
    object Watchables : SplashDirection(SplashScreenFragmentDirections.actionSplashscreenToWatchables())
}


class SplashScreenFragment : BaseFragment(R.layout.fragment_splashscreen), Director<SplashDirection> {
    private val userSessionService: FirebaseUserSessionService by inject()
    private var timerDisposable: Disposable? = null

    override fun onResume() {
        super.onResume()
        Completable.timer(1000, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe {
            when {
                !userSessionService.loggedIn -> direct(SplashDirection.Login)
                else -> direct(SplashDirection.Watchables)
            }
        }.let { timerDisposable = it }
    }

    override fun onPause() {
        super.onPause()
        timerDisposable?.dispose()
    }

    override fun direct(to: SplashDirection) {
        navController.navigate(to.navDirections)
    }
}

