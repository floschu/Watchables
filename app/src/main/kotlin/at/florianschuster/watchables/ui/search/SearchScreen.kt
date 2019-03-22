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
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.reaktor.consume
import at.florianschuster.watchables.R
import at.florianschuster.watchables.ui.base.BaseReactor
import at.florianschuster.watchables.model.Search
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.service.ErrorTranslationService
import at.florianschuster.watchables.service.remote.MovieDatabaseApi
import at.florianschuster.watchables.service.remote.WatchablesApi
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.base.reactor
import at.florianschuster.watchables.util.coordinator.CoordinatorRoute
import at.florianschuster.watchables.util.coordinator.FragmentCoordinator
import at.florianschuster.watchables.util.coordinator.fragmentCoordinator
import at.florianschuster.watchables.util.photodetail.photoDetailConsumer
import at.florianschuster.watchables.worker.AddWatchableWorker
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.visibility
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.tailoredapps.androidutil.extensions.addScrolledPastItemListener
import com.tailoredapps.androidutil.extensions.afterMeasured
import com.tailoredapps.androidutil.extensions.hideKeyboard
import com.tailoredapps.androidutil.extensions.shouldLoadMore
import com.tailoredapps.androidutil.extensions.showKeyBoard
import com.tailoredapps.androidutil.extensions.smoothScrollUp
import com.tailoredapps.androidutil.extensions.toObservableDefault
import com.tailoredapps.androidutil.optional.asOptional
import com.tailoredapps.androidutil.optional.filterSome
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search_toolbar.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SearchCoordinator(fragment: Fragment) : FragmentCoordinator<SearchCoordinator.Route>(fragment) {
    enum class Route : CoordinatorRoute {
        OnDismissed
    }

    private val navController = fragment.findNavController()

    override fun navigate(to: Route) {
        when (to) {
            Route.OnDismissed -> navController.navigateUp()
        }
    }
}

class SearchFragment : BaseFragment(R.layout.fragment_search), ReactorView<SearchReactor> {
    override val reactor: SearchReactor by reactor()
    private val coordinator: SearchCoordinator by fragmentCoordinator { SearchCoordinator(this) }

    private val errorTranslationService: ErrorTranslationService by inject()
    private val adapter: SearchAdapter by inject()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        rvSearch.adapter = adapter

        ivBack.clicks()
                .doOnNext { etSearch.hideKeyboard() }
                .map { SearchCoordinator.Route.OnDismissed }
                .subscribe(coordinator::navigate)
                .addTo(disposables)

        rvSearch.setOnTouchListener { _, _ -> etSearch.hideKeyboard(); false }
        rvSearch.addScrolledPastItemListener { fabScroll.isVisible = it }

        fabScroll.clicks().subscribe { rvSearch.smoothScrollUp() }.addTo(disposables)

        ivClear.clicks()
                .subscribe {
                    rvSearch.smoothScrollUp()
                    etSearch.hideKeyboard()
                    etSearch.setText("")
                }.addTo(disposables)

        etSearch.editorActions()
                .filter { it == EditorInfo.IME_ACTION_DONE }
                .subscribe { etSearch.hideKeyboard() }
                .addTo(disposables)

        etSearch.afterMeasured { showKeyBoard() }

        adapter.imageClick.subscribe(context.photoDetailConsumer).addTo(disposables)

        bind(reactor)
    }

    override fun bind(reactor: SearchReactor) {
        reactor.state.changesFrom { it.query }
                .map { it.isNotEmpty() }
                .bind(ivClear.visibility())
                .addTo(disposables)

        reactor.state.changesFrom { it.allItems }
                .doOnNext(adapter::submitList)
                .skip(1) // initial state always empty
                .map { it.isEmpty() }
                .bind(emptyLayout.visibility())
                .addTo(disposables)

        reactor.state.changesFrom { it.loading }
                .bind(progressSearch.visibility())
                .addTo(disposables)

        reactor.state.map { it.loadingError.asOptional }
                .distinctUntilChanged()
                .filterSome()
                .bind(errorTranslationService.toastConsumer)
                .addTo(disposables)

        etSearch.textChanges()
                .debounce(300, TimeUnit.MILLISECONDS)
                .map { SearchReactor.Action.UpdateQuery(it.toString()) }
                .consume(reactor)
                .addTo(disposables)

        rvSearch.scrollEvents()
                .sample(500, TimeUnit.MILLISECONDS)
                .filter { it.view.shouldLoadMore() }
                .map { SearchReactor.Action.LoadNextPage }
                .consume(reactor)
                .addTo(disposables)

        adapter.addClick
                .filter { !it.added }
                .map { SearchReactor.Action.AddItemToWatchables(it) }
                .consume(reactor)
                .addTo(disposables)
    }
}

