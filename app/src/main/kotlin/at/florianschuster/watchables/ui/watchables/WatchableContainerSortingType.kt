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

package at.florianschuster.watchables.ui.watchables

import androidx.annotation.StringRes
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.WatchableContainer

enum class WatchableContainerSortingType(val comparator: Comparator<in WatchableContainer>) {
    ByWatched(compareBy({ it.watchable.watched }, { it.watchable.name })),
    ByNameAscending(compareBy({ it.watchable.name }, { it.watchable.watched })),
    ByNameDescending(compareByDescending<WatchableContainer> { it.watchable.name }.thenByDescending { it.watchable.watched }),
    ByTypeAscending(compareBy({ it.watchable.type.name }, { it.watchable.watched })),
    ByTypeDescending(compareByDescending<WatchableContainer> { it.watchable.type.name }.thenByDescending { it.watchable.watched })
}

@get:StringRes
val WatchableContainerSortingType.formatted: Int
    get() = when (this) {
        WatchableContainerSortingType.ByWatched -> R.string.watchables_sorting_watched_name
        WatchableContainerSortingType.ByNameAscending -> R.string.watchables_sorting_name_ascending
        WatchableContainerSortingType.ByNameDescending -> R.string.watchables_sorting_name_descending
        WatchableContainerSortingType.ByTypeAscending -> R.string.watchables_sorting_type_ascending
        WatchableContainerSortingType.ByTypeDescending -> R.string.watchables_sorting_type_descending
    }
