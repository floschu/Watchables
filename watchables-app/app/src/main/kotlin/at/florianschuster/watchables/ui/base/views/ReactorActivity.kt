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
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import at.florianschuster.watchables.ui.base.reactor.Reactor
import at.florianschuster.watchables.ui.base.reactor.ReactorView


abstract class ReactorActivity<R>(@LayoutRes layout: Int) : BaseActivity(layout), ReactorView<R> where R : Reactor<*, *, *> {
    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind(reactor)
    }
}
