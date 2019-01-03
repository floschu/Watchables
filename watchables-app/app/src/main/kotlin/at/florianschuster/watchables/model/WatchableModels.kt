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

package at.florianschuster.watchables.model

import at.florianschuster.watchables.util.extensions.FirestoreObject


data class WatchableUser(val watchableCounter: Long = 0) : FirestoreObject()

data class Watchable(
        var watched: Boolean = false,
        val name: String = "",
        val type: Type = Type.movie,
        val posterPath: String? = null,
        val runtimeInMinutes: Long? = null,
        val status: Status = Status.finished
) : FirestoreObject() {
    enum class Type { movie, show }
    enum class Status {
        running, finished;

        val needToUpdate: Boolean get() = this == running
    }
}

data class WatchableSeason(
        val watchableId: String = "",
        val index: Int = 0,
        val posterPath: String? = null,
        val episodes: Map<String, Boolean> = emptyMap()
) : FirestoreObject()

data class WatchableContainer(val watchable: Watchable, val seasons: List<WatchableSeason>?)