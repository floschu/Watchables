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

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import at.florianschuster.koordinator.CoordinatorRoute
import at.florianschuster.koordinator.Router
import at.florianschuster.koordinator.android.LifecycleCoordinator
import at.florianschuster.koordinator.android.koin.coordinator
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.android.koin.reactor
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.watchables.MainDirections
import at.florianschuster.watchables.R
import at.florianschuster.watchables.all.util.extensions.main
import at.florianschuster.watchables.all.util.extensions.openAppInPlayStore
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.service.AppUpdateService
import at.florianschuster.watchables.service.DeepLinkService
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.service.local.PrefRepo
import at.florianschuster.watchables.ui.base.BaseActivity
import at.florianschuster.reaktor.android.ViewModelReactor
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.tailoredapps.androidutil.ui.extensions.RxDialogAction
import com.tailoredapps.androidutil.ui.extensions.rxDialog
import com.tailoredapps.androidutil.ui.extensions.toObservableDefault
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.activity_main.*
import org.threeten.bp.LocalDate

sealed class MainRoute : CoordinatorRoute {
    data class ShowWatchableDetail(val id: String, val type: Watchable.Type) : MainRoute()
    object ToLogin : MainRoute()
}

class MainCoordinator : LifecycleCoordinator<MainRoute, NavController>() {
    override fun navigate(route: MainRoute, handler: NavController) {
        when (route) {
            is MainRoute.ShowWatchableDetail -> {
                handler.navigate(MainDirections.toDetail(route.id, route.type))
            }
            is MainRoute.ToLogin -> {
                handler.navigate(MainDirections.toLogin())
            }
        }
    }
}

class MainActivity : BaseActivity(R.layout.activity_main), ReactorView<MainReactor> {
    private val noSessionNeededDestinations = arrayOf(R.id.login)
    private val navController: NavController by lazy { findNavController(R.id.navHost) }

    override val reactor: MainReactor by reactor()
    private val coordinator: MainCoordinator by coordinator()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        coordinator provideNavigationHandler navController

        bnv.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val bnvShouldBeVisible = destination.id !in noSessionNeededDestinations
            if (bnv.isVisible != bnvShouldBeVisible) main { bnv.isVisible = bnvShouldBeVisible }
        }

        bind(reactor)

        intent.handleDeepLink()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent.handleDeepLink()
    }

    override fun bind(reactor: MainReactor) {
        reactor.state.changesFrom { it.loggedIn }
            .filter { !it && navController.currentDestination?.id !in noSessionNeededDestinations }
            .bind { Router follow MainRoute.ToLogin }
            .addTo(disposables)

        reactor.state.changesFrom { it.shouldShowRateDialog }
            .filter { it && reactor.currentState.loggedIn }
            .flatMapSingle {
                rxDialog(R.style.DialogTheme) {
                    titleResource = R.string.enjoying_dialog_title
                    messageResource = R.string.enjoying_dialog_message
                    positiveButtonResource = R.string.enjoying_dialog_positive
                    negativeButtonResource = R.string.enjoying_dialog_negative
                }
            }
            .map { it is RxDialogAction.Positive }
            .bind { accepted ->
                reactor.action.accept(MainReactor.Action.UpdateRateDialogShownDate(accepted))
                if (accepted) {
                    openAppInPlayStore()
                }
            }
            .addTo(disposables)

        reactor.state.changesFrom { it.shouldShowNewUpdateDialog }
            .filter { it && reactor.currentState.loggedIn }
            .flatMapSingle {
                rxDialog(R.style.DialogTheme) {
                    titleResource = R.string.dialog_new_version_title
                    messageResource = R.string.dialog_new_version_message
                    positiveButtonResource = R.string.dialog_new_version_update
                    negativeButtonResource = R.string.dialog_new_version_cancel
                }
            }
            .map { it is RxDialogAction.Positive }
            .bind { accepted ->
                reactor.action.accept(MainReactor.Action.UpdateVersionDialogShownDate(accepted))
                if (accepted) {
                    openAppInPlayStore()
                }
            }
            .addTo(disposables)
    }

    override fun onSupportNavigateUp() = navController.navigateUp()

    private fun Intent?.handleDeepLink() {
        if (this == null) return
        reactor.action.accept(MainReactor.Action.ParseIntentForDeepLinks(this))
    }
}

