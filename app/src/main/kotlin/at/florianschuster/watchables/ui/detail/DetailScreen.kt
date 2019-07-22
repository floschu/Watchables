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
import androidx.recyclerview.widget.SnapHelper
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
import at.florianschuster.watchables.ui.base.BaseCoordinator
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.globalLayouts
import com.tailoredapps.androidutil.async.Async
import com.tailoredapps.androidutil.ui.extensions.RxDialogAction
import com.tailoredapps.androidutil.ui.extensions.rxDialog
import com.tailoredapps.androidutil.ui.extensions.toObservableDefault
import com.tailoredapps.androidutil.optional.ofType
import com.tailoredapps.androidutil.ui.extensions.observable
import com.tailoredapps.androidutil.ui.extensions.toast
import at.florianschuster.reaktor.android.koin.reactor
import at.florianschuster.watchables.service.DeepLinkService
import at.florianschuster.watchables.model.convertToSearchType
import com.tailoredapps.androidutil.async.mapToAsync
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_detail.*
import kotlinx.android.synthetic.main.fragment_detail_rating.*
import kotlinx.android.synthetic.main.fragment_detail_toolbar.*
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
    private val detailHeaderAdapter: DetailHeaderAdapter by inject()
    private val optionsAdapter: OptionsAdapter by inject()
    private val shareService: ShareService by inject { parametersOf(activity) }

    private val snapHelper = LinearSnapHelper()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coordinator.provideNavigationHandler(navController)

        btnBack.clicks()
            .map { DetailRoute.Pop }
            .bind(to = Router::follow)
            .addTo(disposables)

        with(rvDetailHeader) {
            snapHelper.attachToRecyclerView(this)
            adapter = detailHeaderAdapter

            var previousSnappedPosition = RecyclerView.NO_POSITION
            onSnappedPositionChanged(snapHelper) { position ->
                (rvDetailHeader.findViewHolderForLayoutPosition(previousSnappedPosition) as? DetailHeaderViewHolder)
                    ?.apply(DetailHeaderViewHolder.FocusState.None)
                (rvDetailHeader.findViewHolderForLayoutPosition(position) as? DetailHeaderViewHolder)
                    ?.apply(DetailHeaderViewHolder.FocusState.Highlighted)
                previousSnappedPosition = position
            }
        }

        rvDetailOptions.adapter = optionsAdapter
        optionsAdapter.update()

        bind(reactor)
    }

    override fun bind(reactor: DetailReactor) {
        reactor.state.changesFrom { it.deleteResult }
            .bind { deleteAsync ->
                loading.isVisible = deleteAsync.loading
                if (deleteAsync is Async.Error) {
                    toast(deleteAsync.error.asCauseTranslation(resources))
                }
            }
            .addTo(disposables)

        reactor.state.changesFrom { it.additionalData }
            .bind { additionalDataAsync ->
                when (additionalDataAsync) {
                    is Async.Success -> {
                        tvTitle.text = additionalDataAsync.element.name
                        ivBackground.srcBlurConsumer(R.drawable.ic_logo)
                            .accept(additionalDataAsync.element.thumbnailPoster)

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

                        val rating = additionalDataAsync.element.rating
                        includeRating.isVisible = rating != null
                        if (rating != null) {
                            tvRating.text = "${rating.rating}"
                            tvNumberOfRatings.text = getString(R.string.detail_tv_ratings, rating.count)
                        }

                        optionsAdapter.update()
                    }
                    is Async.Error -> toast(R.string.detail_error_additional_data)
                }
            }
            .addTo(disposables)

        val snapToFirstItemObservable = rvDetailHeader.globalLayouts() // todo
            .map { rvDetailHeader.calculateDistanceToPosition(snapHelper, 1) }
            .doOnNext { rvDetailHeader.scrollBy(it.first, it.second) }
            .debounce(200, TimeUnit.MILLISECONDS)
            .takeUntil { it.first == 0 && it.second == 0 }

        reactor.state.changesFrom { it.headerItems }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(detailHeaderAdapter::submitList)
            .map { items -> items.indexOfFirst { it is DetailHeaderItem.Poster } }
            .map { if (it >= 0) it else 0 }
            .switchMap { snapToFirstItemObservable }
            .bind()
            .addTo(disposables)
    }

    private fun OptionsAdapter.update() {
        listOfNotNull(
            if (reactor.currentState.watchable != null) null else addOption,
            reactor.currentState.additionalData()?.website?.let(::createOpenWebOption),
            reactor.currentState.additionalData()?.imdbId?.let(::createOpenImdbOption),
            reactor.currentState.additionalData()?.facebookId?.let(::createOpenFacebookOption),
            reactor.currentState.additionalData()?.instagramId?.let(::createOpenInstagramOption),
            reactor.currentState.additionalData()?.twitterId?.let(::createOpenTwitterOption),
            reactor.currentState.watchable?.let(::createShareOption),
            if (reactor.currentState.watchable != null) deleteOption else null
        ).also(::submitList)
    }

    private val addOption: Option.Action
        get() = Option.Action(R.string.menu_watchable_add, null) {
            reactor.action.accept(DetailReactor.Action.AddWatchable)
        }

    private fun createOpenWebOption(webSite: String): Option.Action =
        Option.Action(R.string.menu_watchable_website, R.drawable.ic_open_in_browser) { openChromeTab(webSite) }

    private fun createOpenImdbOption(imdbId: String): Option.Action =
        Option.Action(R.string.menu_watchable_imdb, R.drawable.ic_imdb) {
            openChromeTab(getString(R.string.imdb_id_url, imdbId))
        }

    private fun createOpenFacebookOption(facebookId: String): Option.Action =
        Option.Action(R.string.menu_watchable_facebook, R.drawable.ic_facebook) {
            openChromeTab(getString(R.string.facebook_id_url, facebookId))
        }

    private fun createOpenInstagramOption(instagramId: String): Option.Action =
        Option.Action(R.string.menu_watchable_instagram, R.drawable.ic_instagram) {
            openChromeTab(getString(R.string.instagram_id_url, instagramId))
        }

    private fun createOpenTwitterOption(twitterId: String): Option.Action =
        Option.Action(R.string.menu_watchable_twitter, R.drawable.ic_twitter) {
            openChromeTab(getString(R.string.twitter_id_url, twitterId))
        }

    private fun createShareOption(watchable: Watchable): Option.Action =
        Option.Action(R.string.menu_watchable_share, R.drawable.ic_share) {
            shareService.share(watchable)
                .onErrorResumeNext {
                    Timber.e(it)
                    toast(it.asCauseTranslation(resources))
                    Completable.never()
                }
                .subscribe().addTo(disposables)
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

    private fun RecyclerView.onSnappedPositionChanged(
        snapHelper: SnapHelper,
        onSnap: (position: Int) -> Unit
    ): RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        private var oldSnapPosition = RecyclerView.NO_POSITION

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val snapPosition = snapHelper.getSnapPosition(recyclerView)
            if (oldSnapPosition != snapPosition) {
                onSnap(snapPosition)
                oldSnapPosition = snapPosition
            }
        }

        fun SnapHelper.getSnapPosition(recyclerView: RecyclerView): Int {
            val layoutManager = recyclerView.layoutManager
                ?: return RecyclerView.NO_POSITION
            val snapView = findSnapView(layoutManager)
                ?: return RecyclerView.NO_POSITION
            return layoutManager.getPosition(snapView)
        }
    }.also(::addOnScrollListener)
}

