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

package at.florianschuster.watchables.ui.watchables

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.reaktor.consume
import at.florianschuster.reaktor.emptyMutation
import at.florianschuster.watchables.Coordinator
import at.florianschuster.watchables.AppRoute
import at.florianschuster.watchables.R
import at.florianschuster.watchables.Router
import at.florianschuster.watchables.coordinator
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.WatchableContainer
import at.florianschuster.watchables.model.WatchableSeason
import at.florianschuster.watchables.model.convertToWatchableContainer
import at.florianschuster.watchables.ui.base.BaseReactor
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.ShareService
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.service.local.PrefRepo
import at.florianschuster.watchables.service.remote.WatchablesApi
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.util.Utils
import at.florianschuster.watchables.util.extensions.openChromeTab
import at.florianschuster.watchables.util.photodetail.photoDetailConsumer
import at.florianschuster.watchables.worker.DeleteWatchablesWorker
import at.florianschuster.watchables.worker.UpdateWatchablesWorker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.visibility
import com.tailoredapps.androidutil.async.Async
import com.tailoredapps.androidutil.extensions.RxDialogAction
import com.tailoredapps.androidutil.extensions.RxPopupAction
import com.tailoredapps.androidutil.extensions.addScrolledPastItemListener
import com.tailoredapps.androidutil.extensions.rxDialog
import com.tailoredapps.androidutil.extensions.rxPopup
import com.tailoredapps.androidutil.extensions.smoothScrollUp
import com.tailoredapps.androidutil.extensions.snack
import com.tailoredapps.androidutil.optional.asOptional
import com.tailoredapps.androidutil.optional.filterSome
import com.tailoredapps.androidutil.optional.ofType
import com.tailoredapps.reaktor.koin.reactor
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.fragment_watchables.*
import kotlinx.android.synthetic.main.fragment_watchables_toolbar.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class WatchablesCoordinator(
        router: Router
) : Coordinator<WatchablesReactor.Route, NavController>(router) {
    override fun navigate(route: WatchablesReactor.Route, handler: NavController) {
        when (route) {
            is WatchablesReactor.Route.OnWatchableSelected -> {
                WatchablesFragmentDirections.actionWatchablesToDetail(route.id)
            }
            is WatchablesReactor.Route.SearchIsRequired -> {
                WatchablesFragmentDirections.actionWatchablesToSearch()
            }
        }.also(handler::navigate)
    }
}

class WatchablesFragment : BaseFragment(R.layout.fragment_watchables), ReactorView<WatchablesReactor> {
    override val reactor: WatchablesReactor by reactor()

    private val adapter: WatchablesAdapter by inject()
    private val prefRepo: PrefRepo by inject()
    private val shareService: ShareService by inject { parametersOf(activity) }
    private val analyticsService: AnalyticsService by inject()

