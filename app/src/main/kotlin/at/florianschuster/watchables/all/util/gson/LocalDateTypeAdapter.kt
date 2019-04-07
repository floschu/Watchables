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

package at.florianschuster.watchables.all.util.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

class LocalDateTypeAdapter : TypeAdapter<LocalDate?>() {

    override fun write(out: JsonWriter, value: LocalDate?) {
        out.value(value.toString())
    }

    override fun read(`in`: JsonReader): LocalDate? =
            if (`in`.peek() == JsonToken.NULL) {
                `in`.nextNull()
                null
            } else {
                val value = `in`.nextString()
                val formatter = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd")
                        .withLocale(Locale.ENGLISH)
                LocalDate.parse(value, formatter)
            }
}