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

package at.florianschuster.watchables.ui.base.reactor

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.PublishRelay
import com.squareup.leakcanary.RefWatcher
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.koin.core.KoinComponent
import org.koin.core.inject


/**
 * BaseReactor class that handles action and state creation and clearing of state observable.
 */
abstract class BaseReactor<Action : Any, Mutation : Any, State : Any>(
        final override val initialState: State
) : ViewModel(), KoinComponent, Reactor<Action, Mutation, State> {
    private val refWatcher: RefWatcher by inject()

    override var disposable = CompositeDisposable()

    override val action: PublishRelay<Action> by lazy { PublishRelay.create<Action>() }
    override val state: Observable<out State> by lazy { createStateStream() }

    override var currentState: State = initialState

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        disposable.clear()
        refWatcher.watch(this)
    }
}