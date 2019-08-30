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

import at.florianschuster.watchables.service.local.PrefRepo
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable

interface WatchablesFilterService {
    val currentFilter: WatchableContainerFilterType
    val filter: Flowable<WatchableContainerFilterType>
    val currentSorting: WatchableContainerSortingType
    val sorting: Flowable<WatchableContainerSortingType>

    fun setFilter(filter: WatchableContainerFilterType): Completable
    fun setSorting(sorting: WatchableContainerSortingType): Completable
}

class RxWatchablesFilterService(
    private val prefRepo: PrefRepo
) : WatchablesFilterService {
    private val filterRelay: BehaviorRelay<WatchableContainerFilterType> =
            BehaviorRelay.createDefault(prefRepo.watchableContainerFilterType)
    override val currentFilter: WatchableContainerFilterType
        get() = prefRepo.watchableContainerFilterType
    override val filter: Flowable<WatchableContainerFilterType> =
            filterRelay.toFlowable(BackpressureStrategy.LATEST)

    private val sortingRelay: BehaviorRelay<WatchableContainerSortingType> =
            BehaviorRelay.createDefault(prefRepo.watchableContainerSortingType)
    override val currentSorting: WatchableContainerSortingType
        get() = prefRepo.watchableContainerSortingType
    override val sorting: Flowable<WatchableContainerSortingType> =
            sortingRelay.toFlowable(BackpressureStrategy.LATEST)

    override fun setFilter(filter: WatchableContainerFilterType): Completable = Completable.fromAction {
        prefRepo.watchableContainerFilterType = filter
        filterRelay.accept(filter)
    }

    override fun setSorting(sorting: WatchableContainerSortingType): Completable = Completable.fromAction {
        prefRepo.watchableContainerSortingType = sorting
        sortingRelay.accept(sorting)
    }
}
