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

package at.florianschuster.watchables.ui.more

import android.os.Bundle
import android.view.View
import at.florianschuster.reaktor.ReactorView
import at.florianschuster.watchables.R
import at.florianschuster.watchables.all.Option
import at.florianschuster.watchables.all.OptionsAdapter
import at.florianschuster.watchables.all.util.Utils
import at.florianschuster.watchables.all.util.extensions.openChromeTab
import at.florianschuster.watchables.all.worker.DeleteWatchablesWorker
import at.florianschuster.watchables.all.worker.UpdateWatchablesWorker
import at.florianschuster.watchables.service.AnalyticsService
import at.florianschuster.watchables.service.SessionService
import at.florianschuster.watchables.service.ShareService
import at.florianschuster.watchables.ui.base.BaseFragment
import at.florianschuster.watchables.ui.base.BaseReactor
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.tailoredapps.androidutil.optional.ofType
import com.tailoredapps.androidutil.ui.extensions.RxDialogAction
import com.tailoredapps.androidutil.ui.extensions.rxDialog
import com.tailoredapps.reaktor.android.koin.reactor
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_more.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class MoreFragment : BaseFragment(R.layout.fragment_more), ReactorView<MoreReactor> {
    override val reactor: MoreReactor by reactor()
    private val adapter: OptionsAdapter by inject()
    private val shareService: ShareService by inject { parametersOf(activity) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind(reactor)
    }

    override fun bind(reactor: MoreReactor) {
        rvMoreOptions.adapter = adapter
        listOf(
                rateOption,
                shareOption,
                devInfoOption,
                privacyOption,
                licensesOption,
                analyticsOption,
                logoutOption
        ).also(adapter::submitList)
    }

    private val rateOption: Option
        get() = Option.Action(R.string.settings_rate_app, R.drawable.ic_star) {
            startActivity(Utils.rateApp(requireContext()))
        }

    private val shareOption: Option
        get() = Option.Action(R.string.settings_share_app, R.drawable.ic_share) {
            shareService.shareApp().subscribe().addTo(disposables)
        }

    private val devInfoOption: Option
        get() = Option.Action(R.string.settings_developer_info, R.drawable.ic_code) {
            openChromeTab(getString(R.string.developer_url))
        }

    private val privacyOption: Option
        get() = Option.Action(R.string.settings_privacy_policy, R.drawable.ic_phonelink_lock) {
            openChromeTab(getString(R.string.privacy_policy_url))
        }

    private val licensesOption: Option
        get() = Option.Action(R.string.settings_licenses, R.drawable.ic_search) {
            Utils.showLibraries(requireContext())
        }

    private val analyticsOption: Option
        get() = Option.Toggle(R.string.settings_analytics, R.drawable.ic_show_chart, reactor.currentState.analyticsEnabled) {
            reactor.action.accept(MoreReactor.Action.SetAnalytics(it))
        }

    private val logoutOption: Option
        get() = Option.Action(R.string.settings_logout, R.drawable.ic_open_in_browser) {
            rxDialog(R.style.DialogTheme) {
                titleResource = R.string.dialog_logout_title
                messageResource = R.string.dialog_logout_message
                positiveButtonResource = R.string.dialog_ok
                negativeButtonResource = R.string.dialog_cancel
            }.ofType<RxDialogAction.Positive>()
                    .map { MoreReactor.Action.Logout }
                    .subscribe(reactor.action)
                    .addTo(disposables)
        }
}

class MoreReactor(
        private val sessionService: SessionService<FirebaseUser, AuthCredential>,
        private val analyticsService: AnalyticsService
) : BaseReactor<MoreReactor.Action, MoreReactor.Mutation, MoreReactor.State>(
        State(analyticsService.analyticsEnabled)
) {
    sealed class Action {
        object Logout : Action()
        data class SetAnalytics(val enabled: Boolean) : Action()
    }

    sealed class Mutation {
        data class SetAnalyticsEnabled(val enabled: Boolean) : Mutation()
    }

    data class State(
            val analyticsEnabled: Boolean
    )

    override fun mutate(action: Action): Observable<out Mutation> = when (action) {
        is Action.Logout -> {
            sessionService.logout()
                    .doOnComplete { UpdateWatchablesWorker.stop() }
                    .doOnComplete { DeleteWatchablesWorker.stop() }
                    .toObservable()
        }
        is Action.SetAnalytics -> {
            Single.just(action.enabled)
                    .doOnSuccess { analyticsService.analyticsEnabled = it }
                    .map { Mutation.SetAnalyticsEnabled(it) }
                    .toObservable()
        }
    }

    override fun reduce(previousState: State, mutation: Mutation): State = when (mutation) {
        is Mutation.SetAnalyticsEnabled -> previousState.copy(analyticsEnabled = mutation.enabled)
    }
}