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
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.transition.TransitionInflater
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.WatchableUser
import at.florianschuster.watchables.service.ErrorTranslationService
import at.florianschuster.watchables.service.FirebaseUserSessionService
import at.florianschuster.watchables.service.remote.WatchablesApi
import at.florianschuster.watchables.ui.base.reactor.BaseReactor
import at.florianschuster.watchables.ui.base.views.ReactorFragment
import at.florianschuster.watchables.util.Async
import at.florianschuster.watchables.util.extensions.*
import at.florianschuster.watchables.worker.DeleteWatchablesWorker
import at.florianschuster.watchables.worker.UpdateWatchablesWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.visibility
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_login.*
import org.koin.android.ext.android.inject


private const val SIGN_IN_CODE = 3432

class LoginFragment : ReactorFragment<LoginReactor>(R.layout.fragment_login) {
    override val reactor: LoginReactor by reactor()

    private val errorTranslationService: ErrorTranslationService  by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(activity).inflateTransition(R.transition.shared_elements)
    }

    override fun bind(reactor: LoginReactor) {
        onSharedEnterTransition { AnimationUtils.loadAnimation(context, R.anim.pulse).also(ivLogo::startAnimation) }

        tvSource.clicks().subscribe { openChromeTab(getString(R.string.tmdb_url)) }.addTo(disposable)
        tvPolicy.clicks().subscribe { openChromeTab(getString(R.string.privacy_policy_url)) }.addTo(disposable)

        btnSignIn.clicks()
                .map {
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).apply {
                        requestIdToken(getString(R.string.default_web_client_id))
                        requestEmail()
                    }.build()
                }
                .map { GoogleSignIn.getClient(activity!!, it) }
                .subscribe { startActivityForResult(it.signInIntent, SIGN_IN_CODE) }
                .addTo(disposable)

        //state
        reactor.state.map { it.result }
                .distinctUntilChanged()
                .subscribe {
                    progress.visibility(View.INVISIBLE).accept(it is Async.Loading)
                    when (it) {
                        is Async.Success -> navController.navigate(
                                R.id.action_login_to_watchables,
                                null,
                                null,
                                FragmentNavigatorExtras(ivLogo to ivLogo.transitionName)
                        )
                        is Async.Error -> errorTranslationService.toastConsumer.accept(it.error)
                    }
                }
                .addTo(disposable)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SIGN_IN_CODE) return

        RxTasks.single { GoogleSignIn.getSignedInAccountFromIntent(data) }
                .map { GoogleAuthProvider.getCredential(it.idToken, null) }
                .map { LoginReactor.Action.Login(it) }
                .subscribe(reactor.action, errorTranslationService.toastConsumer)
                .addTo(disposable)
    }
}


class LoginReactor(
        private val watchablesApi: WatchablesApi,
        private val userSessionService: FirebaseUserSessionService
) : BaseReactor<LoginReactor.Action, LoginReactor.Mutation, LoginReactor.State>(State()) {
    sealed class Action {
        data class Login(val credential: AuthCredential) : Action()
    }

    sealed class Mutation {
        data class Login(val result: Async<Boolean>) : Mutation()
    }

    data class State(val result: Async<Boolean> = Async.Uninitialized)

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.Login -> {
            val loading = Observable.just(Mutation.Login(Async.Loading))
            Observable.concat(loading, loginMutation(action.credential))
        }
    }

    override fun reduce(state: State, mutation: Mutation): State = when (mutation) {
        is Mutation.Login -> state.copy(result = mutation.result)
    }

    private fun loginMutation(credential: AuthCredential): Observable<Mutation.Login> =
            userSessionService.login(credential)
                    .andThen(userSessionService.user)
                    .flatMapCompletable(::createWatchableUserIfNeeded)
                    .doOnComplete { UpdateWatchablesWorker.start() }
                    .doOnComplete { DeleteWatchablesWorker.start() }
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