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

import at.florianschuster.watchables.model.Search
import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.Decoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.SerialClassDescImpl
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

    override fun deserialize(decoder: Decoder): LocalDate? =
        if (!decoder.decodeNotNullMark()) {
            null
        } else try {
            LocalDate.parse(decoder.decodeString(), formatter)
        } catch (e: DateTimeException) {
            null
        }
}

@Serializer(forClass = Search.Result::class)
object SearchResultSerializer : KSerializer<Search.Result?> {
    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("Search.Result") {
        init {
            addElement("id") //0
            addElement("poster_path", true) //1
            addElement("title", true) //2
            addElement("name", true) //3
        }
    }

    override fun deserialize(decoder: Decoder): Search.Result? {
        var id: Int? = null
        var type: Search.Result.Type? = null
        var poster: String? = null
        var title: String? = null

        val input: CompositeDecoder = decoder.beginStructure(descriptor)
        loop@ while (true) {
            when (val elementIndex = input.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> id = input.decodeIntElement(descriptor, elementIndex)
                1 -> poster = input.decodeStringElement(descriptor, elementIndex)
                2 -> {
                    title = input.decodeStringElement(descriptor, elementIndex)
                    type = Search.Result.Type.movie
                }
                3 -> {
                    title = input.decodeStringElement(descriptor, elementIndex)
                    type = Search.Result.Type.show
                }
                else -> throw SerializationException("Unknown index $elementIndex")
            }
        }

        return if (id == null || type == null || title == null) null
        else Search.Result(id, type, poster, title, false)
    }
}