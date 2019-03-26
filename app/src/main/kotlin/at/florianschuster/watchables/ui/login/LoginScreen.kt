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

package at.florianschuster.watchables.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.watchables.AppRoute
import at.florianschuster.watchables.Coordinator
import at.florianschuster.watchables.R
import at.florianschuster.watchables.Router
import at.florianschuster.watchables.coordinator
import at.florianschuster.watchables.model.WatchableUser
import at.florianschuster.watchables.service.ErrorTranslationService
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.service.remote.WatchablesApi
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.base.BaseReactor
import at.florianschuster.watchables.util.extensions.RxTasks
import at.florianschuster.watchables.util.extensions.openChromeTab
import at.florianschuster.watchables.worker.DeleteWatchablesWorker
import at.florianschuster.watchables.worker.UpdateWatchablesWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.visibility
import com.tailoredapps.androidutil.async.Async
import com.tailoredapps.androidutil.extensions.toObservableDefault
import com.tailoredapps.reaktor.koin.reactor
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_login.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class LoginCoordinator(
        router: Router
) : Coordinator<LoginReactor.Route, NavController>(router) {
    override fun navigate(route: LoginReactor.Route, handler: NavController) {
        when (route) {
            LoginReactor.Route.OnLoggedIn -> {
                handler.navigate(LoginFragmentDirections.actionLoginToWatchables())
            }
        }
    }
}

class LoginFragment : BaseFragment(R.layout.fragment_login), ReactorView<LoginReactor> {
    override val reactor: LoginReactor by reactor()

    private val errorTranslationService: ErrorTranslationService by inject()

    private val coordinator: LoginCoordinator by coordinator { LoginCoordinator(get()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind(reactor)
    }

    override fun bind(reactor: LoginReactor) {
        coordinator.provideNavigationHandler(findNavController())

        AnimationUtils.loadAnimation(context, R.anim.pulse).also(ivLogo::startAnimation)

        tvSource.clicks().subscribe { openChromeTab(getString(R.string.tmdb_url)) }.addTo(disposables)
        tvPolicy.clicks().subscribe { openChromeTab(getString(R.string.privacy_policy_url)) }.addTo(disposables)

        btnSignIn.clicks()
                .map {
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).apply {
                        requestIdToken(getString(R.string.default_web_client_id))
                        requestEmail()
                    }.build()
                }
                .map { GoogleSignIn.getClient(activity!!, it) }
                .subscribe { startActivityForResult(it.signInIntent, SIGN_IN_CODE) }
                .addTo(disposables)

        // state
        reactor.state.changesFrom { it.result }
                .bind {
                    progress.visibility(View.INVISIBLE).accept(it is Async.Loading)
                    if (it is Async.Error) {
                        errorTranslationService.toastConsumer.accept(it.error)
                    }
                }
                .addTo(disposables)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SIGN_IN_CODE) return

        RxTasks.single { GoogleSignIn.getSignedInAccountFromIntent(data) }
                .map { GoogleAuthProvider.getCredential(it.idToken, null) }
                .map { LoginReactor.Action.Login(it) }
                .subscribe(reactor.action, errorTranslationService.toastConsumer)
                .addTo(disposables)
    }

    companion object {
        private const val SIGN_IN_CODE = 3432
    }
}

class LoginReactor(
        private val router: Router,
        private val watchablesApi: WatchablesApi,
        private val sessionService: SessionService<FirebaseUser, AuthCredential>
) : BaseReactor<LoginReactor.Action, LoginReactor.Mutation, LoginReactor.State>(State()) {
    enum class Route : AppRoute {
        OnLoggedIn
    }

    sealed class Action {
        data class Login(val credential: AuthCredential) : Action()
    }

    sealed class Mutation {
        data class Login(val result: Async<Boolean>) : Mutation()
    }

    data class State(
            val result: Async<Boolean> = Async.Uninitialized
    )

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.Login -> {
            val loading = Observable.just(Mutation.Login(Async.Loading))


            Observable.concat(loading, loginMutation(action.credential))
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.Login -> previousState.copy(result = mutation.result)
    }

    private fun loginMutation(credential: AuthCredential): Observable<Mutation.Login> =
            sessionService.login(credential)
                    .andThen(sessionService.user)
                    .flatMapCompletable(::createWatchableUserIfNeeded)
                    .doOnComplete { UpdateWatchablesWorker.start() }
                    .doOnComplete { DeleteWatchablesWorker.start() }
                    .doOnComplete { router follow Route.OnLoggedIn }
                    .toObservableDefault(Mutation.Login(Async.Success(true)))
                    .onErrorReturn { Mutation.Login(Async.Error(it)) }

    private fun createWatchableUserIfNeeded(user: FirebaseUser): Completable =
            watchablesApi.watchableUser.ignoreElement().onErrorResumeNext {
                if (it is NoSuchElementException) {
                    watchablesApi.createUser(WatchableUser(0).apply { id = user.uid })
                } else {
                    Completable.error(it)
                }
            }
}