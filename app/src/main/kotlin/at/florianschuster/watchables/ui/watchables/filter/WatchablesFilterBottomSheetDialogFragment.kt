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

package at.florianschuster.watchables.ui.watchables.filter

import android.os.Bundle
import android.view.View
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.watchables.R
import at.florianschuster.watchables.ui.base.BaseBottomSheetDialogFragment
import at.florianschuster.watchables.ui.base.BaseReactor
import com.tailoredapps.androidutil.optional.asOptional
import com.tailoredapps.androidutil.optional.filterSome
import com.tailoredapps.androidutil.ui.rxviews.checked
import com.tailoredapps.androidutil.ui.rxviews.checkedChanges
import at.florianschuster.reaktor.android.koin.reactor
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_watchables_filter.*

class WatchablesFilterBottomSheetDialogFragment : BaseBottomSheetDialogFragment(
    R.layout.fragment_watchables_filter,
    WatchablesFilterBottomSheetDialogFragment::class.java.simpleName
), ReactorView<WatchablesFilterReactor> {
    override val reactor: WatchablesFilterReactor by reactor()

    private val filterChipsIdMap = mapOf(
        R.id.chipFilterAll to WatchableContainerFilterType.All,
        R.id.chipFilterMovies to WatchableContainerFilterType.Movies,
        R.id.chipFilterShows to WatchableContainerFilterType.Shows
    )

    private val sortingChipsIdMap = mapOf(
        R.id.chipSortingWatched to WatchableContainerSortingType.ByWatched,
        R.id.chipLastUpdated to WatchableContainerSortingType.ByLastUsed,
        R.id.chipSortingNameAsc to WatchableContainerSortingType.ByName,
        R.id.chipSortingTypeAsc to WatchableContainerSortingType.ByType
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind(reactor)
    }

    override fun bind(reactor: WatchablesFilterReactor) {
        cgFilter.checkedChanges()
            .skip(1)
            .map { filterChipsIdMap[it].asOptional }
            .filterSome()
            .map { WatchablesFilterReactor.Action.SelectFilter(it) }
            .bind(to = reactor.action)
            .addTo(disposables)

        cgSorting.checkedChanges()
            .skip(1)
            .map { sortingChipsIdMap[it].asOptional }
            .filterSome()
            .map { WatchablesFilterReactor.Action.SelectSorting(it) }
            .bind(to = reactor.action)
            .addTo(disposables)

        reactor.state.changesFrom { it.selectedFiltering }
            .map { filterType -> filterChipsIdMap.entries.first { it.value == filterType }.key }
            .onErrorReturn { -1 }
            .doOnNext(cgFilter.checked())
            .skip(1)
            .bind { dismiss() }
            .addTo(disposables)

        reactor.state.changesFrom { it.selectedSorting }
            .map { sortingType -> sortingChipsIdMap.entries.first { it.value == sortingType }.key }
            .onErrorReturn { -1 }
            .doOnNext(cgSorting.checked())
            .skip(1)
            .bind { dismiss() }
            .addTo(disposables)
    }
}

class WatchablesFilterReactor(
    private val watchablesFilterService: WatchablesFilterService
) : BaseReactor<WatchablesFilterReactor.Action, WatchablesFilterReactor.Mutation, WatchablesFilterReactor.State>(
    initialState = State(watchablesFilterService.currentFilter, watchablesFilterService.currentSorting)
) {
    sealed class Action {
        data class SelectFilter(val filter: WatchableContainerFilterType) : Action()
        data class SelectSorting(val sorting: WatchableContainerSortingType) : Action()
    }

    sealed class Mutation {
        data class SetFilter(val filter: WatchableContainerFilterType) : Mutation()
        data class SetSorting(val sorting: WatchableContainerSortingType) : Mutation()
    }

    data class State(
        val selectedFiltering: WatchableContainerFilterType,
        val selectedSorting: WatchableContainerSortingType
    )

    override fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> {
        val filterMutation = watchablesFilterService.filter
            .toObservable()
            .map(Mutation::SetFilter)
        val sortingMutation = watchablesFilterService.sorting
            .toObservable()
            .map(Mutation::SetSorting)
        return Observable.merge(mutation, filterMutation, sortingMutation)
    }

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.SelectFilter -> watchablesFilterService.setFilter(action.filter).toObservable()
        is Action.SelectSorting -> watchablesFilterService.setSorting(action.sorting).toObservable()
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetFilter -> previousState.copy(selectedFiltering = mutation.filter)
        is Mutation.SetSorting -> previousState.copy(selectedSorting = mutation.sorting)
    }
}