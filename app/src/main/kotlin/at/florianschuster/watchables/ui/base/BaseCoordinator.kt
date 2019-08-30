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

package at.florianschuster.watchables.ui.base

import androidx.annotation.CallSuper
import at.florianschuster.koordinator.CoordinatorRoute
import at.florianschuster.koordinator.android.LifecycleCoordinator
import com.squareup.leakcanary.RefWatcher
import org.koin.core.KoinComponent
import org.koin.core.inject

abstract class BaseCoordinator<Route, NavigationHandler> :
        LifecycleCoordinator<Route, NavigationHandler>(), KoinComponent
        where Route : CoordinatorRoute, NavigationHandler : Any {

    private val refWatcher: RefWatcher by inject()

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        refWatcher.watch(this)
    }
}
