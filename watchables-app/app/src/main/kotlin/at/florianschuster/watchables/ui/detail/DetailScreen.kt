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

package at.florianschuster.watchables.ui.detail

import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.Videos
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.original
import at.florianschuster.watchables.model.thumbnail
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.ErrorTranslationService
import at.florianschuster.watchables.service.ShareService
import at.florianschuster.watchables.service.remote.MovieDatabaseApi
import at.florianschuster.watchables.service.remote.WatchablesApi
import at.florianschuster.watchables.ui.base.reactor.BaseReactor
import at.florianschuster.watchables.ui.base.reactor.ReactorFragment
import at.florianschuster.watchables.ui.base.reactor.reactor
import at.florianschuster.watchables.util.*
import at.florianschuster.watchables.util.extensions.*
import com.jakewharton.rxbinding3.view.globalLayouts
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_detail.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import java.util.concurrent.TimeUnit


const val ARG_DETAIL_ITEM_ID = "DetailFragment.args"

class DetailFragment : ReactorFragment<DetailReactor>(R.layout.fragment_detail) {
    private val itemId: String by argument(ARG_DETAIL_ITEM_ID)

    override val reactor: DetailReactor by reactor { parametersOf(itemId) }

    private val errorTranslationService: ErrorTranslationService by inject()
    private val detailMediaAdapter: DetailMediaAdapter by inject()
    private val optionsAdapter: OptionsAdapter by inject()
    private val shareService: ShareService by inject { parametersOf(activity) }

    override fun bind(reactor: DetailReactor) {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { navController.navigateUp() }

        val snapHelper = LinearSnapHelper().also { it.attachToRecyclerView(rvMedia) }
        rvMedia.adapter = detailMediaAdapter

        rvOptions.adapter = optionsAdapter
        updateOptions()

        //state
        reactor.state.flatMapOptionalAsMaybe { it.watchable }
                .subscribe {
                    loading.isVisible = false
                    tvTitle.text = it.name
                    ivBackground.srcBlurConsumer(R.drawable.ic_logo).accept(it.thumbnail)
                }
                .addTo(disposables)

        reactor.state.map { it.deleteResult }
                .subscribe {
                    when (it) {
                        is Async.Success -> navController.navigateUp()
                        is Async.Error -> errorTranslationService.toastConsumer.accept(it.error)
                    }
                }
                .addTo(disposables)

        reactor.state.flatMapOptionalAsMaybe { it.imdbId }
                .doOnEach { updateOptions() }
                .subscribe()
                .addTo(disposables)

        reactor.state.flatMapOptionalAsMaybe { it.website }
                .doOnEach { updateOptions() }
                .subscribe()
                .addTo(disposables)

        reactor.state.flatMapOptionalAsMaybe { it.airing }
                .subscribe {
                    tvAiring.text = if (it.isBefore(ZonedDateTime.now().toLocalDate())) {
                        getString(R.string.release_date_past, it.asFormattedString)
                    } else {
                        getString(R.string.release_date_future, it.asFormattedString)
                    }
                }
                .addTo(disposables)

        reactor.state.flatMapOptionalAsMaybe { it.summary }
                .doOnEach { tvSummary.isVisible = it.isOnNext }
                .doOnNext(tvSummary::setText)
                .subscribe()
                .addTo(disposables)

        val snapToFirstItemObservable = rvMedia.globalLayouts()
                .map { rvMedia.calculateDistanceToPosition(snapHelper, 0) }
                .doOnNext { rvMedia.scrollBy(it.first, it.second) }
                .debounce(200, TimeUnit.MILLISECONDS)
                .takeUntil { it.first == 0 && it.second == 0 }

        reactor.state.map { DetailMediaItem.Poster(it.watchable?.thumbnail, it.watchable?.original) to it.videos }
                .map { listOf(it.first, *it.second.toTypedArray()) }
                .distinctUntilChanged()
                .doOnNext(detailMediaAdapter::submitList)
                .switchMap { snapToFirstItemObservable }
                .subscribe()
                .addTo(disposables)

        reactor.action.accept(DetailReactor.Action.LoadData)
    }

    private fun updateOptions() {
        optionsAdapter.submitList(listOfNotNull(
                reactor.currentState.website?.let { openWebOption },
                reactor.currentState.imdbId?.let { openImdbOption },
                reactor.currentState.watchable?.let { shareOption },
                deleteOption
        ))
    }

    private val openWebOption: Option.Action
        get() = Option.Action(R.string.menu_watchable_website, R.drawable.ic_open_in_browser) {
            reactor.currentState.website?.let(::openChromeTab)
        }

