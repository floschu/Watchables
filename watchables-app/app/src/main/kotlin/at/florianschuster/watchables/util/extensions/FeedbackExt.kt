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
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar


fun Context.toast(@StringRes titleRes: Int, duration: Int = Toast.LENGTH_LONG) =
        Feedback.toast(this, getString(titleRes), duration)

fun Context.toast(title: String, duration: Int = Toast.LENGTH_LONG) =
        Feedback.toast(this, title, duration)

fun View.snack(title: CharSequence, duration: Int = Snackbar.LENGTH_LONG) =
        Feedback.snack(this, title, null, duration)

fun View.snack(title: CharSequence, actionText: CharSequence, action: () -> Unit) =
        Feedback.snack(this, title, actionText to action, Snackbar.LENGTH_INDEFINITE)

fun View.snack(@StringRes titleRes: Int, duration: Int = Snackbar.LENGTH_LONG) =
        Feedback.snack(this, context.getString(titleRes), null, duration)

fun View.snack(@StringRes titleRes: Int, @StringRes actionTextRes: Int, action: () -> Unit) =
        Feedback.snack(this, context.getString(titleRes), context.getString(actionTextRes) to action, Snackbar.LENGTH_INDEFINITE)

private object Feedback {
    fun toast(context: Context, title: String, duration: Int) {
        val toastDuration = if (duration == Toast.LENGTH_LONG || duration == Toast.LENGTH_SHORT) duration else Toast.LENGTH_SHORT
        Toast.makeText(context.applicationContext, title, toastDuration).apply { show() }
    }

    fun snack(view: View, title: CharSequence, actionPair: Pair<CharSequence, () -> Unit>?, duration: Int) {
        val snackbarDuration = if (duration == Snackbar.LENGTH_SHORT || duration == Snackbar.LENGTH_LONG || duration == Snackbar.LENGTH_INDEFINITE) duration else Snackbar.LENGTH_LONG
        if (actionPair != null) {
            Snackbar.make(view, title, snackbarDuration).setAction(actionPair.first) { actionPair.second() }.apply { show() }
        } else {
            Snackbar.make(view, title, snackbarDuration).apply { show() }
        }
    }
}

