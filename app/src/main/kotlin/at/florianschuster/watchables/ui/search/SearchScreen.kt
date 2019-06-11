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

package at.florianschuster.watchables.ui.search

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.navigation.NavController
import at.florianschuster.koordinator.CoordinatorRoute
import at.florianschuster.koordinator.Router
import at.florianschuster.koordinator.android.koin.coordinator
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.reaktor.emptyMutation
import at.florianschuster.watchables.R
import at.florianschuster.watchables.all.util.extensions.asCauseTranslation
import at.florianschuster.watchables.ui.base.BaseReactor
import at.florianschuster.watchables.model.Search
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.service.remote.MovieDatabaseApi
import at.florianschuster.watchables.service.WatchablesDataSource
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.all.util.photodetail.photoDetailConsumer
import at.florianschuster.watchables.all.worker.AddWatchableWorker
import at.florianschuster.watchables.ui.base.BaseCoordinator
import at.florianschuster.watchables.ui.main.bnvReselects
import at.florianschuster.watchables.ui.main.mainFabClicks
import at.florianschuster.watchables.ui.main.setMainScreenFabVisibility
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.visibility
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.tailoredapps.androidutil.ui.extensions.addScrolledPastItemListener
import com.tailoredapps.androidutil.ui.extensions.hideKeyboard
import com.tailoredapps.androidutil.ui.extensions.shouldLoadMore
import com.tailoredapps.androidutil.ui.extensions.smoothScrollUp
import com.tailoredapps.androidutil.ui.extensions.toObservableDefault
import com.tailoredapps.androidutil.optional.asOptional
import com.tailoredapps.androidutil.optional.filterSome
import com.tailoredapps.androidutil.ui.extensions.showKeyBoard
import com.tailoredapps.androidutil.ui.extensions.toast
import at.florianschuster.reaktor.android.koin.reactor
import at.florianschuster.watchables.model.toWatchableType
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.emptyLayout
import kotlinx.android.synthetic.main.fragment_search_toolbar.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

sealed class SearchRoute : CoordinatorRoute {
    data class OnAddedItemSelected(val item: Search.Result) : SearchRoute()
}

class SearchCoordinator : BaseCoordinator<SearchRoute, NavController>() {
    override fun navigate(route: SearchRoute, handler: NavController) {
        when (route) {
            is SearchRoute.OnAddedItemSelected -> {
                SearchFragmentDirections.actionSearchToDetail("${route.item.id}", route.item.toWatchableType())
            }
        }.also(handler::navigate)
    }
}

class SearchFragment : BaseFragment(R.layout.fragment_search), ReactorView<SearchReactor> {
    override val reactor: SearchReactor by reactor()
    private val coordinator: SearchCoordinator by coordinator()
    private val adapter: SearchAdapter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind(reactor)
    }

    override fun bind(reactor: SearchReactor) {
        coordinator.provideNavigationHandler(navController)

        with(rvSearch) {
            adapter = this@SearchFragment.adapter
            setOnTouchListener { _, _ -> etSearch.hideKeyboard(); false }
            addScrolledPastItemListener { setMainScreenFabVisibility(it) }
        }

        mainFabClicks.subscribe { rvSearch.smoothScrollUp() }.addTo(disposables)

        bnvReselects.filter { it.itemId == R.id.search }
            .bind {
                if (etSearch.text.isNotEmpty()) etSearch.setText("")
                etSearch.showKeyBoard()
                rvSearch.smoothScrollUp()
            }
            .addTo(disposables)

        ivClear.clicks()
            .bind {
                etSearch.setText("")
                etSearch.hideKeyboard()
                rvSearch.smoothScrollUp()
            }
            .addTo(disposables)

        etSearch.editorActions()
            .filter { it == EditorInfo.IME_ACTION_DONE }
            .subscribe { etSearch.hideKeyboard() }
            .addTo(disposables)

        adapter.interaction
            .ofType<SearchAdapterInteraction.ImageClick>()
            .map { it.imageUrl.asOptional }
            .filterSome()
            .bind(to = requireContext().photoDetailConsumer)
            .addTo(disposables)

        etSearch.textChanges()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { SearchReactor.Action.UpdateQuery(it.toString()) }
            .bind(to = reactor.action)
            .addTo(disposables)

        rvSearch.scrollEvents()
            .sample(500, TimeUnit.MILLISECONDS)
            .filter { it.view.shouldLoadMore() }
            .map { SearchReactor.Action.LoadNextPage }
            .bind(to = reactor.action)
            .addTo(disposables)

        adapter.interaction
            .ofType<SearchAdapterInteraction.AddItemClick>()
            .map { SearchReactor.Action.AddItemToWatchables(it.item) }
            .bind(to = reactor.action)
            .addTo(disposables)

        adapter.interaction
            .ofType<SearchAdapterInteraction.OpenItemClick>()
            .map { SearchReactor.Action.OnItemSelect(it.item) }
            .bind(to = reactor.action)
            .addTo(disposables)

        reactor.state.changesFrom { it.query }
            .map { it.isNotEmpty() }
            .bind(to = ivClear.visibility())
            .addTo(disposables)

        reactor.state.changesFrom { it.allItems }
            .doOnNext(adapter::submitList)
            .skip(1) // initial state always empty
            .map { it.isEmpty() }
            .bind(to = emptyLayout.visibility())
            .addTo(disposables)

        reactor.state.changesFrom { it.loading }
            .bind(to = progressSearch.visibility())
            .addTo(disposables)

        reactor.state.map { it.loadingError.asOptional }
            .distinctUntilChanged()
            .filterSome()
            .bind { toast(it.asCauseTranslation(resources)) }
            .addTo(disposables)
    }
}

