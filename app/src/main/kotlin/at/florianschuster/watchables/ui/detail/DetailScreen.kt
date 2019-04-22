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
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.koordinator.CoordinatorRoute
import at.florianschuster.koordinator.Router
import at.florianschuster.koordinator.android.koin.coordinator
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.reaktor.android.bind
import at.florianschuster.reaktor.changesFrom
import at.florianschuster.reaktor.emptyMutation
import at.florianschuster.watchables.R
import at.florianschuster.watchables.all.Option
import at.florianschuster.watchables.all.OptionsAdapter
import at.florianschuster.watchables.all.util.extensions.asCauseTranslation
import at.florianschuster.watchables.all.util.extensions.asFormattedString
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.ShareService
import at.florianschuster.watchables.service.remote.MovieDatabaseApi
import at.florianschuster.watchables.service.WatchablesDataSource
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.base.BaseReactor
import at.florianschuster.watchables.all.util.extensions.openChromeTab
import at.florianschuster.watchables.all.util.srcBlurConsumer
import at.florianschuster.watchables.all.worker.AddWatchableWorker
import at.florianschuster.watchables.all.worker.DeleteWatchablesWorker
import at.florianschuster.watchables.model.Credits
import at.florianschuster.watchables.model.Images
import at.florianschuster.watchables.model.Videos
import at.florianschuster.watchables.model.convertToSearchType
import at.florianschuster.watchables.model.originalPoster
import at.florianschuster.watchables.model.thumbnailPoster
import at.florianschuster.watchables.ui.base.BaseCoordinator
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
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.fragment_detail.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.concurrent.TimeUnit

enum class DetailRoute : CoordinatorRoute {
    Pop
}

class DetailCoordinator : BaseCoordinator<DetailRoute, NavController>() {
    override fun navigate(route: DetailRoute, handler: NavController) {
        when (route) {
            DetailRoute.Pop -> handler.navigateUp()
        }
    }
}

class DetailFragment : BaseFragment(R.layout.fragment_detail), ReactorView<DetailReactor> {
    private val args: DetailFragmentArgs by navArgs()
    override val reactor: DetailReactor by reactor { parametersOf(args.id, args.type) }
    private val coordinator: DetailCoordinator by coordinator()
    private val detailMediaAdapter: DetailMediaAdapter by inject()
    private val optionsAdapter: OptionsAdapter by inject()
    private val shareService: ShareService by inject { parametersOf(activity) }

    private val snapHelper = LinearSnapHelper()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coordinator.provideNavigationHandler(navController)

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
        reactor.state.changesFrom { it.deleteResult }
                .bind { deleteAsync ->
                    loading.isVisible = deleteAsync.loading
                    if (deleteAsync is Async.Error) toast(deleteAsync.error.asCauseTranslation(resources))
                }
                .addTo(disposables)

        reactor.state.changesFrom { it.additionalData }
                .bind { additionalDataAsync ->
                    when (additionalDataAsync) {
                        is Async.Success -> {
                            tvTitle.text = additionalDataAsync.element.name
                            ivBackground.srcBlurConsumer(R.drawable.ic_logo).accept(additionalDataAsync.element.thumbnailPoster)

                            additionalDataAsync.element.airing?.let { airingDate ->
                                tvAiring.text = if (airingDate.isBefore(ZonedDateTime.now().toLocalDate())) {
                                    getString(R.string.release_date_past, airingDate.asFormattedString)
                                } else {
                                    getString(R.string.release_date_future, airingDate.asFormattedString)
                                }
                            }

                            val summary = additionalDataAsync.element.summary
                            tvSummary.isVisible = summary != null
                            if (summary != null) tvSummary.text = summary

                            val actors = additionalDataAsync.element.actors
                            tvActors.isVisible = actors.isNotEmpty()
                            tvActors.text = getString(R.string.detail_tv_actors, actors.joinToString(", "))

                            optionsAdapter.update()
                        }
                        is Async.Error -> toast(R.string.detail_error_additional_data)
                    }
                }
                .addTo(disposables)

        val snapToFirstItemObservable = rvMedia.globalLayouts()
                .map { rvMedia.calculateDistanceToPosition(snapHelper, 0) }
                .doOnNext { rvMedia.scrollBy(it.first, it.second) }
                .debounce(200, TimeUnit.MILLISECONDS)
                .takeUntil { it.first == 0 && it.second == 0 }

