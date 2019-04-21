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
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import at.florianschuster.koordinator.android.koin.coordinator
import at.florianschuster.koordinator.CoordinatorRoute
import at.florianschuster.koordinator.Router
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.reaktor.emptyMutation
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.WatchableSeason
import at.florianschuster.watchables.ui.base.BaseReactor
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.ShareService
import at.florianschuster.watchables.service.local.PrefRepo
import at.florianschuster.watchables.service.WatchablesDataSource
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.base.BaseCoordinator
import at.florianschuster.watchables.all.util.photodetail.photoDetailConsumer
import at.florianschuster.watchables.ui.main.mainScreenFabClicks
import at.florianschuster.watchables.ui.main.setMainScreenFabVisibility
import at.florianschuster.watchables.ui.watchables.recyclerview.ItemClickType
import at.florianschuster.watchables.ui.watchables.recyclerview.WatchablesAdapter
import at.florianschuster.watchables.ui.watchables.recyclerview.containerDiff
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.visibility
import com.tailoredapps.androidutil.async.Async
import com.tailoredapps.androidutil.optional.asOptional
import com.tailoredapps.androidutil.optional.filterSome
import com.tailoredapps.androidutil.ui.extensions.RxDialogAction
import com.tailoredapps.androidutil.ui.extensions.addScrolledPastItemListener
import com.tailoredapps.androidutil.ui.extensions.rxDialog
import com.tailoredapps.androidutil.ui.extensions.smoothScrollUp
import com.tailoredapps.androidutil.ui.extensions.snack
import com.tailoredapps.androidutil.optional.ofType
import com.tailoredapps.reaktor.android.koin.reactor
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.fragment_watchables.*
import kotlinx.android.synthetic.main.fragment_watchables_toolbar.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

sealed class WatchablesRoute : CoordinatorRoute {
    data class OnWatchableSelected(val id: String) : WatchablesRoute()
}

class WatchablesCoordinator : BaseCoordinator<WatchablesRoute, NavController>() {
    override fun navigate(route: WatchablesRoute, handler: NavController) {
        when (route) {
            is WatchablesRoute.OnWatchableSelected -> {
                WatchablesFragmentDirections.actionWatchablesToDetail(route.id)
            }
        }.also(handler::navigate)
    }
}

class WatchablesFragment : BaseFragment(R.layout.fragment_watchables), ReactorView<WatchablesReactor> {
    override val reactor: WatchablesReactor by reactor()
    private val adapter: WatchablesAdapter by inject()
    private val shareService: ShareService by inject { parametersOf(activity) }
    private val coordinator: WatchablesCoordinator by coordinator()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coordinator.provideNavigationHandler(findNavController())

        AnimationUtils.loadAnimation(requireContext(), R.anim.pulse).also(ivLogo::startAnimation)

        with(rvWatchables) {
            adapter = this@WatchablesFragment.adapter
            addScrolledPastItemListener { setMainScreenFabVisibility(it) }
        }

        mainScreenFabClicks()?.subscribe { rvWatchables.smoothScrollUp() }?.addTo(disposables)

        adapter.itemClick.ofType<ItemClickType.PhotoDetail>()
                .map { it.url.asOptional }
                .filterSome()
                .bind(to = requireContext().photoDetailConsumer)
                .addTo(disposables)

        bind(reactor)
    }

    override fun bind(reactor: WatchablesReactor) {
        // state
        reactor.state.changesFrom { it.watchables }
                .ofType<Async.Success<List<WatchableContainer>>>()
                .map { it.element to (adapter.data containerDiff it.element) }
                .bind(to = adapter::setData)
                .addTo(disposables)

        reactor.state.changesFrom { it.numberOfWatchables }
                .map { it > 5 }
                .bind(to = rvWatchables::setFastScrollEnabled)
                .addTo(disposables)

        reactor.state.changesFrom { it.watchablesEmpty }
                .bind(to = emptyLayout.visibility())
                .addTo(disposables)

        reactor.state.changesFrom { it.showOnboardingSnack }
                .filter { it }
                .take(1) // only show this once
                .bind {
                    rootLayout.snack(
                            R.string.onboarding_snack,
                            Snackbar.LENGTH_INDEFINITE,
                            R.string.dialog_ok
                    ) { reactor.action.accept(WatchablesReactor.Action.SetOnboardingSnackShown) }
                }
                .addTo(disposables)

        // action
        adapter.itemClick.ofType<ItemClickType.Watched>()
                .map { WatchablesReactor.Action.SetWatched(it.watchableId, it.watched) }
                .bind(to = reactor.action)
                .addTo(disposables)

        adapter.itemClick.ofType<ItemClickType.WatchedEpisode>()
                .map { WatchablesReactor.Action.SetEpisodeWatched(it.seasonId, it.episode, it.watched) }
                .bind(to = reactor.action)
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
                .flatMapMaybe { clickType ->
                    val seasonId = clickType.seasonId
                    rxDialog(R.style.DialogTheme) {
                        title = getString(R.string.dialog_options_watchable, getString(R.string.episode_name, clickType.seasonIndex, clickType.episodeIndex))
                        negativeButtonResource = R.string.dialog_cancel
                        setItems(getString(R.string.menu_watchable_season_set_watched), getString(R.string.menu_watchable_season_set_not_watched))
                    }.ofType<RxDialogAction.Selected<*>>()
                            .map { WatchablesReactor.Action.SetSeasonWatched(seasonId, it.index == 0) }
                }
                .bind(to = reactor.action)
                .addTo(disposables)

        adapter.itemClick.ofType<ItemClickType.ItemDetail>()
                .map { WatchablesReactor.Action.SelectWatchable(it.watchable.id) }
                .bind(to = reactor.action)
                .addTo(disposables)

        btnFilter.clicks()
                .flatMapSingle {
                    rxDialog(R.style.DialogTheme) {
                        titleResource = R.string.watchables_dialog_sorting_title
                        setSingleChoiceItems<WatchableContainerSortingType>(
                                WatchableContainerSortingType.values().toList(),
                                WatchableContainerSortingType.values().indexOf(reactor.currentState.sorting)
                        ) { resources.getString(it.formatted) }
                    }
                }
                .ofType<RxDialogAction.Selected<WatchableContainerSortingType>>()
                .map { WatchablesReactor.Action.SortWatchables(it.item) }
                .bind(to = reactor.action)
                .addTo(disposables)
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
}

