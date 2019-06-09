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

package at.florianschuster.watchables.ui.watchables.filter

import at.florianschuster.watchables.model.WatchableSeason
import at.florianschuster.watchables.ui.watchables.WatchableContainer

enum class WatchableContainerSortingType(val comparator: Comparator<in WatchableContainer>) {
    ByWatched(compareBy({ it.watchable.watched }, { it.watchable.name })),
    ByLastUsed(
        compareByDescending<WatchableContainer> { it.watchable.lastUpdated }
            .thenByDescending { it.seasons?.sortedByDescending(WatchableSeason::lastUpdated)?.firstOrNull()?.lastUpdated }
            .thenByDescending { it.watchable.name }
    ),
    ByName(compareBy({ it.watchable.name }, { it.watchable.watched })),
    ByType(compareBy({ it.watchable.type.name }, { it.watchable.watched })),
}