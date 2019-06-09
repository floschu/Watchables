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

import at.florianschuster.watchables.BuildConfig
import at.florianschuster.watchables.model.Search
import com.ashokvarma.gander.GanderInterceptor
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tailoredapps.androidutil.network.networkresponse.NetworkResponseRxJava2CallAdapterFactory
import io.reactivex.schedulers.Schedulers
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.threeten.bp.LocalDate
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

val remoteModule = module {
    single { Json.nonstrict }
    single { HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC } }
    single { GanderInterceptor(androidContext()).apply { showNotification(true) } }
    single { provideOkHttpClient(get(), get()) }
    single { provideMovieDatabaseApi(get(), get(), BuildConfig.MOVIEDB_BASE_URL) }
}

private fun provideOkHttpClient(
    loggingInterceptor: HttpLoggingInterceptor,
    ganderInterceptor: GanderInterceptor
): OkHttpClient = OkHttpClient().newBuilder().apply {
    if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor)
    addInterceptor(ganderInterceptor)
}.build()

private fun provideMovieDatabaseApi(
    okHttpClient: OkHttpClient,
    json: Json,
    apiUrl: String
): MovieDatabaseApi {
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
        addConverterFactory(json.asConverterFactory(MediaType.get("application/json")))
        addCallAdapterFactory(NetworkResponseRxJava2CallAdapterFactory.create())
        addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
    }.build().create(MovieDatabaseApi::class.java)
}
