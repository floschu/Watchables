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
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import at.florianschuster.koordinator.CoordinatorRoute
import at.florianschuster.koordinator.Router
import at.florianschuster.koordinator.android.LifecycleCoordinator
import at.florianschuster.koordinator.android.koin.coordinator
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.ViewModelReactor
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.android.koin.reactor
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.reaktor.emptyMutation
import at.florianschuster.watchables.R
import at.florianschuster.watchables.all.util.extensions.asCauseTranslation
import at.florianschuster.watchables.all.util.extensions.rxDiff
import at.florianschuster.watchables.all.util.photodetail.photoDetailConsumer
import at.florianschuster.watchables.all.worker.DeleteWatchablesWorker
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.ShareService
import at.florianschuster.watchables.service.WatchablesDataSource
import at.florianschuster.watchables.service.local.PrefRepo
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.watchables.filter.WatchableContainerFilterType
import at.florianschuster.watchables.ui.watchables.filter.WatchableContainerSortingType
import at.florianschuster.watchables.ui.watchables.filter.WatchablesFilterBottomSheetDialogFragment
import at.florianschuster.watchables.ui.watchables.filter.WatchablesFilterService
import at.florianschuster.watchables.ui.watchables.recyclerview.WatchablesAdapter
import at.florianschuster.watchables.ui.watchables.recyclerview.WatchablesAdapterInteraction
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.visibility
import com.tailoredapps.androidutil.optional.asOptional
import com.tailoredapps.androidutil.optional.filterSome
import com.tailoredapps.androidutil.optional.ofType
import com.tailoredapps.androidutil.ui.extensions.RxDialogAction
import com.tailoredapps.androidutil.ui.extensions.addScrolledPastItemListener
import com.tailoredapps.androidutil.ui.extensions.rxDialog
import com.tailoredapps.androidutil.ui.extensions.smoothScrollUp
import com.tailoredapps.androidutil.ui.extensions.snack
import com.tailoredapps.androidutil.ui.extensions.toast
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_watchables.*
import kotlinx.android.synthetic.main.fragment_watchables.emptyLayout
import kotlinx.android.synthetic.main.fragment_watchables_toolbar.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

sealed class WatchablesRoute : CoordinatorRoute {
    data class OnWatchableSelected(val id: String, val type: Watchable.Type) : WatchablesRoute()
}

