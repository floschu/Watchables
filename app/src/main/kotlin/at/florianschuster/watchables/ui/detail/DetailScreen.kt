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

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.changesFrom
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
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.base.BaseReactor
import at.florianschuster.watchables.util.extensions.asFormattedString
import at.florianschuster.watchables.util.extensions.openChromeTab
import at.florianschuster.watchables.util.srcBlurConsumer
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.globalLayouts
import com.tailoredapps.androidutil.async.Async
import com.tailoredapps.androidutil.extensions.RxDialogAction
import com.tailoredapps.androidutil.extensions.observeOnMain
import com.tailoredapps.androidutil.extensions.rxDialog
import com.tailoredapps.androidutil.extensions.toObservableDefault
import com.tailoredapps.androidutil.optional.Optional
import com.tailoredapps.androidutil.optional.asOptional
import com.tailoredapps.androidutil.optional.filterSome
import com.tailoredapps.androidutil.optional.ofType
import com.tailoredapps.reaktor.koin.reactor
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_detail.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.concurrent.TimeUnit

class DetailFragment : BaseFragment(R.layout.fragment_detail), ReactorView<DetailReactor> {
    private val args: DetailFragmentArgs by navArgs()

    override val reactor: DetailReactor by reactor { parametersOf(args.itemId) }

    private val errorTranslationService: ErrorTranslationService by inject()
    private val detailMediaAdapter: DetailMediaAdapter by inject()
    private val optionsAdapter: OptionsAdapter by inject()
    private val shareService: ShareService by inject { parametersOf(activity) }

    private val snapHelper = LinearSnapHelper()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        btnBack.clicks().subscribe { navController.navigateUp() }.addTo(disposables)

        with(rvMedia) {
            snapHelper.attachToRecyclerView(this)
            adapter = detailMediaAdapter
        }

        rvOptions.adapter = optionsAdapter
        optionsAdapter.update()

