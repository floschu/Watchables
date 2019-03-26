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

import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

/**
 * An object defining the routes that are possible results within or through a Coordinator.
 *
 * It is not up to the [AppRoute] to decide where or how to navigate! Do not couple view flows to a
 * route, rather try to define a coordinated action as route.
 * e.g. "ShowIsPicked(show: Show)" instead of "OpenShowDetailViewWith(show: Show)"
 *
 * Each screen should have one [AppRoute] object (This could be an enum or a sealed class).
 */
interface AppRoute

/**
 * A [Router] can be injected into a View independent class and used to follow a specific [AppRoute].
 * A flow specific [Coordinator] then uses the router to determine where to navigate.
 *
 * An App should only have one [Router].
 */
class Router {
    private val routeRelay: PublishRelay<AppRoute> = PublishRelay.create<AppRoute>()

    /**
     * Use this to observe which [AppRoute] to handle.
     */
    val routes: Flowable<AppRoute> = routeRelay.toFlowable(BackpressureStrategy.LATEST)

    infix fun follow(route: AppRoute) {
        routeRelay.accept(route)
    }
}