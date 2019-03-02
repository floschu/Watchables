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

package at.florianschuster.watchables

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore


private const val VERSION = "v2"

fun FirebaseFirestore.users(): CollectionReference = collection("$VERSION/database/users")
fun FirebaseFirestore.user(userId: String): DocumentReference = document("$VERSION/database/users/$userId")
fun FirebaseFirestore.watchables(userId: String): CollectionReference = collection("$VERSION/database/users/$userId/watchables")
fun FirebaseFirestore.seasons(userId: String): CollectionReference = collection("$VERSION/database/users/$userId/seasons")
