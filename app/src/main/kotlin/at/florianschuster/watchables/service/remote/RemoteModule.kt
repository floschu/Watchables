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

import com.google.gson.Gson
import at.florianschuster.watchables.BuildConfig
import at.florianschuster.watchables.model.Search
import at.florianschuster.watchables.all.util.gson.LocalDateTypeAdapter
import at.florianschuster.watchables.all.util.gson.SearchItemTypeAdapter
import com.google.gson.GsonBuilder
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import org.threeten.bp.LocalDate
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

val remoteModule = module {
    single { provideGson() }
    single { provideOkHttpClient() }
    single { provideMovieDatabaseApi(get(), get(), BuildConfig.MOVIEDB_BASE_URL) }
    single { FirebaseWatchablesApi(get()) as WatchablesApi }
}

private fun provideGson(): Gson = GsonBuilder().apply {
    registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
    registerTypeAdapter(Search.SearchItem::class.java, SearchItemTypeAdapter())
}.create()

private fun provideOkHttpClient() =
        OkHttpClient().newBuilder().apply {
            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC
                addInterceptor(loggingInterceptor)
            }
        }.build()

private fun provideMovieDatabaseApi(okHttpClient: OkHttpClient, gson: Gson, apiUrl: String): MovieDatabaseApi {
    val httpClientBuilder = okHttpClient.newBuilder()

    httpClientBuilder.addInterceptor {
        val originalRequest = it.request()
        val request = originalRequest.newBuilder().apply {
            val url = originalRequest.url().newBuilder().apply {
                addQueryParameter("api_key", BuildConfig.MOVIEDB_KEY)
            }.build()
            url(url)
        }.build()
        return@addInterceptor it.proceed(request)
    }

    return Retrofit.Builder().apply {
        baseUrl(apiUrl)
        client(httpClientBuilder.build())
        addConverterFactory(GsonConverterFactory.create(gson))
        addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
    }.build().create(MovieDatabaseApi::class.java)
}
