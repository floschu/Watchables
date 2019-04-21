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

package at.florianschuster.watchables.ui.main

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.reaktor.emptyMutation
import at.florianschuster.watchables.MainDirections
import at.florianschuster.watchables.R
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.service.local.PrefRepo
import at.florianschuster.watchables.ui.base.BaseActivity
import at.florianschuster.watchables.all.util.Utils
import at.florianschuster.watchables.all.util.extensions.main
import at.florianschuster.watchables.ui.base.BaseReactor
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.jakewharton.rxrelay2.PublishRelay
import com.tailoredapps.androidutil.ui.extensions.RxDialogAction
import com.tailoredapps.androidutil.ui.extensions.rxDialog
import com.tailoredapps.reaktor.android.koin.reactor
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.activity_main.*
import org.threeten.bp.LocalDate

class MainActivity : BaseActivity(R.layout.activity_main), ReactorView<MainReactor>, MainScreenFab {
    private val navController: NavController by lazy { findNavController(R.id.navHost) }
    private val noSessionNeededDestinations = arrayOf(R.id.splashscreen, R.id.login)

    override val reactor: MainReactor by reactor()

    override val mainScreenFab: FloatingActionButton by lazy { fabScrollDown }
    override val fabClickedRelay: PublishRelay<View> = PublishRelay.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bnv.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, dest, _ ->
            main { mainScreenFab.isVisible = false }
            val bnvShouldBeVisible = dest.id !in noSessionNeededDestinations
            if (bnv.isVisible != bnvShouldBeVisible) main { bnv.isVisible = bnvShouldBeVisible }
        }

        attachMainScreenFabClicks()

        bind(reactor)
    }

    override fun bind(reactor: MainReactor) {
        reactor.state.changesFrom { it.loggedIn }
                .skip(1)
                .filter { it.not() }
                .filter { navController.currentDestination?.id !in noSessionNeededDestinations }
                .bind {
                    val options = navController.currentDestination?.id?.let {
                        navOptions { popUpTo(it) { inclusive = true } }
                    }
                    navController.navigate(MainDirections.toLogin(), options)
                }
                .addTo(disposables)

        reactor.state.changesFrom { it.dialogShownDate }
                .filter { it.isBefore(LocalDate.now().minusMonths(1)) }
                .filter { reactor.currentState.loggedIn }
                .flatMapSingle {
                    rxDialog(R.style.DialogTheme) {
                        titleResource = R.string.enjoying_dialog_title
                        messageResource = R.string.enjoying_dialog_message
                        positiveButtonResource = R.string.enjoying_dialog_positive
                        negativeButtonResource = R.string.enjoying_dialog_negative
                    }
                }
                .ofType<RxDialogAction.Positive>()
                .map { Utils.rateApp(this) }
                .doOnNext { reactor.action.accept(MainReactor.Action.UpdateDialogShownDate) }
                .bind(::startActivity)
                .addTo(disposables)
    }

    override fun onSupportNavigateUp() = navController.navigateUp()
}

class MainReactor(
        private val prefRepo: PrefRepo,
        private val sessionService: SessionService<FirebaseUser, AuthCredential>
) : BaseReactor<MainReactor.Action, MainReactor.Mutation, MainReactor.State>(
        initialState = State(),
        initialAction = Action.LoadDialogShownDate
) {
    sealed class Action {
        object LoadDialogShownDate : Action()
        object UpdateDialogShownDate : Action()
    }

    sealed class Mutation {
        data class SetLoggedIn(val loggedIn: Boolean) : Mutation()
        data class SetDialogShownDate(val dialogShownDate: LocalDate) : Mutation()
    }

    data class State(
            val loggedIn: Boolean = false,
            val dialogShownDate: LocalDate = LocalDate.now()
    )

    override fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> {
        val sessionMutation = sessionService.session
                .map { Mutation.SetLoggedIn(it.loggedIn) }
                .toObservable()
        return Observable.merge(mutation, sessionMutation)
    }

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.LoadDialogShownDate -> {
            Observable.just(Mutation.SetDialogShownDate(prefRepo.enjoyingAppDialogShownDate))
        }
        is Action.UpdateDialogShownDate -> {
            emptyMutation { prefRepo.enjoyingAppDialogShownDate = LocalDate.now() }
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetLoggedIn -> previousState.copy(loggedIn = mutation.loggedIn)
        is Mutation.SetDialogShownDate -> previousState.copy(dialogShownDate = mutation.dialogShownDate)
    }
}