class WatchablesCoordinator : LifecycleCoordinator<WatchablesRoute, NavController>() {
    override fun navigate(route: WatchablesRoute, handler: NavController) {
        when (route) {
            is WatchablesRoute.OnWatchableSelected -> {
                WatchablesFragmentDirections.actionWatchablesToDetail(route.id, route.type)
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
        coordinator provideNavigationHandler findNavController()

        with(rvWatchables) {
            adapter = this@WatchablesFragment.adapter
            addScrolledPastItemListener { fabScroll?.isVisible = it }
        }

        fabScroll.clicks().subscribe { rvWatchables.smoothScrollUp() }.addTo(disposables)

        adapter.interaction.ofType<WatchablesAdapterInteraction.PhotoDetail>()
            .map { it.url.asOptional }
            .filterSome()
            .bind(to = requireContext().photoDetailConsumer)
            .addTo(disposables)

        bind(reactor)
    }

    override fun bind(reactor: WatchablesReactor) {
        // state
        reactor.state.changesFrom { it.loading }
            .bind(to = pbLoading.visibility())
            .addTo(disposables)

        reactor.state.changesFrom { it.displayWatchables }
            .rxDiff(WatchablesAdapter.Companion::diff)
            .subscribeOn(Schedulers.computation())
            .bind(to = adapter.dataConsumer)
            .addTo(disposables)

        reactor.state.changesFrom { it.displayWatchables.isEmpty() && !it.loading }
            .bind(to = emptyLayout.visibility())
            .addTo(disposables)

        reactor.state.changesFrom { it.showOnboardingSnack }
            .filter { it }
            .take(1) // only show this once
            .bind {
                bnvSpace.snack(
                    R.string.onboarding_snack,
                    Snackbar.LENGTH_INDEFINITE,
                    R.string.dialog_ok
                ) { reactor.action.accept(WatchablesReactor.Action.SetOnboardingSnackShown) }
            }
            .addTo(disposables)

        // action
        adapter.interaction.ofType<WatchablesAdapterInteraction.Watched>()
            .map { WatchablesReactor.Action.SetWatched(it.watchableId, it.watched) }
            .bind(to = reactor.action)
            .addTo(disposables)

        adapter.interaction.ofType<WatchablesAdapterInteraction.WatchedEpisode>()
            .map { WatchablesReactor.Action.SetEpisodeWatched(it.seasonId, it.episode, it.watched) }
            .bind(to = reactor.action)
            .addTo(disposables)

        adapter.interaction.ofType<WatchablesAdapterInteraction.Options>()
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
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorResumeNext {
                Timber.e(it)
                toast(it.asCauseTranslation(resources))
                Completable.never()
            }
            .subscribe()
            .addTo(disposables)

        adapter.interaction.ofType<WatchablesAdapterInteraction.EpisodeOptions>()
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

        adapter.interaction.ofType<WatchablesAdapterInteraction.ItemDetail>()
            .map { WatchablesReactor.Action.SelectWatchable(it.watchable.id, it.watchable.type) }
            .bind(to = reactor.action)
            .addTo(disposables)

        btnFilter.clicks()
            .throttleFirst(1, TimeUnit.SECONDS)
            .bind { WatchablesFilterBottomSheetDialogFragment().show(fragmentManager) }
            .addTo(disposables)
    }

    private fun deleteWatchableDialog(watchable: Watchable): Completable {
        return rxDialog(R.style.DialogTheme) {
            titleResource = R.string.dialog_delete_watchable_title
            messageResource = R.string.dialog_delete_watchable_message
            positiveButtonResource = R.string.dialog_ok
            negativeButtonResource = R.string.dialog_cancel
        }.ofType<RxDialogAction.Positive>()
            .map { WatchablesReactor.Action.DeleteWatchable(watchable) }
            .doOnSuccess(reactor.action)
            .ignoreElement()
    }
}

class WatchablesReactor(

    private val watchablesDataSource: WatchablesDataSource,
    private val analyticsService: AnalyticsService,
    private val prefRepo: PrefRepo,
    private val watchablesFilterService: WatchablesFilterService
) : ViewModelReactor<WatchablesReactor.Action, WatchablesReactor.Mutation, WatchablesReactor.State>(
    State(
        sorting = watchablesFilterService.currentSorting,
        filtering = watchablesFilterService.currentFilter,
        onboardingSnackShown = prefRepo.onboardingSnackShown
    )
) {
    sealed class Action {
        data class SetWatched(val watchableId: String, val watched: Boolean) : Action()
        data class SetEpisodeWatched(val watchableSeasonId: String, val episode: String, val watched: Boolean) : Action()
        data class SetSeasonWatched(val watchableSeasonId: String, val watched: Boolean) : Action()
        data class DeleteWatchable(val watchable: Watchable) : Action()
        data class SelectWatchable(val id: String, val type: Watchable.Type) : Action()
        object SetOnboardingSnackShown : Action()
    }

    sealed class Mutation {
        data class SetWatchables(val watchables: List<WatchableContainer>) : Mutation()
        data class SetOnboardingSnackShown(val shown: Boolean) : Mutation()
        data class SetFilter(val filtering: WatchableContainerFilterType) : Mutation()
        data class SetSorting(val sorting: WatchableContainerSortingType) : Mutation()
    }

    data class State(
        val allWatchables: List<WatchableContainer> = emptyList(),
        val displayWatchables: List<WatchableContainer> = emptyList(),
        val sorting: WatchableContainerSortingType,
        val filtering: WatchableContainerFilterType,
        val loading: Boolean = true,
        private val onboardingSnackShown: Boolean
    ) {
        val showOnboardingSnack: Boolean
            get() = !onboardingSnackShown && displayWatchables.isEmpty()
    }

    override fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> {
        val watchablesMutation = watchablesDataSource
            .watchableContainerObservable
            .map { Mutation.SetWatchables(it) }
            .toObservable()
        val filterMutation = watchablesFilterService.filter
            .toObservable()
            .map(Mutation::SetFilter)
        val sortingMutation = watchablesFilterService.sorting
            .toObservable()
            .map(Mutation::SetSorting)
        return Observable.merge(mutation, watchablesMutation, filterMutation, sortingMutation)
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
                .setWatchableDeleted(action.watchable.id)
                .doOnComplete { analyticsService.logWatchableDelete(action.watchable) }
                .doOnComplete { DeleteWatchablesWorker.once() }
                .toObservable()
        }
        is Action.SelectWatchable -> {
            emptyMutation { Router follow WatchablesRoute.OnWatchableSelected(action.id, action.type) }
        }
        is Action.SetOnboardingSnackShown -> {
            Single.just(true)
                .doOnSuccess { prefRepo.onboardingSnackShown = it }
                .map { Mutation.SetOnboardingSnackShown(it) }
                .toObservable()
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetWatchables -> {
            previousState.copy(
                allWatchables = mutation.watchables,
                displayWatchables = mutation.watchables.sortAndFilter(
                    currentState.filtering,
                    currentState.sorting
                ),
                loading = false
            )
        }
        is Mutation.SetFilter -> {
            previousState.copy(
                displayWatchables = previousState.allWatchables.sortAndFilter(
                    mutation.filtering,
                    currentState.sorting
                ),
                filtering = mutation.filtering
            )
        }
        is Mutation.SetSorting -> {
            previousState.copy(
                displayWatchables = previousState.allWatchables.sortAndFilter(
                    currentState.filtering,
                    mutation.sorting
                ),
                sorting = mutation.sorting
            )
        }
        is Mutation.SetOnboardingSnackShown -> previousState.copy(onboardingSnackShown = mutation.shown)
    }

    private fun List<WatchableContainer>.sortAndFilter(
        filtering: WatchableContainerFilterType,
        sorting: WatchableContainerSortingType
    ): List<WatchableContainer> = asSequence()
        .filter(filtering.predicate)
        .sortedWith(sorting.comparator)
        .toList()
}
