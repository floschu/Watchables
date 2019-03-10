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

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import at.florianschuster.watchables.*
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.util.coordinator.*
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit


class SplashCoordinator(fragment: Fragment) : FragmentCoordinator<SplashCoordinator.Route>(fragment) {
    enum class Route : CoordinatorRoute {
        OnNotLoggedIn, OnLoggedIn
    }

    private val navController = fragment.findNavController()

    override fun navigate(to: Route) {
        val navDirections = when (to) {
            Route.OnNotLoggedIn -> SplashFragmentDirections.actionSplashscreenToLogin()
            Route.OnLoggedIn -> SplashFragmentDirections.actionSplashscreenToWatchables()
        }
        navController.navigate(navDirections)
    }
}


class SplashFragment : BaseFragment(R.layout.fragment_splash) {
    private val coordinator: SplashCoordinator by fragmentCoordinator { SplashCoordinator(this) }

    private val sessionService: SessionService<FirebaseUser, AuthCredential> by inject()
    private var timerDisposable: Disposable? = null

    override fun onResume() {
        super.onResume()
        timerDisposable = Completable.timer(750, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when {
                        !sessionService.loggedIn -> coordinator.navigate(SplashCoordinator.Route.OnNotLoggedIn)
                        else -> coordinator.navigate(SplashCoordinator.Route.OnLoggedIn)
                    }
                }
    }

    override fun onPause() {
        super.onPause()
        timerDisposable?.dispose()
    }
}