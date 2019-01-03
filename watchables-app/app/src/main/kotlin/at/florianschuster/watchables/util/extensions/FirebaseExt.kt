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

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import io.reactivex.*
import java.io.File
import java.lang.ArithmeticException


//google task

object RxTasks {
    fun <T> single(taskCreator: () -> Task<T>): Single<T> =
            Single.create { taskCreator().singleEmitter(it) }

    fun <T> completable(taskCreator: () -> Task<T>): Completable =
            Completable.create { taskCreator().completableEmitter(it) }

    private fun <T> Task<T>.singleEmitter(emitter: SingleEmitter<T>) {
        addOnSuccessListener(emitter::onSuccess)
        addOnFailureListener(emitter::onError)
    }

    private fun <T> Task<T>.completableEmitter(emitter: CompletableEmitter) {
        addOnSuccessListener { emitter.onComplete() }
        addOnFailureListener(emitter::onError)
    }
}

//firestore

open class FirestoreObject(var deleted: Boolean = false) {
    @get:Exclude
    var id: String = ""

    fun with(obj: FirestoreObject): FirestoreObject {
        this.id = obj.id
        this.deleted = obj.deleted
        return this
    }
}

inline fun <reified T : FirestoreObject> DocumentSnapshot.localObject(): T? =
        toObject(T::class.java)?.apply { this.id = getId() }

inline fun <reified T : FirestoreObject> QuerySnapshot.localObjects(): List<T> =
        if (this.isEmpty) emptyList() else documents.mapNotNull { it.localObject<T>() }

fun <T : FirestoreObject> CollectionReference.createObject(objectToCreate: T): Completable =
        RxTasks.completable {
            if (objectToCreate.id.isNotEmpty()) {
                document(objectToCreate.id).set(objectToCreate)
            } else {
                document().set(objectToCreate)
            }
        }

fun DocumentReference.updateObject(value: Any): Completable =
        RxTasks.completable { set(value, SetOptions.merge()) }

fun DocumentReference.updateField(key: String, value: Any): Completable =
        RxTasks.completable { set(mapOf(key to value), SetOptions.merge()) }

fun DocumentReference.updateNestedField(fieldName: String, keyValuePair: Pair<String, Any>): Completable =
        RxTasks.completable { update("$fieldName.${keyValuePair.first}", keyValuePair.second) }

inline fun <reified T : FirestoreObject> DocumentReference.localObject(): Single<T> =
        RxTasks.single(::get).map {
            if (!it.exists()) throw NoSuchElementException()
            else it.localObject<T>()
        }

inline fun <reified T : FirestoreObject> Query.localObjectList(): Single<List<T>> =
        RxTasks.single(::get).map { it.localObjects<T>() }

inline fun <reified T : FirestoreObject> DocumentReference.localObjectObservable(): Flowable<T> =
        Flowable.create({ emitter: FlowableEmitter<T> ->
            val listener = addSnapshotListener { snapshot, error ->
                val element: T? = snapshot?.localObject()
                when {
                    error != null -> emitter.onError(error)
                    element != null -> emitter.onNext(element)
                }
            }
            emitter.setCancellable(listener::remove)
        }, BackpressureStrategy.LATEST)

inline fun <reified T : FirestoreObject> Query.localObjectListObservable(): Flowable<List<T>> =
        Flowable.create({ emitter: FlowableEmitter<List<T>> ->
            val listener = addSnapshotListener { snapshot, error ->
                val elements: List<T>? = snapshot?.localObjects()
                when {
                    error != null -> emitter.onError(error)
                    elements != null -> emitter.onNext(elements)
                }
            }
            emitter.setCancellable(listener::remove)
        }, BackpressureStrategy.LATEST)

//firebase storage

fun StorageReference.upload(file: File, customMetaData: StorageMetadata? = null): Flowable<Long> =
        Flowable.create({ emitter ->
            emitter.onNext(0)
            val uploadTask = when (customMetaData) {
                null -> putFile(Uri.fromFile(file))
                else -> putFile(Uri.fromFile(file), customMetaData)
            }.also {
                it.addOnFailureListener(emitter::onError)
                it.addOnSuccessListener { emitter.onComplete() }
                it.addOnProgressListener {
                    try {
                        val percentage = 100 * it.bytesTransferred / it.totalByteCount
                        emitter.onNext(percentage)
                    } catch (e: ArithmeticException) {
                        emitter.onNext(0)
                    }
                }
            }
            emitter.setCancellable { uploadTask.cancel() }
        }, BackpressureStrategy.LATEST)