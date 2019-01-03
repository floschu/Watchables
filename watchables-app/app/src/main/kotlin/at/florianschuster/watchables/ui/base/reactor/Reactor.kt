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

import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber


/**
 * A Reactor is an UI-independent layer which manages the state of a view. The foremost role of a
 * reactor is to separate control flow from a view. Every view has its corresponding reactor and
 * delegates all logic to its reactor. A reactor has no dependency to a view, so it can be easily
 * tested.
 *
 * Reference: https://github.com/ReactorKit/ReactorKit
 */
interface Reactor<Action : Any, Mutation : Any, State : Any> {
    var disposable: CompositeDisposable

    /**
     * The action from the view. Bind user inputs to this subject.
     */
    val action: PublishRelay<Action>

    /**
     * The state stream. Use this observable to observe the state changes.
     */
    val state: Observable<out State>

    /**
     * The initial state.
     */
    val initialState: State

    /**
     * The current state. This value is changed just after the state stream emits a new state.
     */
    var currentState: State

    /**
     * Commits mutation from the action. This is the best place to perform side-effects such as
     * async tasks.
     *
     * @param action Action
     * @return Observable<Mutation>
     */
    fun mutate(action: Action): Observable<out Mutation> = Observable.empty()

    /**
     * Generates a new state with the previous state and the action. It should be purely functional
     * so it should not perform any side-effects here. This method is called every time when
     * the mutation is committed.
     *
     * @param state State
     * @param mutation Mutation
     * @return State
     */
    fun reduce(state: State, mutation: Mutation): State = state

    /**
     * Transforms the action. Use this function to combine with other observables. This method is
     * called once before the state stream is created.
     *
     * @param action Observable<Action>
     * @return Observable<Action>
     */
    fun transformAction(action: Observable<Action>): Observable<out Action> = action

    /**
     * Transforms the mutation stream. Implement this method to transform or combine with other
     * observables. This method is called once before the state stream is created.
     *
     * @param mutation Observable<Mutation>
     * @return Observable<Mutation>
     */
    fun transformMutation(mutation: Observable<Mutation>): Observable<out Mutation> = mutation

    /**
     * Transforms the state stream. Use this function to perform side-effects such as logging. This
     * method is called once after the state stream is created.
     *
     * @param state Observable<State>
     * @return Observable<State>
     */
    fun transformState(state: Observable<State>): Observable<State> = state

    /**
     * Creates the State stream by transforming the action relay to a state observable.
     *
     * @return Observable<State>
     */
    fun createStateStream(): Observable<State> {
        val transformedAction: Observable<out Action> = transformAction(action)

        val mutation: Observable<Mutation> = transformedAction.flatMap {
            mutate(it).doOnError(Timber::e).onErrorResumeNext { _: Throwable -> Observable.empty() }
        }

        val transformedMutation: Observable<out Mutation> = transformMutation(mutation)

        val state: Observable<State> = transformedMutation
                .scan(initialState) { state, mutate -> reduce(state, mutate) }
                .doOnError(Timber::e)
                .onErrorResumeNext { _: Throwable -> Observable.empty() }
                .startWith(initialState)
                .observeOn(AndroidSchedulers.mainThread())

        val transformedState = transformState(state)
                .doOnNext { currentState = it }
                .replay(1)
        disposable.add(transformedState.connect())
        return transformedState
    }
}
