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

import com.tailoredapps.androidutil.firebase.firestore.FirestoreObject
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId

open class WatchablesFirestoreObject(var deleted: Boolean = false) : FirestoreObject() {
    fun with(obj: WatchablesFirestoreObject): WatchablesFirestoreObject {
        this.id = obj.id
        this.deleted = obj.deleted
        return this
    }
}

data class WatchableUser(val watchableCounter: Long = 0) : WatchablesFirestoreObject()

data class Watchable(
    var watched: Boolean = false,
    val name: String = "",
    val type: Type = Type.movie,
    val posterPath: String? = null,
    val runtimeInMinutes: Long? = null,
    val status: Status = Status.finished,
    val lastUpdated: Long = WatchableTimestamp.min
) : WatchablesFirestoreObject() {
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
    val episodes: Map<String, Boolean> = emptyMap(),
    val lastUpdated: Long = WatchableTimestamp.min
) : WatchablesFirestoreObject()

object WatchableTimestamp {
    val min: Long get() = OffsetDateTime.MIN.toEpochSecond()
    val now: Long get() = OffsetDateTime.now().toEpochSecond()
    fun toOffsetDateTime(epochSeconds: Long): OffsetDateTime = OffsetDateTime
        .ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault())
}
