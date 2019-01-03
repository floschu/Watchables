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

package at.florianschuster.watchables.util.gson

import at.florianschuster.watchables.BuildConfig
import at.florianschuster.watchables.model.Search
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter


class SearchItemTypeAdapter : TypeAdapter<Search.SearchItem>() {
    override fun write(out: JsonWriter, value: Search.SearchItem) {
        //unused
    }

    override fun read(reader: JsonReader): Search.SearchItem? {
        var id: Int? = null
        var mediaType: Search.SearchItem.Type? = null
        var image: String? = null
        var title: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "original_name" -> {
                    title = if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull()
                        null
                    } else reader.nextString()
                    mediaType = Search.SearchItem.Type.tv
                }
                "original_title" -> {
                    title = if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull()
                        null
                    } else reader.nextString()
                    mediaType = Search.SearchItem.Type.movie
                }
                "id" -> id = reader.nextInt()
                "poster_path" -> {
                    when {
                        reader.peek() == JsonToken.NULL -> {
                            reader.nextNull()
                            null
                        }
                        else -> reader.nextString()
                    }?.let { image = it }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return if (id != null && title != null && mediaType != null) {
            Search.SearchItem(id, title, mediaType, image, false)
        } else {
            null
        }
    }
}