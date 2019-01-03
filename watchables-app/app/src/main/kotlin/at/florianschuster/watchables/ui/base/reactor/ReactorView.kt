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

import io.reactivex.disposables.CompositeDisposable


/**
 * A ReactorView displays data. The view binds user inputs to the action stream and binds the view
 * states to each UI component. There's no business logic in a view layer. A view just defines how
 * to map the action stream and the state stream.
 *
 * Reference: https://github.com/ReactorKit/ReactorKit
 */
interface ReactorView<R : Reactor<*, *, *>> {
    val disposable: CompositeDisposable
    val reactor: R
    fun bind(reactor: R)
}
