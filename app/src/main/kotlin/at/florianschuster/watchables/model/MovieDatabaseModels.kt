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

package at.florianschuster.watchables.model

import at.florianschuster.watchables.service.remote.LocalDateSerializer
import at.florianschuster.watchables.service.remote.SearchItemSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.threeten.bp.LocalDate

@Serializable
data class Search(val results: List<@Serializable(with = SearchItemSerializer::class) SearchItem?> = emptyList()) {

    //todo polymorphic

    @Serializable
    data class SearchItem(
        val id: Int,
        val title: String,
        val type: Type,
        val posterPath: String?,
        val added: Boolean
    ) {
        enum class Type { movie, tv }
    }
}

@Serializable
data class Movie(
    val id: Int,
    @SerialName("original_title") val name: String,
    @SerialName("imdb_id") val imdbId: String?,
    @SerialName("poster_path") val image: String?,
    @SerialName("homepage") val website: String?,
    @SerialName("release_date") @Serializable(with = LocalDateSerializer::class) val releaseDate: LocalDate?,
    @SerialName("runtime") val durationInMinutes: Long?,
    @SerialName("original_language") val language: String,
    @SerialName("overview") val summary: String?,
    val status: Status,
    val videos: Videos,
    val credits: Credits?
) {
    enum class Status {
        @SerialName("Rumored")
        rumored,
        @SerialName("Planned")
        planned,
        @SerialName("In Production")
        inProduction,
        @SerialName("Post Production")
        postProduction,
        @SerialName("Released")
        released,
        @SerialName("Canceled")
        canceled
    }
}

@Serializable
data class Show(
    val id: Int,
    @SerialName("original_name") val name: String,
    @SerialName("poster_path") val image: String?,
    @SerialName("homepage") val website: String,
    @SerialName("number_of_seasons") val seasons: Int,
    @SerialName("episode_run_time") val episodeRuntimes: List<Int>,
    @SerialName("external_ids") val externalIds: ExternalIds,
    @SerialName("first_air_date") @Serializable(with = LocalDateSerializer::class) val releaseDate: LocalDate?,
    @SerialName("overview") val summary: String?,
    val status: Status,
    val videos: Videos,
    @SerialName("next_episode_to_air") val nextEpisode: Season.Episode?,
    @SerialName("last_episode_to_air") val lastEpisode: Season.Episode?,
    val credits: Credits?
) {
    enum class Status {
        @SerialName("Returning Series")
        returningSeries,
        @SerialName("Planned")
        planned,
        @SerialName("In Production")
        inProduction,
        @SerialName("Ended")
        ended,
        @SerialName("Canceled")
        canceled,
        @SerialName("Pilot")
        pilot
    }

    @Serializable
    data class ExternalIds(@SerialName("imdb_id") val imdbId: String?)
}

@Serializable
data class Season(
    val id: Int,
    @SerialName("season_number") val index: Int,
    @SerialName("poster_path") val image: String?,
    @SerialName("air_date") @Serializable(with = LocalDateSerializer::class) val airingDate: LocalDate?,
    @SerialName("episodes") val episodes: List<Episode>
) {
    @Serializable
    data class Episode(
        val id: Int,
        val name: String,
        @SerialName("season_number") val seasonIndex: Int,
        @SerialName("episode_number") val episodeIndex: Int,
        @SerialName("air_date") @Serializable(with = LocalDateSerializer::class) val airingDate: LocalDate?
    )
}

@Serializable
data class Videos(val results: List<Video>) {
    @Serializable
    data class Video(
        val id: String,
        val name: String,
        val key: String,
        val type: Type?,
        val site: String
    ) {
        enum class Type {
            @SerialName("Trailer")
            trailer,
            @SerialName("Teaser")
            teaser,
            @SerialName("Clip")
            clip,
            @SerialName("Featurette")
            featurette,
            @SerialName("Opening Credits")
            openingCredits
        }

        val isYoutube: Boolean
            get() = site == "YouTube"
    }
}

@Serializable
data class Credits(val id: Int, val cast: List<Cast>) {
    @Serializable
    data class Cast(
        @SerialName("cast_id")
        val id: Int,
        val name: String,
        val order: Int
    )
}