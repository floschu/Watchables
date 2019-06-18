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

package at.florianschuster.watchables.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.leakcanary.RefWatcher
import io.reactivex.disposables.CompositeDisposable
import org.koin.android.ext.android.inject

abstract class BaseBottomSheetDialogFragment(
    @LayoutRes protected val layout: Int? = null,
    private val fragmentTag: String
) : BottomSheetDialogFragment() {
    private val refWatcher: RefWatcher by inject()
    open val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        if (layout != null) inflater.inflate(layout, container, false)
        else throw RuntimeException("Please implement onCreateView() with your layout.")

    fun show(manager: FragmentManager?) {
        if (manager == null || manager.findFragmentByTag(fragmentTag) != null) return
        super.show(manager, fragmentTag)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        throw IllegalStateException("Use fun show(FragmentManager)")
    }

    override fun show(transaction: FragmentTransaction, tag: String?): Int {
        throw IllegalStateException("Use fun show(FragmentManager)")
    }

    override fun showNow(manager: FragmentManager, tag: String?) {
        throw IllegalStateException("Use fun show(FragmentManager)")
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }
}
