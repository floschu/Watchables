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

package at.florianschuster.watchables.service.remote

import kotlinx.serialization.Decoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.withName
import org.threeten.bp.DateTimeException
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : KSerializer<LocalDate?> {
    private val formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd")
        .withLocale(Locale.ENGLISH)

    override val descriptor: SerialDescriptor = StringDescriptor.withName(LocalDate::class.java.simpleName)

    override fun deserialize(decoder: Decoder): LocalDate? =
        if (!decoder.decodeNotNullMark()) {
            null
        } else try {
            LocalDate.parse(decoder.decodeString(), formatter)
        } catch (e: DateTimeException) {
            null
        }
}