        bind(reactor)
    }

    override fun bind(reactor: DetailReactor) {
        reactor.state.map { it.watchable.asOptional }
                .filterSome()
                .bind {
                    loading.isVisible = false
                    tvTitle.text = it.name
                    ivBackground.srcBlurConsumer(R.drawable.ic_logo).accept(it.thumbnail)
                }
                .addTo(disposables)

        reactor.state.changesFrom { it.deleteResult }
                .bind {
                    when (it) {
                        is Async.Success -> navController.navigateUp()
                        is Async.Error -> errorTranslationService.toastConsumer.accept(it.error)
                    }
                }
                .addTo(disposables)

        reactor.state.map { it.imdbId.asOptional }
                .filterSome()
                .bind { optionsAdapter.update() }
                .addTo(disposables)

        reactor.state.map { it.website.asOptional }
                .filterSome()
                .bind { optionsAdapter.update() }
                .addTo(disposables)

        reactor.state.map { it.airing.asOptional }
                .filterSome()
                .bind {
                    tvAiring.text = if (it.isBefore(ZonedDateTime.now().toLocalDate())) {
                        getString(R.string.release_date_past, it.asFormattedString)
                    } else {
                        getString(R.string.release_date_future, it.asFormattedString)
                    }
                }
                .addTo(disposables)

        reactor.state.map { it.summary.asOptional }
                .bind {
                    tvSummary.isVisible = it is Optional.Some
                    if (it is Optional.Some) {
                        tvSummary.text = it.value
                    }
                }
                .addTo(disposables)

        val snapToFirstItemObservable = rvMedia.globalLayouts()
                .map { rvMedia.calculateDistanceToPosition(snapHelper, 0) }
                .doOnNext { rvMedia.scrollBy(it.first, it.second) }
                .debounce(200, TimeUnit.MILLISECONDS)
                .takeUntil { it.first == 0 && it.second == 0 }

        reactor.state.map { DetailMediaItem.Poster(it.watchable?.thumbnail, it.watchable?.original) to it.videos }
                .map { listOf(it.first, *it.second.toTypedArray()) }
                .distinctUntilChanged()
                .observeOnMain()
                .doOnNext(detailMediaAdapter::submitList)
                .switchMap { snapToFirstItemObservable }
                .subscribe()
                .addTo(disposables)
    }

    private fun OptionsAdapter.update() {
        listOfNotNull(
                reactor.currentState.website?.let { openWebOption },
                reactor.currentState.imdbId?.let { openImdbOption },
                reactor.currentState.watchable?.let { shareOption },
                deleteOption
        ).also(::submitList)
    }

    private val openWebOption: Option.Action
        get() = Option.Action(R.string.menu_watchable_website, R.drawable.ic_open_in_browser) {
            reactor.currentState.website?.let(this::openChromeTab)
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
            rxDialog(R.style.DialogTheme) {
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
        private val itemId: String,
        private val movieDatabaseApi: MovieDatabaseApi,
        private val watchablesApi: WatchablesApi,
        private val analyticsService: AnalyticsService
) : BaseReactor<DetailReactor.Action, DetailReactor.Mutation, DetailReactor.State>(
        initialState = State(),
        initialAction = Action.LoadData
) {

    sealed class Action {
        object LoadData : Action()
        object DeleteWatchable : Action()
    }

    sealed class Mutation {
        data class DeleteWatchableResult(val deleteResult: Async<Unit>) : Mutation()
        data class SetWatchable(val watchable: Watchable?) : Mutation()
        data class SetData(
                val website: String? = null,
                val imdbId: String? = null,
                val videos: List<Videos.Video> = emptyList(),
                val summary: String? = null,
                val airing: LocalDate? = null
        ) : Mutation()
    }

    override fun transformAction(action: Observable<Action>): Observable<out Action> {
        return super.transformAction(action).doOnNext { Timber.d("Action: $it") }
    }

    data class State(
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
            // todo if change this to id + type for watchable sharing?
            watchablesApi.watchable(itemId)
                    .flatMapObservable {
                        val watchableMutation = Observable.just(Mutation.SetWatchable(it))
                        val loadMutation = when (it.type) {
                            Watchable.Type.movie -> loadMovieInfoMutation
                            Watchable.Type.show -> loadShowInfoMutation
                        }
                        Observable.concat(watchableMutation, loadMutation)
                    }
        }
        is Action.DeleteWatchable -> {
            watchablesApi.setWatchableDeleted(itemId)
                    .doOnComplete { currentState.watchable?.let(analyticsService::logWatchableDelete) }
                    .toObservableDefault(Mutation.DeleteWatchableResult(Async.Success(Unit)))
                    .onErrorReturn { Mutation.DeleteWatchableResult(Async.Error(it)) }
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.DeleteWatchableResult -> previousState.copy(deleteResult = mutation.deleteResult)
        is Mutation.SetWatchable -> previousState.copy(watchable = mutation.watchable)
        is Mutation.SetData -> {
            val youtubeVideos = mutation.videos.asSequence()
                    .filter { it.isYoutube }
                    .sortedBy { it.type }
                    .map { DetailMediaItem.YoutubeVideo(it.id, it.name, it.key, it.type) }
                    .toList()
            previousState.copy(
                    website = mutation.website,
                    imdbId = mutation.imdbId,
                    videos = youtubeVideos,
                    summary = mutation.summary,
                    airing = mutation.airing
            )
        }
    }

    private val loadMovieInfoMutation: Observable<out Mutation>
        get() = movieDatabaseApi
                .movie(itemId.toInt())
                .map { Mutation.SetData(it.website, it.imdbId, it.videos.results, it.summary, it.releaseDate) }
                .onErrorReturnItem(Mutation.SetData())
                .toObservable()

    private val loadShowInfoMutation: Observable<out Mutation>
        get() = movieDatabaseApi
                .show(itemId.toInt())
                .map { Mutation.SetData(it.website, it.externalIds.imdbId, it.videos.results, it.summary, it.nextEpisode?.airingDate) }
                .onErrorReturnItem(Mutation.SetData())
                .toObservable()
}