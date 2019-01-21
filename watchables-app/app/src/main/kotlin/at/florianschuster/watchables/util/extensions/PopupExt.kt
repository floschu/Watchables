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

package at.florianschuster.watchables.util.extensions

import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import io.reactivex.Single


sealed class RxPopupAction {
    object Cancelled : RxPopupAction()
    data class Selected(@IdRes val itemId: Int, val menuItem: MenuItem) : RxPopupAction()
}


fun View.rxPopup(@MenuRes menuId: Int): Single<RxPopupAction> = Single.create { emitter ->
    val menu = PopupMenu(context, this).apply {
        menuInflater.inflate(menuId, menu)
        setOnMenuItemClickListener {
            emitter.onSuccess(RxPopupAction.Selected(it.itemId, it))
            true
        }
        setOnDismissListener { emitter.onSuccess(RxPopupAction.Cancelled) }
    }

    emitter.setCancellable { menu.dismiss() }
    menu.show()
}