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
import at.florianschuster.watchables.all.Option
import at.florianschuster.watchables.all.OptionsAdapter
import at.florianschuster.watchables.all.util.extensions.asFormattedString
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.ErrorTranslationService
import at.florianschuster.watchables.service.ShareService
import at.florianschuster.watchables.service.remote.MovieDatabaseApi
import at.florianschuster.watchables.service.remote.WatchablesApi
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.base.BaseReactor
import at.florianschuster.watchables.all.util.extensions.openChromeTab
import at.florianschuster.watchables.all.util.srcBlurConsumer
import at.florianschuster.watchables.model.Videos
import at.florianschuster.watchables.model.originalPoster
import at.florianschuster.watchables.model.thumbnailPoster
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.globalLayouts
import com.tailoredapps.androidutil.async.Async
import com.tailoredapps.androidutil.ui.extensions.RxDialogAction
import com.tailoredapps.androidutil.ui.extensions.rxDialog
import com.tailoredapps.androidutil.ui.extensions.toObservableDefault
import com.tailoredapps.androidutil.optional.asOptional
import com.tailoredapps.androidutil.optional.filterSome
import com.tailoredapps.androidutil.optional.ofType
import com.tailoredapps.androidutil.ui.extensions.observable
import com.tailoredapps.androidutil.ui.extensions.toast
import com.tailoredapps.reaktor.android.koin.reactor
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_detail.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
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

        rvDetailOptions.adapter = optionsAdapter
        optionsAdapter.update()

        bind(reactor)
    }

    override fun bind(reactor: DetailReactor) {
        reactor.state.changesFrom { it.watchable }
                .bind { watchableAsync ->
                    loading.isVisible = watchableAsync is Async.Loading
                    when (watchableAsync) {
                        is Async.Success -> {
                            tvTitle.text = watchableAsync.element.name
                            ivBackground.srcBlurConsumer(R.drawable.ic_logo).accept(watchableAsync.element.thumbnailPoster)
                        }
                        is Async.Error -> {
                            toast(R.string.detail_error_watchable)
                            navController.navigateUp()
                        }
                    }
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

        reactor.state.changesFrom { it.additionalData }
                .bind { additionalDataAsync ->
                    when (additionalDataAsync) {
                        is Async.Success -> {
                            additionalDataAsync.element.airing?.let { airingDate ->
                                tvAiring.text = if (airingDate.isBefore(ZonedDateTime.now().toLocalDate())) {
                                    getString(R.string.release_date_past, airingDate.asFormattedString)
                                } else {
                                    getString(R.string.release_date_future, airingDate.asFormattedString)
                                }
                            }

                            val summary = additionalDataAsync.element.summary
                            tvSummary.isVisible = summary == null
                            if (summary != null) tvSummary.text = summary

                            optionsAdapter.update()
                        }
                        is Async.Error -> {
                            toast(R.string.detail_error_additional_data)
                        }
                    }
                }
                .addTo(disposables)

        val snapToFirstItemObservable = rvMedia.globalLayouts()
                .map { rvMedia.calculateDistanceToPosition(snapHelper, 0) }
                .doOnNext { rvMedia.scrollBy(it.first, it.second) }
                .debounce(200, TimeUnit.MILLISECONDS)
                .takeUntil { it.first == 0 && it.second == 0 }

        val watchablePoster = reactor.state.changesFrom { it.watchable().asOptional }
                .filterSome()
                .map { DetailMediaItem.Poster(it.thumbnailPoster, it.originalPoster) }

        val videos = reactor.state.changesFrom { it.additionalData().asOptional }
                .filterSome()
                .map { it.videos }

        Observables.combineLatest(watchablePoster, videos)
                .map { listOf(it.first) + it.second }
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(detailMediaAdapter::submitList)
                .switchMap { snapToFirstItemObservable }
                .bind()
                .addTo(disposables)
    }

    private fun OptionsAdapter.update() {
        listOfNotNull(
                reactor.currentState.additionalData()?.website?.let { openWebOption },
                reactor.currentState.additionalData()?.imdbId?.let { openImdbOption },
                reactor.currentState.watchable()?.let { shareOption },
                deleteOption
        ).also(::submitList)
    }

    private val openWebOption: Option.Action
        get() = Option.Action(R.string.menu_watchable_website, R.drawable.ic_open_in_browser) {
            reactor.currentState.additionalData()?.website?.let(this::openChromeTab)
        }

    private val openImdbOption: Option.Action
        get() = Option.Action(R.string.menu_watchable_imdb, R.drawable.ic_imdb) {
            reactor.currentState.additionalData()?.imdbId?.let {
                openChromeTab(getString(R.string.imdb_search, it))
            }
        }

    private val shareOption: Option.Action
        get() = Option.Action(R.string.menu_watchable_share, R.drawable.ic_share) {
            reactor.currentState.watchable()?.let {
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
) : BaseReactor<DetailReactor.Action, DetailReactor.Mutation, DetailReactor.State>(State()) {

    sealed class Action {
        data class LoadAdditionalData(val watchable: Watchable) : Action()
        object DeleteWatchable : Action()
        data class SetWatched(val watched: Boolean) : Action()
    }

    sealed class Mutation {
        data class DeleteWatchableResult(val deleteResult: Async<Unit>) : Mutation()
        data class SetWatchable(val watchable: Async<Watchable>) : Mutation()
        data class SetAdditionalData(val additionalData: Async<State.AdditionalData>) : Mutation()
    }

    data class State(
            val watchable: Async<Watchable> = Async.Uninitialized,
            val additionalData: Async<AdditionalData> = Async.Uninitialized,
            val deleteResult: Async<Unit> = Async.Uninitialized
    ) {
        data class AdditionalData(
                val website: String? = null,
                val imdbId: String? = null,
                val videos: List<DetailMediaItem.YoutubeVideo> = emptyList(),
                val summary: String? = null,
                val airing: LocalDate? = null
        )
    }

    override fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> =
            Observable.merge(mutation, loadDataMutation)

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.LoadAdditionalData -> {
            val loading = Mutation.SetAdditionalData(Async.Loading).observable

            val additionalInfoLoad = when {
                action.watchable.type == Watchable.Type.movie -> loadAdditionalInfoFromMovie()
                else -> loadAdditionalInfoFromShow()
            }
            val load = additionalInfoLoad
                    .map { Mutation.SetAdditionalData(Async.Success(it)) }
                    .onErrorReturn { Mutation.SetAdditionalData(Async.Error(it)) }
                    .toObservable()

            Observable.concat(loading, load)
        }
        is Action.DeleteWatchable -> {
            watchablesApi.setWatchableDeleted(itemId)
                    .doOnComplete { currentState.watchable()?.let(analyticsService::logWatchableDelete) }
                    .toObservableDefault(Mutation.DeleteWatchableResult(Async.Success(Unit)))
                    .onErrorReturn { Mutation.DeleteWatchableResult(Async.Error(it)) }
        }
        is Action.SetWatched -> {
            watchablesApi.updateWatchable(itemId, action.watched).toObservable()
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.DeleteWatchableResult -> previousState.copy(deleteResult = mutation.deleteResult)
        is Mutation.SetWatchable -> previousState.copy(watchable = mutation.watchable)
        is Mutation.SetAdditionalData -> previousState.copy(additionalData = mutation.additionalData)
    }

    private val loadDataMutation: Observable<out Mutation>
        get() {
            val loading = Mutation.SetWatchable(Async.Loading).observable
            val watchable = watchablesApi.watchable(itemId)
                    .map { Mutation.SetWatchable(Async.Success(it)) }
                    .onErrorReturn { Mutation.SetWatchable(Async.Error(it)) }
                    .toObservable()
                    .switchMap { watchableMutation ->
                        if (watchableMutation.watchable is Async.Success) {
                            Observable.concat(
                                    watchableMutation.observable,
                                    mutate(Action.LoadAdditionalData(watchableMutation.watchable.element))
                            )
                        } else {
                            watchableMutation.observable
                        }
                    }
            return Observable.concat(loading, watchable)
        }

    private fun loadAdditionalInfoFromMovie(): Single<State.AdditionalData> {
        return movieDatabaseApi
                .movie(itemId.toInt())
                .map {
                    State.AdditionalData(
                            it.website,
                            it.imdbId,
                            it.videos.results.mapToYoutubeVideos(),
                            it.summary,
                            it.releaseDate
                    )
                }
    }

    private fun loadAdditionalInfoFromShow(): Single<State.AdditionalData> {
        return movieDatabaseApi
                .show(itemId.toInt())
                .map {
                    State.AdditionalData(
                            it.website,
                            it.externalIds.imdbId,
                            it.videos.results.mapToYoutubeVideos(),
                            it.summary,
                            it.nextEpisode?.airingDate
                    )
                }
    }

    private fun List<Videos.Video>.mapToYoutubeVideos(): List<DetailMediaItem.YoutubeVideo> {
        return asSequence()
                .filter { it.isYoutube }
                .sortedBy { it.type }
                .map { DetailMediaItem.YoutubeVideo(it.id, it.name, it.key, it.type) }
                .toList()
    }
}