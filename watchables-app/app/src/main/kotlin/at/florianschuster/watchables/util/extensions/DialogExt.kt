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

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import at.florianschuster.watchables.R
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber


sealed class RxDialogAction {
    object Positive : RxDialogAction()
    object Negative : RxDialogAction()
    object Neutral : RxDialogAction()
    object Cancelled : RxDialogAction()
    data class Selected<T>(val index: Int, val item: T) : RxDialogAction()
}


fun FragmentActivity.rxDialog(init: RxAlert.() -> Unit): Single<RxDialogAction> =
        Single.create { emitter: SingleEmitter<RxDialogAction> ->
            val alert = RxAlert(this, emitter).apply { init() }.build()
            emitter.setCancellable(alert::dismiss)
            alert.show()
        }.doOnError(Timber::e).subscribeOn(AndroidSchedulers.mainThread())

fun Fragment.rxDialog(init: RxAlert.() -> Unit): Single<RxDialogAction> {
    val activity = this.activity
            ?: throw RuntimeException("No Activity attached to Fragment. Cannot show Dialog.")
    return activity.rxDialog(init)
}


class RxAlert(private val context: Context, private val emitter: SingleEmitter<RxDialogAction>) {
    private val builder = AlertDialog.Builder(context, R.style.DialogTheme)

    var title: CharSequence
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setTitle(value)
        }

    @setparam:StringRes
    var titleResource: Int
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setTitle(value)
        }

    var message: CharSequence
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setMessage(value)
        }

    @setparam:StringRes
    var messageResource: Int
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setMessage(value)
        }

    var icon: Drawable
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setIcon(value)
        }

    @setparam:DrawableRes
    var iconResource: Int
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setIcon(value)
        }

    var cancelable: Boolean
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setCancelable(value)
            if (value) builder.setOnCancelListener { emitter.onSuccess(RxDialogAction.Cancelled) }
        }

    var positiveButtonText: CharSequence
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setPositiveButton(value) { _, _ -> emitter.onSuccess(RxDialogAction.Positive) }
        }

    @setparam:StringRes
    var positiveButtonResource: Int
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setPositiveButton(value) { _, _ -> emitter.onSuccess(RxDialogAction.Positive) }
        }

    var negativeButtonText: CharSequence
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setNegativeButton(value) { _, _ -> emitter.onSuccess(RxDialogAction.Negative) }
        }

    @setparam:StringRes
    var negativeButtonResource: Int
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setNegativeButton(value) { _, _ -> emitter.onSuccess(RxDialogAction.Negative) }
        }

    var neutralButtonText: CharSequence
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setNeutralButton(value) { _, _ -> emitter.onSuccess(RxDialogAction.Neutral) }
        }

    @setparam:StringRes
    var neutralButtonResource: Int
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setNeutralButton(value) { _, _ -> emitter.onSuccess(RxDialogAction.Neutral) }
        }

    @setparam:ArrayRes
    var itemsResource: Int
        get() = throw RuntimeException("No getter.")
        set(value) {
            builder.setItems(value) { _, which -> emitter.onSuccess(RxDialogAction.Selected(which, context.resources.getStringArray(value)[which])) }
        }

    fun <T> setItems(vararg elements: T, displayMapping: ((T) -> CharSequence)? = null) {
        val listItems = Array(elements.size) { i ->
            if (displayMapping != null) displayMapping(elements[i])
            else elements[i].toString()
        }
        builder.setItems(listItems) { _, which -> emitter.onSuccess(RxDialogAction.Selected(which, listItems[which])) }
    }

    fun <T> setItems(elements: List<T>, displayMapping: ((T) -> CharSequence)? = null) {
        val listItems = Array(elements.size) { i ->
            if (displayMapping != null) displayMapping(elements[i])
            else elements[i].toString()
        }
        builder.setItems(listItems) { _, which -> emitter.onSuccess(RxDialogAction.Selected(which, listItems[which])) }
    }

    fun <T> setSingleChoiceItems(elements: List<T>, presetIndex: Int = 0, displayMapping: ((T) -> CharSequence)? = null) {
        val listItems = Array(elements.size) { i ->
            if (displayMapping != null) displayMapping(elements[i])
            else elements[i].toString()
        }
        builder.setSingleChoiceItems(listItems, presetIndex) { _, which -> emitter.onSuccess(RxDialogAction.Selected(which, listItems[which])) }
    }


    fun build(): AlertDialog = builder.create()
}
