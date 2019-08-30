package at.florianschuster.watchables.service

import at.florianschuster.watchables.model.Movie
import at.florianschuster.watchables.model.Season
import at.florianschuster.watchables.model.Show
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.convertToWatchable
import at.florianschuster.watchables.model.convertToWatchableSeason
import at.florianschuster.watchables.service.remote.MovieDatabaseApi
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

typealias UpdatedMovies = List<Pair<Watchable, Movie>>
typealias UpdatedShows = List<Triple<Watchable, Show, Season>> // newest season
typealias UpdatedSeason = Pair<Season, Boolean> // true if added

interface WatchablesUpdateService {
    fun updateAll(): Completable
    fun updateMovies(): Single<UpdatedMovies>
    fun updateShows(): Single<UpdatedShows>
}

class TMDBWatchablesUpdateService(
    private val watchablesDataSource: WatchablesDataSource,
    private val movieDatabaseApi: MovieDatabaseApi
) : WatchablesUpdateService {

    override fun updateAll(): Completable {
        return Completable.mergeArrayDelayError(
            updateMovies().ignoreElement(),
            updateShows().ignoreElement()
        )
    }

    override fun updateMovies(): Single<UpdatedMovies> = watchablesDataSource.watchablesToUpdate
        .flattenAsFlowable { it }
        .filter { it.type == Watchable.Type.movie }
        .flatMapSingle(::updateMovie)
        .toList()
        .subscribeOn(Schedulers.io())

    private fun updateMovie(watchable: Watchable): Single<Pair<Watchable, Movie>> {
        return movieDatabaseApi
            .movie(watchable.id.toInt())
            .flatMap { movie ->
                Single.fromCallable { movie.convertToWatchable().apply { watched = watchable.watched } }
                    .flatMap(watchablesDataSource::createWatchable)
                    .map { it to movie }
            }
    }

    override fun updateShows(): Single<UpdatedShows> {
        return watchablesDataSource.watchablesToUpdate
            .flattenAsFlowable { it }
            .filter { it.type == Watchable.Type.show }
            .flatMapMaybe(::updateShow)
            .toList()
            .subscribeOn(Schedulers.io())
    }

    private fun updateShow(watchable: Watchable): Maybe<Triple<Watchable, Show, Season>> {
        return movieDatabaseApi
            .show(watchable.id.toInt())
            .flatMapMaybe { show ->
                val updatedWatchable = show.convertToWatchable()

                watchablesDataSource.createWatchable(updatedWatchable)
                    .ignoreElement()
                    .andThen((1..show.seasons).reversed().toFlowable())
                    .flatMapSingle { movieDatabaseApi.season(show.id, it) }
                    .flatMapSingle { createSeasonIfNotExists(updatedWatchable, it) }
                    .toList()
                    .flatMapMaybe { seasons ->
                        val season = seasons.filter { it.second }.map { it.first }.maxBy(Season::index)
                        if (season != null) Maybe.just(Triple(updatedWatchable, show, season))
                        else Maybe.empty()
                    }
            }
            .doOnError(Timber::e)
    }

    private fun createSeasonIfNotExists(watchable: Watchable, season: Season): Single<UpdatedSeason> {
        return watchablesDataSource.season("${season.id}")
            .materialize()
            .flatMap { notification ->
                val error = notification.error
                val watchableSeason = notification.value

                when {
                    notification.isOnError && error != null -> {
                        if (error is NoSuchElementException) {
                            createSeason(watchable, season).map { season to true }
                        } else {
                            Single.error(error)
                        }
                    }
                    notification.isOnNext && watchableSeason != null &&
                        watchableSeason.episodes.count() != season.episodes.count() -> {
                        createSeason(watchable, season).map { season to false }
                    }
                    else -> Single.fromCallable { season to false }
                }
            }
    }

    private fun createSeason(watchable: Watchable, season: Season) = Single
        .just(season.convertToWatchableSeason(watchable.id))
        .flatMap(watchablesDataSource::createSeason)
}