class SearchReactor(
    private val movieDatabaseApi: MovieDatabaseApi,
    private val watchablesDataSource: WatchablesDataSource
) : BaseReactor<SearchReactor.Action, SearchReactor.Mutation, SearchReactor.State>(State()) {

    sealed class Action {
        data class UpdateQuery(val query: String) : Action()
        object LoadNextPage : Action()
        data class AddItemToWatchables(val item: Search.Result) : Action()
        data class OnItemSelect(val item: Search.Result) : Action()
    }

    sealed class Mutation {
        data class SetQuery(val query: String) : Mutation()
        data class SetLoading(val loading: Boolean) : Mutation()
        data class SetLoadingError(val throwable: Throwable) : Mutation()
        data class SetSearchItems(val items: List<Search.Result>) : Mutation()
        data class AppendSearchItems(val items: List<Search.Result>) : Mutation()
        data class SetAddedWatchableIds(val watchableIds: List<String>) : Mutation()
        data class AddCurrentlyAddingWatchableId(val watchableId: String) : Mutation()
    }

    data class State(
        val query: String = "",
        val page: Int = 1,
        val allItems: List<Search.Result> = emptyList(),
        val loading: Boolean = false,
        val loadingError: Throwable? = null,
        val addedWatchableIds: List<String> = emptyList()
    )

    override fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> {
        val watchablesMutation = watchablesDataSource.watchablesObservable
            .doOnError(Timber::e).onErrorReturn { emptyList() }
            .map { it.map(Watchable::id) }
            .map(Mutation::SetAddedWatchableIds)
            .toObservable()
        return Observable.merge(watchablesMutation, mutation)
    }

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.UpdateQuery -> {
            val queryMutation = Observable.just(Mutation.SetQuery(action.query))
            val loadMutation = Observable.just(Mutation.SetLoading(true))
            val firstPageMutation = loadPage(action.query, 1)
                .map<Mutation>(Mutation::SetSearchItems)
                .onErrorReturn { Mutation.SetLoadingError(it) }
            val endLoadMutation = Observable.just(Mutation.SetLoading(false))
            Observable.concat(queryMutation, loadMutation, firstPageMutation, endLoadMutation)
        }
        is Action.LoadNextPage -> {
            if (currentState.loading) Observable.empty()
            else {
                val loadMutation = Observable.just(Mutation.SetLoading(true))
                val nextPageMutation = loadPage(currentState.query, currentState.page + 1)
                    .map<Mutation>(Mutation::AppendSearchItems)
                    .onErrorReturn { Mutation.SetLoadingError(it) }
                val endLoadMutation = Observable.just(Mutation.SetLoading(false))
                Observable.concat(loadMutation, nextPageMutation, endLoadMutation)
            }
        }
        is Action.AddItemToWatchables -> {
            Completable.fromAction { AddWatchableWorker.start(action.item) }
                .toObservableDefault("${action.item.id}")
                .map(Mutation::AddCurrentlyAddingWatchableId)
        }
        is Action.OnItemSelect -> {
            emptyMutation { Router follow SearchRoute.OnAddedItemSelected(action.item) }
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetQuery -> previousState.copy(query = mutation.query)
        is Mutation.SetLoading -> previousState.copy(loading = mutation.loading)
        is Mutation.SetLoadingError -> previousState.copy(loadingError = mutation.throwable)
        is Mutation.SetSearchItems -> {
            val filteredItems = mutation.items.mapAdded(previousState.addedWatchableIds)
            previousState.copy(allItems = filteredItems, page = 1)
        }
        is Mutation.AppendSearchItems -> {
            val filteredItems = previousState.allItems + mutation.items.mapAdded(previousState.addedWatchableIds)
            previousState.copy(allItems = filteredItems, page = previousState.page + 1)
        }
        is Mutation.SetAddedWatchableIds -> {
            val filteredItems = previousState.allItems.mapAdded(mutation.watchableIds)
            previousState.copy(allItems = filteredItems, addedWatchableIds = mutation.watchableIds)
        }
        is Mutation.AddCurrentlyAddingWatchableId -> {
            val newWatchableIds = (previousState.addedWatchableIds + mutation.watchableId).distinct()
            val filteredItems = previousState.allItems.mapAdded(newWatchableIds)
            previousState.copy(allItems = filteredItems, addedWatchableIds = newWatchableIds)
        }
    }

    private fun loadPage(query: String, page: Int): Observable<List<Search.Result>> {
        val loadPageSingle = when {
            query.isEmpty() -> movieDatabaseApi.trending(page)
            else -> movieDatabaseApi.search(query, page)
        }
        return loadPageSingle
            .map { it.results.filterNotNull() }
            .toObservable()
            .takeUntil(this.action.filter { it is Action.UpdateQuery })
    }

    private fun List<Search.Result>.mapAdded(addedIds: List<String>): List<Search.Result> = map { item ->
        val added = addedIds.any { it == "${item.id}" }
        if (item.added != added) item.copyWith(added) else item
    }
}