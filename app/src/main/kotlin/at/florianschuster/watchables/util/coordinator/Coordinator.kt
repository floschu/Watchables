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

import androidx.fragment.app.Fragment
import android.app.Activity
import android.view.ViewGroup


/**
 * An object defining the routes that are possible results within or through a Coordinator.
 * This could be an enum or a sealed class.
 *
 * It is not up to the Route to decide where or how to navigate! Do not couple view flows to a
 * route, rather try to define a coordinated action as route.
 * e.g. "ShowIsPicked(show: Show)" instead of "OpenShowDetailViewWith(show: Show)"
 */
interface CoordinatorRoute


/**
 * A Coordinator handles navigation or view flow for one or more view controller (e.g. [Fragment],
 * [Activity], [ViewGroup]). Its purpose is to isolate navigation logic.
 */
interface Coordinator<Route> where Route : CoordinatorRoute {

    /**
     * Method that handles the navigation that is defined through a [Route].
     */
    fun navigate(to: Route)

}