        reactor.state.changesFrom { it.additionalData }
                .ofType<Async.Success<DetailReactor.State.AdditionalData>>()
                .map { listOf(DetailMediaItem.Poster(it.element.thumbnailPoster, it.element.originalPoster)) + it.element.videos }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(detailMediaAdapter::submitList)
                .switchMap { snapToFirstItemObservable }
                .bind()
                .addTo(disposables)
    }

    private fun OptionsAdapter.update() {
        listOfNotNull(
                if (reactor.currentState.watchable != null) null else addOption,
                reactor.currentState.additionalData()?.website?.let(::createOpenWebOption),
                reactor.currentState.additionalData()?.imdbId?.let(::createOopenImdbOption),
                reactor.currentState.watchable?.let(::createShareOption),
                if (reactor.currentState.watchable != null) deleteOption else null
        ).also(::submitList)
    }

    private fun createOpenWebOption(webSite: String): Option.Action =
            Option.Action(R.string.menu_watchable_website, R.drawable.ic_open_in_browser) { openChromeTab(webSite) }

    private fun createOopenImdbOption(imdbId: String): Option.Action =
            Option.Action(R.string.menu_watchable_imdb, R.drawable.ic_imdb) {
                openChromeTab(getString(R.string.imdb_search, imdbId))
            }

    private fun createShareOption(watchable: Watchable): Option.Action =
            Option.Action(R.string.menu_watchable_share, R.drawable.ic_share) {
                shareService.share(watchable).subscribe().addTo(disposables)
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

    private val addOption: Option.Action
        get() = Option.Action(R.string.menu_watchable_add, null) {
            reactor.action.accept(DetailReactor.Action.AddWatchable)
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
        private val type: Watchable.Type,
        private val movieDatabaseApi: MovieDatabaseApi,
        private val watchablesDataSource: WatchablesDataSource,
        private val analyticsService: AnalyticsService
) : BaseReactor<DetailReactor.Action, DetailReactor.Mutation, DetailReactor.State>(
        initialState = State(),
        initialAction = Action.InitialLoad
) {

    sealed class Action {
        object InitialLoad : Action()
        object LoadDetailData : Action()
        object DeleteWatchable : Action()
        data class SetWatched(val watched: Boolean) : Action()
        object Dismiss : Action()
        object AddWatchable : Action()
    }

    sealed class Mutation {
        data class DeleteWatchableResult(val deleteResult: Async<Unit>) : Mutation()
        data class SetWatchable(val watchable: Watchable?) : Mutation()
        data class SetAdditionalData(val additionalData: Async<State.AdditionalData>) : Mutation()
    }

    data class State(
            val watchable: Watchable? = null,
            val additionalData: Async<AdditionalData> = Async.Uninitialized,
            val deleteResult: Async<Unit> = Async.Uninitialized
    ) {
        data class AdditionalData(
                val name: String? = null,
                val thumbnailPoster: String? = null,
                val originalPoster: String? = null,
                val website: String? = null,
                val imdbId: String? = null,
                val videos: List<DetailMediaItem.YoutubeVideo> = emptyList(),
                val summary: String? = null,
                val airing: LocalDate? = null,
                val actors: List<String> = emptyList()
        )
    }

    override fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> {
        val watchableMutation = watchablesDataSource.watchableObservable(itemId)
                .toObservable()
                .map { Mutation.SetWatchable(it) }
                .doOnError(Timber::e)
                .onErrorReturn { Mutation.SetWatchable(null) }
                .switchMap { Observable.concat(it.observable, mutate(Action.LoadDetailData)) }
        return Observable.merge(mutation, watchableMutation)
    }

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.InitialLoad -> {
            watchablesDataSource.watchable(itemId)
                    .ignoreElement()
                    .toObservable<Mutation>()
                    .onErrorResumeNext { t: Throwable ->
                        if (t is NoSuchElementException) mutate(Action.LoadDetailData)
                        else Observable.empty()
                    }
        }
        is Action.LoadDetailData -> {
            val loading = Mutation.SetAdditionalData(Async.Loading).observable

            val detailInfoLoad = when (type) {
                Watchable.Type.movie -> loadAdditionalInfoFromMovie()
                else -> loadAdditionalInfoFromShow()
            }
            val load = detailInfoLoad
                    .map { Mutation.SetAdditionalData(Async.Success(it)) }
                    .doOnError(Timber::e)
                    .onErrorReturn { Mutation.SetAdditionalData(Async.Error(it)) }
                    .toObservable()

            Observable.concat(loading, load)
        }
        is Action.DeleteWatchable -> {
            watchablesDataSource.setWatchableDeleted(itemId)
                    .doOnComplete { currentState.watchable?.let(analyticsService::logWatchableDelete) }
                    .doOnComplete { DeleteWatchablesWorker.startSingle() }
                    .toObservableDefault(Mutation.DeleteWatchableResult(Async.Success(Unit)))
                    .onErrorReturn { Mutation.DeleteWatchableResult(Async.Error(it)) }
                    .doOnComplete { Router follow DetailRoute.Pop }
        }
        is Action.SetWatched -> {
            watchablesDataSource.updateWatchable(itemId, action.watched).toObservable()
        }
        is Action.Dismiss -> {
            emptyMutation { Router follow DetailRoute.Pop }
        }
        is Action.AddWatchable -> {
            emptyMutation { AddWatchableWorker.start(itemId.toInt(), type.convertToSearchType()) }
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.DeleteWatchableResult -> previousState.copy(deleteResult = mutation.deleteResult)
        is Mutation.SetWatchable -> previousState.copy(watchable = mutation.watchable)
        is Mutation.SetAdditionalData -> previousState.copy(additionalData = mutation.additionalData)
    }

    private fun loadAdditionalInfoFromMovie(): Single<State.AdditionalData> {
        return movieDatabaseApi.movie(itemId.toInt())
                .map {
                    State.AdditionalData(
                            it.name,
                            Images.thumbnail.from(it.image),
                            Images.original.from(it.image),
                            it.website,
                            it.imdbId,
                            it.videos.results.mapToYoutubeVideos(),
                            it.summary,
                            it.releaseDate,
                            it.credits?.mapToActorList() ?: emptyList()
                    )
                }
    }

    private fun loadAdditionalInfoFromShow(): Single<State.AdditionalData> {
        return movieDatabaseApi.show(itemId.toInt())
                .map {
                    State.AdditionalData(
                            it.name,
                            Images.thumbnail.from(it.image),
                            Images.original.from(it.image),
                            it.website,
                            it.externalIds.imdbId,
                            it.videos.results.mapToYoutubeVideos(),
                            it.summary,
                            it.nextEpisode?.airingDate,
                            it.credits?.mapToActorList() ?: emptyList()
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

    private fun Credits.mapToActorList(): List<String> {
        return cast.sortedWith(compareBy(Credits.Cast::order))
                .map(Credits.Cast::name)
                .take(5)
    }
}