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

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity


/**
 * Fills an intent with extra parameters.
 *
 * @receiver Bundle
 * @param params Array<out Pair<String, Any>>
 * @return Bundle
 */
fun Intent.extras(vararg params: Pair<String, Any>) = apply { putExtras(bundleOf(*params)) }

/**
 * Lazily retrieves an extra from an the activity intent.
 *
 * @receiver FragmentActivity
 * @param key String
 * @param defaultValue T?
 * @return Lazy<T>
 */
inline fun <reified T : Any> FragmentActivity.extra(key: String, defaultValue: T? = null): Lazy<T> = lazy {
    if (defaultValue == null) intent.extras?.get(key) as T
    else intent.extras?.get(key) as? T ?: defaultValue
}

/**
 * Fills the Fragment arguments with with parameters.
 *
 * @receiver Bundle
 * @param params Array<out Pair<String, Any>>
 * @return Bundle
 */
fun Fragment.args(vararg params: Pair<String, Any>) = apply { arguments = bundleOf(*params) }

/**
 * Lazily retrieves an argument from the fragment arguments.
 *
 * @receiver Fragment
 * @param key String
 * @param defaultValue T?
 * @return Lazy<T>
 */
inline fun <reified T : Any> Fragment.argument(key: String, defaultValue: T? = null): Lazy<T> = lazy {
    if (defaultValue == null) arguments?.get(key) as T
    else arguments?.get(key) as? T ?: defaultValue
}
