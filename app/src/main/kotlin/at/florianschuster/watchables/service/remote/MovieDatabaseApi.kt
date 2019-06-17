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

package at.florianschuster.watchables.service.remote

import at.florianschuster.watchables.model.Movie
import at.florianschuster.watchables.model.Search
import at.florianschuster.watchables.model.Season
import at.florianschuster.watchables.model.Show
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieDatabaseApi {

    @GET("search/multi")
    fun search(@Query("query") query: String, @Query("page") page: Int = 1): Single<Search>

    @GET("trending/movie,tv/day")
    fun trending(@Query("page") page: Int = 1): Single<Search>

    @GET("movie/{movie_id}?append_to_response=external_ids,videos,credits")
    fun movie(@Path("movie_id") id: Int): Single<Movie>

    @GET("tv/{tv_id}?append_to_response=external_ids,videos")
    fun show(@Path("tv_id") id: Int): Single<Show>

    @GET("tv/{tv_id}/season/{season_number}?append_to_response=credits")
    fun season(@Path("tv_id") tvId: Int, @Path("season_number") seasonNumber: Int): Single<Season>
}