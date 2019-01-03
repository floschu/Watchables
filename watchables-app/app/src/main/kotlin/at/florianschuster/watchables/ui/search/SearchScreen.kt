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

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.ui.base.reactor.BaseReactor
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.Search
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.thumbnail
import at.florianschuster.watchables.service.ErrorTranslationService
import at.florianschuster.watchables.service.remote.MovieDatabaseApi
import at.florianschuster.watchables.service.remote.WatchablesApi
import at.florianschuster.watchables.ui.base.views.ReactorFragment
import at.florianschuster.watchables.util.extensions.*
import at.florianschuster.watchables.util.photodetail.photoDetailConsumer
import at.florianschuster.watchables.worker.AddWatchableWorker
import com.jakewharton.rxbinding3.material.visibility
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.visibility
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search_toolbar.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit


class SearchFragment : ReactorFragment<SearchReactor>(R.layout.fragment_search) {
    override val reactor: SearchReactor by reactor()

    private val errorTranslationService: ErrorTranslationService by inject()
    private val adapter: SearchAdapter by inject()

    override fun bind(reactor: SearchReactor) {
        rvSearch.adapter = adapter

        ivBack.clicks().subscribe {
            etSearch.hideKeyboard()
            navController.navigateUp()
        }.addTo(disposable)

        rvSearch.setOnTouchListener { _, _ -> etSearch.hideKeyboard(); false }
        rvSearch.addScrolledPastFirstItemListener(fabScroll.visibility())

        fabScroll.clicks().subscribe { rvSearch.smoothScrollUp() }.addTo(disposable)

        ivClear.clicks()
                .subscribe {
                    rvSearch.smoothScrollUp()
                    etSearch.hideKeyboard()
                    etSearch.setText("")
                }.addTo(disposable)

        etSearch.editorActions()
                .filter { it == EditorInfo.IME_ACTION_DONE }
                .subscribe { etSearch.hideKeyboard() }
                .addTo(disposable)

        etSearch.afterMeasured { showKeyBoard() }

        adapter.imageClick.subscribe(context.photoDetailConsumer).addTo(disposable)

        //state
        reactor.state.map { it.query }
                .distinctUntilChanged()
                .map { it.isNotEmpty() }
                .subscribe(ivClear.visibility())
                .addTo(disposable)

        reactor.state.map { it.allItems }
                .distinctUntilChanged()
                .doOnNext(adapter::submitList)
                .skip(1) //initial state always empty
                .map { it.isEmpty() }
                .subscribe(emptyLayout.visibility())
                .addTo(disposable)

        reactor.state.map { it.loading }
                .distinctUntilChanged()
                .subscribe(progressSearch.visibility())
                .addTo(disposable)

        reactor.state.flatMapOptionalAsMaybe { it.loadingError }
                .distinctUntilChanged()
                .subscribe(errorTranslationService::toast)
                .addTo(disposable)

        //action
        etSearch.textChanges()
                .debounce(300, TimeUnit.MILLISECONDS)
                .map { SearchReactor.Action.UpdateQuery(it.toString()) }
                .subscribe(reactor.action)
                .addTo(disposable)

        rvSearch.scrollEvents()
                .sample(500, TimeUnit.MILLISECONDS)
                .filter { it.view.shouldLoadMore() }
                .map { SearchReactor.Action.LoadNextPage }
                .subscribe(reactor.action)
                .addTo(disposable)

        adapter.addClick
                .filter { !it.added }
                .map { SearchReactor.Action.AddItemToWatchables(it) }
                .subscribe(reactor.action)
                .addTo(disposable)
    }

    private fun RecyclerView.shouldLoadMore(threshold: Int = 8): Boolean {
        val layoutManager = layoutManager ?: return false
        return when (layoutManager) {
            is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition() + threshold > layoutManager.itemCount
            else -> false
        }
    }

    private fun View.showKeyBoard() {
        requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun View.hideKeyboard() {
        val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (inputMethodManager.isActive) inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
        clearFocus()
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