class WatchablesReactor(
        private val watchablesDataSource: WatchablesDataSource,
        private val analyticsService: AnalyticsService,
        private val prefRepo: PrefRepo
) : BaseReactor<WatchablesReactor.Action, WatchablesReactor.Mutation, WatchablesReactor.State>(
        State(
                sorting = prefRepo.watchableContainerSortingType,
                onboardingSnackShown = prefRepo.onboardingSnackShown
        )
) {

    sealed class Action {
        data class SetWatched(val watchableId: String, val watched: Boolean) : Action()
        data class SetEpisodeWatched(val watchableSeasonId: String, val episode: String, val watched: Boolean) : Action()
        data class SetSeasonWatched(val watchableSeasonId: String, val watched: Boolean) : Action()
        data class DeleteWatchable(val watchableId: String) : Action()
        data class SelectWatchable(val watchableId: String) : Action()
        data class SortWatchables(val sorting: WatchableContainerSortingType) : Action()
        object SetOnboardingSnackShown : Action()
    }

    sealed class Mutation {
        data class SetWatchables(val watchables: Async<List<WatchableContainer>>) : Mutation()
        data class SortWatchables(val sorting: WatchableContainerSortingType) : Mutation()
        data class SetOnboardingSnackShown(val shown: Boolean) : Mutation()
    }

    data class State(
            val watchables: Async<List<WatchableContainer>> = Async.Uninitialized,
            val sorting: WatchableContainerSortingType,
            private val onboardingSnackShown: Boolean
    ) {
        val numberOfWatchables: Int
            get() = if (watchables is Async.Success) watchables.element.count() else 0

        val watchablesEmpty: Boolean
            get() = watchables is Async.Success && watchables.element.isEmpty()

        val showOnboardingSnack: Boolean
            get() = !onboardingSnackShown && watchablesEmpty
    }

    override fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> {
        val watchablesMutation = watchableContainerObservable
                .map<Async<List<WatchableContainer>>> { Async.Success(it) }
                .onErrorReturn { Async.Error(it) }
                .map { Mutation.SetWatchables(it) }
                .toObservable()
        return Observable.merge(mutation, watchablesMutation)
    }

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.SetWatched -> {
            watchablesDataSource
                    .updateWatchable(action.watchableId, action.watched)
                    .doOnComplete { analyticsService.logWatchableWatched(action.watchableId, action.watched) }
                    .toObservable()
        }
        is Action.SetEpisodeWatched -> {
            watchablesDataSource
                    .updateSeasonEpisode(action.watchableSeasonId, action.episode, action.watched)
                    .toObservable()
        }
        is Action.SetSeasonWatched -> {
            watchablesDataSource
                    .updateSeason(action.watchableSeasonId, action.watched)
                    .toObservable()
        }
        is Action.DeleteWatchable -> {
            watchablesDataSource
                    .setWatchableDeleted(action.watchableId)
                    .toObservable()
        }
        is Action.SelectWatchable -> {
            emptyMutation { Router follow WatchablesRoute.OnWatchableSelected(action.watchableId) }
        }
        is Action.SortWatchables -> {
            Single.just(action.sorting)
                    .doOnSuccess { prefRepo.watchableContainerSortingType = it }
                    .map { Mutation.SortWatchables(it) }
                    .toObservable()
        }
        is Action.SetOnboardingSnackShown -> {
            Single.just(true)
                    .doOnSuccess { prefRepo.onboardingSnackShown = it }
                    .map { Mutation.SetOnboardingSnackShown(it) }
                    .toObservable()
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetWatchables -> previousState.copy(watchables = mutation.watchables)
        is Mutation.SortWatchables -> {
            val sorted = when {
                previousState.watchables is Async.Success -> {
                    Async.Success(previousState.watchables.element.sortedWith(mutation.sorting.comparator))
                }
                else -> previousState.watchables
            }
            previousState.copy(watchables = sorted, sorting = mutation.sorting)
        }
        is Mutation.SetOnboardingSnackShown -> previousState.copy(onboardingSnackShown = mutation.shown)
    }

    private val watchableContainerObservable: Flowable<List<WatchableContainer>>
        get() {
            val watchablesObservable = watchablesDataSource.watchablesObservable
                    .startWith(emptyList<Watchable>())
            val watchableSeasonsObservable = watchablesDataSource.watchableSeasonsObservable
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
                                .sortedWith(prefRepo.watchableContainerSortingType.comparator)
                                .toList()
                    }
                    .skip(1) // skips initial startWith emptyLists that are needed when seasons are still empty
        }
}