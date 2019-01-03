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

import androidx.navigation.fragment.FragmentNavigatorExtras
import at.florianschuster.watchables.R
import at.florianschuster.watchables.service.FirebaseUserSessionService
import at.florianschuster.watchables.ui.base.views.BaseFragment
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_splashscreen.*
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit


class SplashScreenFragment : BaseFragment(R.layout.fragment_splashscreen) {
    private val userSessionService: FirebaseUserSessionService by inject()
    private var timerDisposable: Disposable? = null

    override fun onResume() {
        super.onResume()
        Completable.timer(200, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe {
            val extras = FragmentNavigatorExtras(ivLogo to ivLogo.transitionName)
            when {
                !userSessionService.loggedIn -> {
                    navController.navigate(R.id.action_splashscreen_to_login, null, null, extras)
                }
                else -> navController.navigate(R.id.action_splashscreen_to_watchables, null, null, extras)
            }
        }.let { timerDisposable = it }
    }

    override fun onPause() {
        super.onPause()
        timerDisposable?.dispose()
    }
}