class MainReactor(
    private val prefRepo: PrefRepo,
    private val sessionService: SessionService<FirebaseUser, AuthCredential>,
    private val deepLinkService: DeepLinkService,
    private val appUpdateService: AppUpdateService
) : ViewModelReactor<MainReactor.Action, MainReactor.Mutation, MainReactor.State>(
    initialState = State(),
    initialAction = Action.LoadDialogShownDate
) {
    sealed class Action {
        object LoadDialogShownDate : Action()
        data class UpdateRateDialogShownDate(val rated: Boolean) : Action()
        data class ParseIntentForDeepLinks(val intent: Intent) : Action()
        data class UpdateVersionDialogShownDate(val updated: Boolean) : Action()
    }

    sealed class Mutation {
        data class SetLoggedIn(val loggedIn: Boolean) : Mutation()
        data class SetShouldShowRateDialog(val shouldShow: Boolean) : Mutation()
        data class SetShouldShowNewVersionDialog(val shouldShow: Boolean) : Mutation()
    }

    data class State(
        val loggedIn: Boolean = true,
        val shouldShowRateDialog: Boolean = false,
        val shouldShowNewUpdateDialog: Boolean = false
    )

    override fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> {
        val sessionMutation = sessionService.session
            .map { Mutation.SetLoggedIn(it.loggedIn) }
            .toObservable()

        val appUpdateMutation = appUpdateService.liveStatus
            .filter { it.updateAvailable }
            .map { it.availableVersionCode > prefRepo.lastNewUpdateDialogVersionCode }
            .map { Mutation.SetShouldShowNewVersionDialog(it) }
            .toObservable()

        return Observable.merge(mutation, sessionMutation, appUpdateMutation)
    }

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.LoadDialogShownDate -> {
            val shouldShowRate = prefRepo.rated ||
                prefRepo.enjoyingAppDialogShownDate.isBefore(LocalDate.now().minusMonths(1))
            Observable.just(Mutation.SetShouldShowRateDialog(shouldShowRate))
        }
        is Action.UpdateRateDialogShownDate -> {
            Completable
                .fromAction {
                    prefRepo.rated = action.rated
                    prefRepo.enjoyingAppDialogShownDate = LocalDate.now()
                }
                .toObservableDefault(Mutation.SetShouldShowRateDialog(false))
        }
        is Action.ParseIntentForDeepLinks -> {
            val deepLinkObservable = deepLinkService.handleIntent(action.intent)
                .ofType<DeepLinkService.Link.ToWatchable>()
                .doOnSuccess { Router follow MainRoute.ShowWatchableDetail(it.id, it.type) }
                .ignoreElement()
            sessionService.session.take(1)
                .filter { it.loggedIn }
                .concatWith(deepLinkObservable)
                .ignoreElements()
                .toObservable()
        }
        is Action.UpdateVersionDialogShownDate -> {
            appUpdateService.status
                .doOnSuccess { prefRepo.lastNewUpdateDialogVersionCode = it.availableVersionCode }
                .map { Mutation.SetShouldShowNewVersionDialog(false) }
                .toObservable()
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetLoggedIn -> previousState.copy(loggedIn = mutation.loggedIn)
        is Mutation.SetShouldShowRateDialog -> previousState.copy(shouldShowRateDialog = mutation.shouldShow)
        is Mutation.SetShouldShowNewVersionDialog -> previousState.copy(shouldShowNewUpdateDialog = mutation.shouldShow)
    }
}
