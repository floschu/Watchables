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

package at.florianschuster.watchables.ui.base.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.squareup.leakcanary.RefWatcher
import io.reactivex.disposables.CompositeDisposable
import org.koin.android.ext.android.inject


abstract class BaseFragment(@LayoutRes protected val layout: Int? = null) : Fragment() {
    protected val navController: NavController by lazy { findNavController() }
    private val refWatcher: RefWatcher by inject()
    open val disposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            if (layout != null) inflater.inflate(layout, container, false)
            else throw RuntimeException("Please implement onCreateView() with your layout.")

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        refWatcher.watch(this)
    }
}