class SearchReactor(
    private val movieDatabaseApi: MovieDatabaseApi,
    watchablesApi: WatchablesApi
) : BaseReactor<SearchReactor.Action, SearchReactor.Mutation, SearchReactor.State>(State()) {

    sealed class Action {
        data class UpdateQuery(val query: String) : Action()
        object LoadNextPage : Action()
        data class AddItemToWatchables(val item: Search.SearchItem) : Action()
    }

    sealed class Mutation {
        data class SetQuery(val query: String) : Mutation()
        data class SetLoading(val loading: Boolean) : Mutation()
        data class SetLoadingError(val throwable: Throwable) : Mutation()
        data class SetSearchItems(val items: List<Search.SearchItem>) : Mutation()
        data class AppendSearchItems(val items: List<Search.SearchItem>) : Mutation()
        data class SetAddedWatchableIds(val watchableIds: List<String>) : Mutation()
        data class AddCurrentlyAddingWatchableId(val watchableId: String) : Mutation()
    }

    data class State(
        val query: String = "",
        val page: Int = 1,
        val allItems: List<Search.SearchItem> = emptyList(),
        val loading: Boolean = false,
        val loadingError: Throwable? = null,
        val addedWatchableIds: List<String> = emptyList()
    )

    override fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> =
            Observable.merge(watchablesMutation, mutation)

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
    }

    override fun reduce(state: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetQuery -> state.copy(query = mutation.query)
        is Mutation.SetLoading -> state.copy(loading = mutation.loading)
        is Mutation.SetLoadingError -> state.copy(loadingError = mutation.throwable)
        is Mutation.SetSearchItems -> {
            val filteredItems = mutation.items.mapAdded(state.addedWatchableIds)
            state.copy(allItems = filteredItems, page = 1)
        }
        is Mutation.AppendSearchItems -> {
            val filteredItems = state.allItems + mutation.items.mapAdded(state.addedWatchableIds)
            state.copy(allItems = filteredItems, page = state.page + 1)
        }
        is Mutation.SetAddedWatchableIds -> {
            val filteredItems = state.allItems.mapAdded(mutation.watchableIds)
            state.copy(allItems = filteredItems, addedWatchableIds = mutation.watchableIds)
        }
        is Mutation.AddCurrentlyAddingWatchableId -> {
            val newWatchableIds = (state.addedWatchableIds + mutation.watchableId).distinct()
            val filteredItems = state.allItems.mapAdded(newWatchableIds)
            state.copy(allItems = filteredItems, addedWatchableIds = newWatchableIds)
        }
    }

    private fun loadPage(query: String, page: Int): Observable<List<Search.SearchItem>> {
        val loadPageSingle = when {
            query.isEmpty() -> movieDatabaseApi.trending(page)
            else -> movieDatabaseApi.search(query, page)
        }
        return loadPageSingle
                .map { it.results.filterNotNull() }
                .toObservable()
                .takeUntil(this.action.filter { it is Action.UpdateQuery })
    }

    private val watchablesMutation = watchablesApi.watchablesObservable
            .doOnError(Timber::e).onErrorReturn { emptyList() }
            .map { it.map(Watchable::id) }
            .map(Mutation::SetAddedWatchableIds)
            .toObservable()

    private fun List<Search.SearchItem>.mapAdded(addedIds: List<String>): List<Search.SearchItem> = map { item ->
        val added = addedIds.any { it == "${item.id}" }
        if (item.added != added) item.copy(added = added) else item
    }
}