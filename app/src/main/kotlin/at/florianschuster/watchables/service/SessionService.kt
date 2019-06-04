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

package at.florianschuster.watchables.service

import android.content.Context
import at.florianschuster.watchables.R
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.tailoredapps.androidutil.firebase.RxTasks
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.Flowables
import java.lang.Exception

sealed class Session<out UserType> {
    open operator fun invoke(): UserType? = null
    data class None<out UserType>(val error: Throwable? = null) : Session<UserType>()
    data class Exists<out UserType>(val user: UserType) : Session<UserType>() {
        override operator fun invoke(): UserType = user
    }

    val loggedIn: Boolean
        get() = this is Exists
}

interface SessionService<User, Credential> {
    val loggedIn: Boolean
    val user: Single<User>
    val session: Flowable<Session<User>>

    fun login(credential: Credential): Completable
    fun logout(): Completable
}

class FirebaseSessionService(
    private val context: Context
) : SessionService<FirebaseUser, AuthCredential> {
    private val auth = FirebaseAuth.getInstance()
    private val authThrowable: FirebaseAuthException
        get() = FirebaseAuthException("69", context.getString(R.string.error_not_authenticated))

    override val loggedIn: Boolean
        get() = try {
            user.blockingGet() != null
        } catch (e: Exception) {
            false
        }

    override val user: Single<FirebaseUser>
        get() = Single.create { emitter ->
            val user = auth.currentUser
            if (user != null) emitter.onSuccess(user)
            else emitter.onError(authThrowable)
        }

    override val session: Flowable<Session<FirebaseUser>>
        get() = Flowables.create(BackpressureStrategy.LATEST) { emitter ->
            val emitSessionConsumer = Consumer<FirebaseUser?> {
                if (it != null) emitter.onNext(Session.Exists(it))
                else emitter.onNext(Session.None(authThrowable))
            }

            val authListener = FirebaseAuth.AuthStateListener { emitSessionConsumer.accept(it.currentUser) }
            auth.addAuthStateListener(authListener)
            emitter.setCancellable { auth.removeAuthStateListener(authListener) }
        }

    override fun login(credential: AuthCredential): Completable = RxTasks.completable {
        auth.signInWithCredential(credential)
    }

    override fun logout(): Completable = Completable.fromAction {
        auth.signOut()
    }
}