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

import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import at.florianschuster.watchables.service.DeepLinkService
import at.florianschuster.watchables.service.FirebaseDeepLinkService
import at.florianschuster.watchables.all.OptionsAdapter
import at.florianschuster.watchables.all.util.QrCodeService
import at.florianschuster.watchables.all.util.ZXingQrCodeService
import at.florianschuster.watchables.service.ActivityShareService
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.AndroidNotificationService
import at.florianschuster.watchables.service.FirebaseAnalyticsService
import at.florianschuster.watchables.service.FirebaseSessionService
import at.florianschuster.watchables.service.FirebaseWatchablesDataSource
import at.florianschuster.watchables.service.NotificationService
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.service.ShareService
import at.florianschuster.watchables.service.WatchablesDataSource
import at.florianschuster.watchables.ui.watchables.filter.RxWatchablesFilterService
import at.florianschuster.watchables.ui.watchables.filter.WatchablesFilterService
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.squareup.leakcanary.LeakCanary
import com.tbruyelle.rxpermissions2.RxPermissions
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.Locale

val appModule = module {
    single { provideAppLocale(androidContext().resources) }
    single { LeakCanary.install(androidApplication()) }
    single<SessionService<FirebaseUser, AuthCredential>> { FirebaseSessionService(androidContext()) }
    single<AnalyticsService> { FirebaseAnalyticsService(androidContext(), get()) }
    single<NotificationService> { AndroidNotificationService(androidContext()) }
    single<WatchablesDataSource> { FirebaseWatchablesDataSource(get()) }
    single<WatchablesFilterService> { RxWatchablesFilterService(get()) }
    single<DeepLinkService> { FirebaseDeepLinkService(androidContext().resources) }
    single<QrCodeService> { ZXingQrCodeService(androidContext()) }

    factory<ShareService> { (activity: AppCompatActivity) -> ActivityShareService(activity) }
    factory { OptionsAdapter() }
    factory { (fragment: Fragment) -> RxPermissions(fragment) }
}

private fun provideAppLocale(resources: Resources): Locale {
    val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        resources.configuration.locale
    }

    return when (currentLocale.language) {
        Locale.GERMAN.language -> currentLocale
        else -> Locale.US
    }
}