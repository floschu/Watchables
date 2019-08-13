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

package at.florianschuster.watchables.all.util.extensions

import androidx.recyclerview.widget.DiffUtil
import com.tailoredapps.androidutil.async.Async
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.annotations.Experimental

@Deprecated(message = "Replace with AndroidAppUtil.Async if v16 is available.")
@Experimental
@CheckReturnValue
fun <T : Any> Maybe<T>.mapToAsync(onComplete: () -> Async<T> = { Async.Error(NoSuchElementException()) }): Single<Async<T>> {
    return materialize()
        .flatMap {
            val value = it.value
            val error = it.error
            when {
                it.isOnNext && value != null -> Single.just(Async.Success(value))
                it.isOnError && error != null -> Single.just(Async.Error(error))
                it.isOnComplete -> Single.just(onComplete())
                else -> Single.never()
            }
        }
}

fun <T : Any> Observable<List<T>>.rxDiff(
    differ: (List<T>, List<T>) -> DiffUtil.Callback
): Observable<Pair<List<T>, DiffUtil.DiffResult?>> {
    val seedPair: Pair<List<T>, DiffUtil.DiffResult?> = Pair(emptyList(), null)

    return scan(seedPair, { oldPair, nextItems ->
        val callback = differ(oldPair.first, nextItems)
        val result = DiffUtil.calculateDiff(callback, true)

        nextItems to result
    }).skip(1)
}