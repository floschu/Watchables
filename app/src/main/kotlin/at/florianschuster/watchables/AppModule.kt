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

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import at.florianschuster.watchables.all.AndroidAppLocaleProvider
import at.florianschuster.watchables.all.AppLocaleProvider
import at.florianschuster.watchables.all.OptionsAdapter
import at.florianschuster.watchables.all.util.QrCodeService
import at.florianschuster.watchables.all.util.ZXingQrCodeService
import at.florianschuster.watchables.service.ActivityShareService
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.AndroidNotificationService
import at.florianschuster.watchables.service.AppUpdateService
import at.florianschuster.watchables.service.DeepLinkService
import at.florianschuster.watchables.service.FirebaseAnalyticsService
import at.florianschuster.watchables.service.FirebaseDeepLinkService
import at.florianschuster.watchables.service.FirebaseSessionService
import at.florianschuster.watchables.service.FirebaseWatchablesDataSource
import at.florianschuster.watchables.service.NotificationService
import at.florianschuster.watchables.service.PlayAppUpdateService
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.service.ShareService
import at.florianschuster.watchables.service.TMDBWatchablesUpdateService
import at.florianschuster.watchables.service.WatchablesDataSource
import at.florianschuster.watchables.service.WatchablesUpdateService
import at.florianschuster.watchables.service.local.HawkPersistenceProvider
import at.florianschuster.watchables.service.local.PersistenceProvider
import at.florianschuster.watchables.ui.watchables.filter.RxWatchablesFilterService
import at.florianschuster.watchables.ui.watchables.filter.WatchablesFilterService
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.squareup.leakcanary.LeakCanary
import com.tbruyelle.rxpermissions2.RxPermissions
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { LeakCanary.install(androidApplication()) }

    single<AppLocaleProvider> { AndroidAppLocaleProvider() }
    single<PersistenceProvider> { HawkPersistenceProvider(androidContext()) }

    single<SessionService<FirebaseUser, AuthCredential>> { FirebaseSessionService(androidContext()) }
    single<AnalyticsService> { FirebaseAnalyticsService(androidContext(), get()) }
    single<NotificationService> { AndroidNotificationService(androidContext()) }
    single<WatchablesDataSource> { FirebaseWatchablesDataSource(get()) }
    single<WatchablesFilterService> { RxWatchablesFilterService(get()) }
    single<DeepLinkService> { FirebaseDeepLinkService() }
    single<QrCodeService> { ZXingQrCodeService(androidContext()) }
    single<WatchablesUpdateService> { TMDBWatchablesUpdateService(get(), get()) }
    single<AppUpdateService> { PlayAppUpdateService(androidContext()) }

    factory<ShareService> { (activity: AppCompatActivity) -> ActivityShareService(activity, get()) }
    factory { OptionsAdapter() }
    factory { (fragment: Fragment) -> RxPermissions(fragment) }
}
