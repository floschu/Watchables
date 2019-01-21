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
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.SharedElementCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.florianschuster.watchables.R
import at.florianschuster.watchables.util.Utils
import io.reactivex.functions.Consumer
import timber.log.Timber


/**
 * Inflates a View in a ViewGroup.
 *
 * @receiver ViewGroup
 * @param layoutRes Int
 * @return View
 */
fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View =
        LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)

fun Context.toast(@StringRes titleRes: Int, duration: Int = Toast.LENGTH_LONG) {
    val toastDuration = if (duration == Toast.LENGTH_LONG || duration == Toast.LENGTH_SHORT) duration else Toast.LENGTH_SHORT
    Toast.makeText(applicationContext, getString(titleRes), toastDuration).apply { show() }
}

fun Context.toast(title: String, duration: Int = Toast.LENGTH_LONG) {
    val toastDuration = if (duration == Toast.LENGTH_LONG || duration == Toast.LENGTH_SHORT) duration else Toast.LENGTH_SHORT
    Toast.makeText(applicationContext, title, toastDuration).apply { show() }
}

fun FragmentActivity.openChromeTab(url: String) {
    if (url.isEmpty()) return
    try {
        val tabsIntent = CustomTabsIntent.Builder().apply {
            setToolbarColor(ContextCompat.getColor(this@openChromeTab, R.color.colorPrimary))
            setShowTitle(true)
            enableUrlBarHiding()
        }.build()
        tabsIntent.launchUrl(this, Uri.parse(url))
    } catch (throwable: Throwable) {
        Timber.w(throwable)
        startActivity(Utils.web(url)) //fallback of chrome not installed -> open default browser
    }
}

fun Fragment.openChromeTab(url: String) {
    val activity = this.activity
            ?: throw RuntimeException("No Activity attached to Fragment. Cannot show Dialog.")
    activity.openChromeTab(url)
}


fun RecyclerView.smoothScrollUp(firstVisiblePosition: Int = 50) {
    val firstVisible = (layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
    if (firstVisible != null && firstVisible < firstVisiblePosition) {
        smoothScrollToPosition(0)
    } else {
        scrollToPosition(0)
    }
}

fun RecyclerView.addScrolledPastFirstItemListener(scrolledPastConsumer: Consumer<in Boolean>) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val firstVisible = (layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
            scrolledPastConsumer.accept(firstVisible != null && firstVisible > 0)
        }
    })
}

fun Fragment.onSharedEnterTransition(callback: () -> Unit) = setEnterSharedElementCallback(object : SharedElementCallback() {
    override fun onSharedElementEnd(sharedElementNames: MutableList<String>?, sharedElements: MutableList<View>?, sharedElementSnapshots: MutableList<View>?) {
        super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots)
        callback.invoke()
    }
})

inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                f()
            }
        }
    })
}