    private val coordinator: WatchablesCoordinator by coordinator { WatchablesCoordinator(get()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coordinator.provideNavigationHandler(findNavController())

        AnimationUtils.loadAnimation(context, R.anim.pulse).also(ivLogo::startAnimation)

        with(rvWatchables) {
            adapter = this@WatchablesFragment.adapter
            addScrolledPastItemListener { fabScroll.isVisible = it }
        }

        fabScroll.clicks()
                .subscribe { rvWatchables.smoothScrollUp() }
                .addTo(disposables)

        adapter.itemClick.ofType<ItemClickType.PhotoDetail>()
                .map { it.url }
                .subscribe(context.photoDetailConsumer)
                .addTo(disposables)

        btnOptions.clicks()
                .flatMapSingle {
                    btnOptions.rxPopup(R.menu.menu_settings) {
                        if (it.itemId == R.id.analytics) {
                            it.isChecked = analyticsService.analyticsEnabled
                        }
                    }
                }
                .ofType<RxPopupAction.Selected>()
                .subscribe(::openMenuItem)
                .addTo(disposables)

        bind(reactor)
    }

    override fun bind(reactor: WatchablesReactor) {

        // state
        reactor.state.changesFrom { it.watchables }
                .ofType<Async.Success<List<WatchableContainer>>>()
                .map { it.element to (adapter.data to it.element).diff }
                .bind(adapter::setData)
                .addTo(disposables)

        var snack: Snackbar? = null
        reactor.state.changesFrom { it.watchables }
                .filter { it.complete }
                .map { it().asOptional }
                .filterSome()
                .doOnNext { rvWatchables.setFastScrollEnabled(it.count() > 5) }
                .map { it.isEmpty() }
                .doOnNext {
                    if (!it && !prefRepo.onboardingSnackShown && snack == null) {
                        snack = rootLayout.snack(
                                R.string.onboarding_snack,
                                Snackbar.LENGTH_INDEFINITE,
                                R.string.dialog_ok
                        ) { prefRepo.onboardingSnackShown = true }
                    }
                }
                .bind(emptyLayout.visibility())
                .addTo(disposables)

        // action
        adapter.itemClick.ofType<ItemClickType.Watched>()
                .map { WatchablesReactor.Action.SetWatched(it.watchableId, it.watched) }
                .consume(reactor)
                .addTo(disposables)

        adapter.itemClick.ofType<ItemClickType.WatchedEpisode>()
                .map { WatchablesReactor.Action.SetEpisodeWatched(it.seasonId, it.episode, it.watched) }
                .consume(reactor)
                .addTo(disposables)

        adapter.itemClick.ofType<ItemClickType.Options>()
                .map { it.watchable }
                .flatMapCompletable { watchable ->
                    rxDialog(R.style.DialogTheme) {
                        title = getString(R.string.dialog_options_watchable, watchable.name)
                        negativeButtonResource = R.string.dialog_cancel
                        setItems(getString(R.string.menu_watchable_share), getString(R.string.menu_watchable_delete))
                    }.ofType<RxDialogAction.Selected<*>>()
                            .flatMapCompletable {
                                when (it.index) {
                                    0 -> shareService.share(watchable)
                                    else -> deleteWatchableDialog(watchable)
                                }
                            }
                }
                .subscribe()
                .addTo(disposables)

        adapter.itemClick.ofType<ItemClickType.EpisodeOptions>()
                .flatMapCompletable { clickType ->
                    val seasonId = clickType.seasonId
                    rxDialog(R.style.DialogTheme) {
                        title = getString(R.string.dialog_options_watchable, getString(R.string.episode_name, clickType.seasonIndex, clickType.episodeIndex))
                        negativeButtonResource = R.string.dialog_cancel
                        setItems(getString(R.string.menu_watchable_season_set_watched), getString(R.string.menu_watchable_season_set_not_watched))
                    }.ofType<RxDialogAction.Selected<*>>()
                            .map { WatchablesReactor.Action.SetSeasonWatched(seasonId, it.index == 0) }
                            .doOnSuccess(reactor.action)
                            .ignoreElement()
                }
                .subscribe()
                .addTo(disposables)

        flSearch.clicks()
                .map { WatchablesReactor.Action.Search }
                .consume(reactor)
                .addTo(disposables)

        adapter.itemClick.ofType<ItemClickType.ItemDetail>()
                .map { WatchablesFragmentDirections.actionWatchablesToDetail(it.watchable.id) }
                .subscribe(navController::navigate)
//                .map { WatchablesReactor.Action.SetWatchablesSelected(it.watchable.id) }
//                .consume(reactor)
                .addTo(disposables)
    }

    private fun openMenuItem(item: RxPopupAction.Selected) {
        when (item.itemId) {
            R.id.devInfo -> openChromeTab(getString(R.string.developer_url))
            R.id.shareApp -> shareService.shareApp().subscribe().addTo(disposables)
            R.id.rateApp -> startActivity(Utils.rateApp(requireContext()))
            R.id.privacyPolicy -> openChromeTab(getString(R.string.privacy_policy_url))
            R.id.licenses -> Utils.showLibraries(requireContext())
            R.id.analytics -> analyticsService.analyticsEnabled = !analyticsService.analyticsEnabled
            R.id.logout -> showLogoutDialog()
        }
    }

    private fun deleteWatchableDialog(watchable: Watchable): Completable {
        return rxDialog(R.style.DialogTheme) {
            titleResource = R.string.dialog_delete_watchable_title
            messageResource = R.string.dialog_delete_watchable_message
            positiveButtonResource = R.string.dialog_ok
            negativeButtonResource = R.string.dialog_cancel
        }.ofType<RxDialogAction.Positive>()
                .map { WatchablesReactor.Action.DeleteWatchable(watchable.id) }
                .doOnSuccess(reactor.action)
                .ignoreElement()
    }

    private fun showLogoutDialog() {
        rxDialog(R.style.DialogTheme) {
            titleResource = R.string.dialog_logout_title
            messageResource = R.string.dialog_logout_message
            positiveButtonResource = R.string.dialog_ok
            negativeButtonResource = R.string.dialog_cancel
        }.ofType<RxDialogAction.Positive>()
                .map { WatchablesReactor.Action.Logout }
                .subscribe(reactor.action)
                .addTo(disposables)
    }
}


class WatchablesReactor(
        private val router: Router,
        private val watchablesApi: WatchablesApi,
        private val analyticsService: AnalyticsService,
        private val sessionService: SessionService<FirebaseUser, AuthCredential>
) : BaseReactor<WatchablesReactor.Action, WatchablesReactor.Mutation, WatchablesReactor.State>(State()) {

    sealed class Route : AppRoute {
        data class OnWatchableSelected(val id: String) : Route()
        object SearchIsRequired : Route()
    }

    sealed class Action {
        data class SetWatched(val watchableId: String, val watched: Boolean) : Action()
        data class SetEpisodeWatched(val watchableSeasonId: String, val episode: String, val watched: Boolean) : Action()
        data class SetSeasonWatched(val watchableSeasonId: String, val watched: Boolean) : Action()
        data class DeleteWatchable(val watchableId: String) : Action()
        object Logout : Action()
        object Search : Action()
        data class SetWatchablesSelected(val watchableId: String) : Action()
    }

    sealed class Mutation {
        data class SetWatchables(val watchables: Async<List<WatchableContainer>>) : Mutation()
    }

    data class State(
            val watchables: Async<List<WatchableContainer>> = Async.Uninitialized
    )

    override fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> =
            Observable.merge(mutation, watchablesMutation)

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.SetWatched -> {
            watchablesApi
                    .updateWatchable(action.watchableId, action.watched)
                    .doOnComplete { analyticsService.logWatchableWatched(action.watchableId, action.watched) }
                    .andThen(Observable.empty())
        }
        is Action.SetEpisodeWatched -> {
            watchablesApi
                    .updateSeasonEpisode(action.watchableSeasonId, action.episode, action.watched)
                    .andThen(Observable.empty())
        }
        is Action.SetSeasonWatched -> {
            watchablesApi
                    .updateSeason(action.watchableSeasonId, action.watched)
                    .andThen(Observable.empty())
        }
        is Action.DeleteWatchable -> {
            watchablesApi
                    .setWatchableDeleted(action.watchableId)
                    .andThen(Observable.empty())
        }
        is Action.Logout -> {
            sessionService.logout()
                    .doOnComplete { UpdateWatchablesWorker.stop() }
                    .doOnComplete { DeleteWatchablesWorker.stop() }
                    .andThen(Observable.empty())
        }
        is Action.Search -> {
            emptyMutation { router follow Route.SearchIsRequired }
        }
        is Action.SetWatchablesSelected -> {
            emptyMutation { router follow Route.OnWatchableSelected(action.watchableId) }
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetWatchables -> previousState.copy(watchables = mutation.watchables)
    }

    private val watchablesMutation: Observable<out Mutation>
        get() = watchableContainerObservable
                .map { Mutation.SetWatchables(Async.Success(it)) }
                .onErrorReturn { Mutation.SetWatchables(Async.Error(it)) }
                .toObservable()

    private val watchableContainerObservable: Flowable<List<WatchableContainer>>
        get() {
            val watchablesObservable = watchablesApi.watchablesObservable
                    .startWith(emptyList<Watchable>())
            val watchableSeasonsObservable = watchablesApi.watchableSeasonsObservable
                    .startWith(emptyList<WatchableSeason>())

            return Flowables.combineLatest(watchablesObservable, watchableSeasonsObservable)
                    .map { (watchables, seasons) ->
                        watchables.asSequence()
                                .map { watchable ->
                                    val watchableSeasons = seasons.asSequence()
                                            .filter { it.watchableId == watchable.id }
                                            .sortedBy { it.index }
                                            .toList()
                                    watchable.convertToWatchableContainer(watchableSeasons)
                                }
                                .sortedWith(compareBy({ it.watchable.watched }, { it.watchable.name }))
                                .toList()
                    }
                    .skip(1) // skips initial startWith emptyLists that are needed when seasons are still empty
        }
}