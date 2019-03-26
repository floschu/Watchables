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

import at.florianschuster.watchables.R
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.ui.base.BaseFragment
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SplashFragment : BaseFragment(R.layout.fragment_splash) {
    private val sessionService: SessionService<FirebaseUser, AuthCredential> by inject()
    private var timerDisposable: Disposable? = null

    override fun onResume() {
        super.onResume()
        timerDisposable = Completable.timer(750, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when {
                        !sessionService.loggedIn -> SplashFragmentDirections.actionSplashscreenToLogin()
                        else -> SplashFragmentDirections.actionSplashscreenToWatchables()
                    }.also(navController::navigate)
                }
    }

    override fun onPause() {
        super.onPause()
        timerDisposable?.dispose()
    }
}