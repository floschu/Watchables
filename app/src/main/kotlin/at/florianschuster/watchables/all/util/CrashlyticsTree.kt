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

package at.florianschuster.watchables.all.util

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            Log.ERROR, Log.WARN -> {
                val throwable = t ?: CrashlyticsException(customMessage(priority, message))
                Crashlytics.logException(throwable)
            }
            else -> return
        }
    }

    private fun customMessage(priority: Int, message: String): String = when (priority) {
        Log.VERBOSE -> "VERBOSE"
        Log.DEBUG -> "DEBUG"
        Log.INFO -> "INFO"
        Log.WARN -> "WARN"
        Log.ERROR -> "ERROR"
        Log.ASSERT -> "ASSERT"
        else -> "NO_PRIORITY"
    }.let { "$it: $message" }

    private inner class CrashlyticsException(message: String) : Throwable(message) {
        @Synchronized
        override fun fillInStackTrace(): Throwable {
            super.fillInStackTrace()

            val ignoredClassNames = listOf(Timber::class.java.simpleName, CrashlyticsTree::class.java.simpleName)

            stackTrace = stackTrace.asSequence()
                    .filterNot { it.fileName != null && ignoredClassNames.any { name -> it.fileName.contains(name) } }
                    .filterNot { it.className != null && ignoredClassNames.any { name -> it.className.contains(name) } }
                    .toList()
                    .toTypedArray()

            return this
        }
    }
}