    private val openImdbOption: Option.Action
        get() = Option.Action(R.string.menu_watchable_imdb, R.drawable.ic_imdb) {
            val imdbId = reactor.currentState.imdbId
            openChromeTab(getString(R.string.imdb_search, imdbId))
        }

    private val shareOption: Option.Action
        get() = Option.Action(R.string.menu_watchable_share, R.drawable.ic_share) {
            reactor.currentState.watchable?.let {
                shareService.share(it).subscribe().addTo(disposables)
            }
        }

    private val deleteOption: Option.Action
        get() = Option.Action(R.string.menu_watchable_delete, R.drawable.ic_delete_forever) {
            rxDialog {
                titleResource = R.string.dialog_delete_watchable_title
                messageResource = R.string.dialog_delete_watchable_message
                positiveButtonResource = R.string.dialog_ok
                negativeButtonResource = R.string.dialog_cancel
            }.ofType<RxDialogAction.Positive>()
                    .map { DetailReactor.Action.DeleteWatchable }
                    .subscribe(reactor.action)
                    .addTo(disposables)
        }

    private fun RecyclerView.calculateDistanceToPosition(snapHelper: LinearSnapHelper, position: Int): Pair<Int, Int> {
        val layoutManager = layoutManager as? LinearLayoutManager ?: return 0 to 0
        val child = layoutManager.findViewByPosition(position) ?: return 0 to 0
        val distances = snapHelper.calculateDistanceToFinalSnap(layoutManager, child)
                ?: return 0 to 0
        return distances[0] to distances[1]
    }
}


class DetailReactor(
        itemId: String,
        private val movieDatabaseApi: MovieDatabaseApi,
        private val watchablesApi: WatchablesApi,
        private val analyticsService: AnalyticsService
) : BaseReactor<DetailReactor.Action, DetailReactor.Mutation, DetailReactor.State>(State(itemId)) {

    sealed class Action {
        object LoadData : Action()
        object DeleteWatchable : Action()
    }

    sealed class Mutation {
        data class DeleteWatchableResult(val deleteResult: Async<Unit>) : Mutation()
        data class SetData(
                val watchable: Watchable? = null,
                val website: String? = null,
                val imdbId: String? = null,
                val videos: List<Videos.Video> = emptyList(),
                val summary: String? = null,
                val airing: LocalDate? = null
        ) : Mutation()
    }

    data class State(
            val itemId: String,
            val watchable: Watchable? = null,
            val deleteResult: Async<Unit> = Async.Uninitialized,
            val website: String? = null,
            val imdbId: String? = null,
            val videos: List<DetailMediaItem.YoutubeVideo> = emptyList(),
            val summary: String? = null,
            val airing: LocalDate? = null
    )

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.LoadData -> {
            watchablesApi.watchable(currentState.itemId)
                    .flatMapObservable {
                        when (it.type) {
                            Watchable.Type.movie -> loadMovieInfoMutation(it)
                            Watchable.Type.show -> loadShowInfoMutation(it)
                        }
                    }
        }
        is Action.DeleteWatchable -> {
            watchablesApi.setWatchableDeleted(currentState.itemId)
                    .doOnComplete { currentState.watchable?.let(analyticsService::logWatchableDelete) }
                    .toObservableDefault(Mutation.DeleteWatchableResult(Async.Success(Unit)))
                    .onErrorReturn { Mutation.DeleteWatchableResult(Async.Error(it)) }
        }
    }

    override fun reduce(state: State, mutation: Mutation): State = when (mutation) {
        is Mutation.DeleteWatchableResult -> state.copy(deleteResult = mutation.deleteResult)
        is Mutation.SetData -> {
            val youtubeVideos = mutation.videos
                    .filter { it.isYoutube }
                    .sortedBy { it.type }
                    .map { DetailMediaItem.YoutubeVideo(it.id, it.name, it.key, it.type) }
            state.copy(
                    watchable = mutation.watchable,
                    website = mutation.website,
                    imdbId = mutation.imdbId,
                    videos = youtubeVideos,
                    summary = mutation.summary,
                    airing = mutation.airing
            )
        }
    }

    private fun loadMovieInfoMutation(watchable: Watchable) = movieDatabaseApi
            .movie(currentState.itemId.toInt())
            .map { Mutation.SetData(watchable, it.website, it.imdbId, it.videos.results, it.summary, it.releaseDate) }
            .onErrorReturnItem(Mutation.SetData())
            .toObservable()


    private fun loadShowInfoMutation(watchable: Watchable) = movieDatabaseApi
            .show(currentState.itemId.toInt())
            .map { Mutation.SetData(watchable, it.website, it.externalIds.imdbId, it.videos.results, it.summary, it.nextEpisode?.airingDate) }
            .onErrorReturnItem(Mutation.SetData())
            .toObservable()
}