class DetailReactor(
    private val itemId: String,
    private val type: Watchable.Type,
    private val movieDatabaseApi: MovieDatabaseApi,
    private val watchablesDataSource: WatchablesDataSource,
    private val analyticsService: AnalyticsService,
    private val deepLinkService: DeepLinkService
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
        data class SetVideoHeaderItems(val videos: List<DetailHeaderItem.YoutubeVideo>) : Mutation()
        data class SetPosterHeadItem(val poster: DetailHeaderItem.Poster) : Mutation()
    }

    data class State(
        val watchable: Watchable? = null,
        val headerItems: List<DetailHeaderItem> = emptyList(),
        val additionalData: Async<AdditionalData> = Async.Uninitialized,
        val deleteResult: Async<Unit> = Async.Uninitialized
    ) {
        data class AdditionalData(
            val name: String? = null,
            val thumbnailPoster: String? = null,
            val originalPoster: String? = null,
            val videos: List<Videos.Video> = emptyList(),
            val website: String? = null,
            val imdbId: String? = null,
            val facebookId: String? = null,
            val instagramId: String? = null,
            val twitterId: String? = null,
            val summary: String? = null,
            val airing: LocalDate? = null,
            val actors: List<String> = emptyList(),
            val rating: Rating? = null
        ) {
            data class Rating(val rating: Double, val count: Int)
        }
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
            watchablesDataSource.watchable(itemId) // todo why do i need this?
                .ignoreElement()
                .toObservable<Mutation>()
                .onErrorResumeNext { t: Throwable ->
                    if (t is NoSuchElementException) mutate(Action.LoadDetailData)
                    else Observable.empty()
                }
        }
        is Action.LoadDetailData -> {
            val loading = Mutation.SetAdditionalData(Async.Loading).observable

            val additionalDataLoad = when (type) {
                Watchable.Type.movie -> loadAdditionalInfoFromMovie()
                else -> loadAdditionalInfoFromShow()
            }

            val load = additionalDataLoad
                .doOnError(Timber::e)
                .mapToAsync()
                .toObservable()
                .flatMap {
                    val posterMutation = if (it is Async.Success) {
                        Mutation.SetPosterHeadItem(DetailHeaderItem.Poster(
                            it.element.thumbnailPoster,
                            it.element.originalPoster
                        )).observable
                    } else Observable.empty()

                    val videosMutation = if (it is Async.Success) {
                        Mutation.SetVideoHeaderItems(it.element.videos.mapToYoutubeVideos()).observable
                    } else Observable.empty()
                    Observable.concat(Mutation.SetAdditionalData(it).observable, posterMutation, videosMutation)
                }

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
            emptyMutation {
                AddWatchableWorker.start(
                    itemId.toInt(),
                    type.convertToSearchType(),
                    currentState.additionalData()?.name
                )
            }
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.DeleteWatchableResult -> previousState.copy(deleteResult = mutation.deleteResult)
        is Mutation.SetWatchable -> {
            if (mutation.watchable != null) {
                val newHeaderItems = previousState.headerItems
                    .filter { it !is DetailHeaderItem.QRCode }
                    .plus(DetailHeaderItem.QRCode(deepLinkService.createDeepLink(mutation.watchable)))
                    .sorted()
                previousState.copy(watchable = mutation.watchable, headerItems = newHeaderItems)
            } else {
                previousState.copy(watchable = mutation.watchable)
            }
        }
        is Mutation.SetAdditionalData -> previousState.copy(additionalData = mutation.additionalData)
        is Mutation.SetVideoHeaderItems -> {
            val newHeaderItems = previousState.headerItems
                .filter { it !is DetailHeaderItem.YoutubeVideo }
                .plus(mutation.videos)
                .sorted()
            previousState.copy(headerItems = newHeaderItems)
        }
        is Mutation.SetPosterHeadItem -> {
            val newHeaderItems = previousState.headerItems
                .filter { it !is DetailHeaderItem.Poster }
                .plus(mutation.poster)
                .sorted()
            previousState.copy(headerItems = newHeaderItems)
        }
    }

    private fun loadAdditionalInfoFromMovie(): Single<State.AdditionalData> {
        return movieDatabaseApi.movie(itemId.toInt())
            .map {
                State.AdditionalData(
                    it.name,
                    Images.thumbnail.from(it.image),
                    Images.original.from(it.image),
                    it.videos.results,
                    it.website,
                    it.externalIds.imdbId,
                    it.externalIds.facebookId,
                    it.externalIds.instagramId,
                    it.externalIds.twitterId,
                    it.summary,
                    it.releaseDate,
                    it.credits?.mapToActorList() ?: emptyList(),
                    if (it.rating != null && it.rating > 0 &&
                        it.numberOfRatings != null && it.numberOfRatings > 0) {
                        State.AdditionalData.Rating(it.rating, it.numberOfRatings)
                    } else null
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
                    it.videos.results,
                    it.website,
                    it.externalIds.imdbId,
                    it.externalIds.facebookId,
                    it.externalIds.instagramId,
                    it.externalIds.twitterId,
                    it.summary,
                    it.nextEpisode?.airingDate,
                    it.credits?.mapToActorList() ?: emptyList(),
                    if (it.rating != null && it.rating > 0 &&
                        it.numberOfRatings != null && it.numberOfRatings > 0) {
                        State.AdditionalData.Rating(it.rating, it.numberOfRatings)
                    } else null
                )
            }
    }

    private fun List<Videos.Video>.mapToYoutubeVideos(): List<DetailHeaderItem.YoutubeVideo> {
        return asSequence()
            .filter { it.isYoutube }
            .sortedBy { it.type }
            .map { DetailHeaderItem.YoutubeVideo(it.id, it.name, it.key, it.type) }
            .toList()
    }

    private fun Credits.mapToActorList(): List<String> {
        return cast.sortedWith(compareBy(Credits.Cast::order))
            .map(Credits.Cast::name)
            .take(5)
    }
}