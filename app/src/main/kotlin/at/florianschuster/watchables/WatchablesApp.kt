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

package at.florianschuster.watchables

import android.app.Application
import at.florianschuster.reaktor.Reaktor
import at.florianschuster.watchables.service.local.localModule
import at.florianschuster.watchables.service.remote.remoteModule
import at.florianschuster.watchables.ui.detail.detailModule
import at.florianschuster.watchables.ui.login.loginModule
import at.florianschuster.watchables.ui.more.moreModule
import at.florianschuster.watchables.ui.search.searchModule
import at.florianschuster.watchables.ui.watchables.watchablesModule
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import at.florianschuster.watchables.all.util.CrashlyticsTree
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.plugins.RxJavaPlugins
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class WatchablesApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) return

        instance = this

        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashlyticsTree())
        RxJavaPlugins.setErrorHandler(Timber::e)
        AndroidThreeTen.init(this)
        FirebaseAnalytics.getInstance(this) // initialize for deep link tracking
        Reaktor.handleErrorsWith(handler = Timber::e)

        startKoin {
            androidContext(this@WatchablesApp)
            androidLogger(Level.INFO)
            modules(
                    appModule, localModule, remoteModule,
                    loginModule, watchablesModule, searchModule, detailModule, moreModule
            )
        }
    }

    companion object {
        lateinit var instance: WatchablesApp
            private set
    }
}
