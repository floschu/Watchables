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

package at.florianschuster.watchables.util.coordinator

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent


/**
 * A [Coordinator] that handles [CoordinatorRoute]s for a [FragmentActivity]. It also observes the
 * [Lifecycle] of the [FragmentActivity] to provide a way of clearing resources.
 */
abstract class ActivityCoordinator<Route>(
        val activity: FragmentActivity
) : Coordinator<Route>, LifecycleObserver where Route : CoordinatorRoute {

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    open fun onCleared() {
        //noop
    }
}


/**
 * Lazily creates an [ActivityCoordinator] for a [FragmentActivity].
 */
fun <A, Route, C> A.activityCoordinator(
        factory: () -> C
): Lazy<C> where  Route : CoordinatorRoute, C : ActivityCoordinator<Route>, A : FragmentActivity =
        lazy {
            val coordinator = factory.invoke()
            this.lifecycle.addObserver(coordinator)
            coordinator
        }
