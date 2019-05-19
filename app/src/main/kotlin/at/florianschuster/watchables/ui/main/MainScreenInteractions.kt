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

package at.florianschuster.watchables.ui.main

import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import java.lang.IllegalStateException

interface MainScreenInteractions {
    val mainFab: FloatingActionButton
    val mainFabClickRelay: PublishRelay<Unit>
    val bnvReselectRelay: PublishRelay<MenuItem>
}

val Fragment.mainFabClicks: Observable<Unit>
    get() {
        val activity = activity as? MainScreenInteractions ?: throw IllegalStateException()
        return activity.mainFabClickRelay.hide()
    }

fun Fragment.setMainScreenFabVisibility(visible: Boolean) {
    val activity = activity as? MainScreenInteractions ?: return
    activity.mainFab.isVisible = visible
}

val Fragment.bnvReselects: Observable<MenuItem>
    get() {
        val activity = activity as? MainScreenInteractions ?: throw IllegalStateException()
        return activity.bnvReselectRelay.hide()
    }