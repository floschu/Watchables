package at.florianschuster.watchables.service.remote

import at.florianschuster.watchables.model.Search
import kotlinx.serialization.CompositeDecoder
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

@Serializer(forClass = Search.SearchItem::class)
object SearchItemSerializer : KSerializer<Search.SearchItem?> {
    override val descriptor: SerialDescriptor = StringDescriptor.withName(Search.SearchItem::class.java.simpleName)

    override fun deserialize(decoder: Decoder): Search.SearchItem? {
        var id: Int? = null
        var mediaType: Search.SearchItem.Type? = null
        var image: String? = null
        var title: String? = null

        val input: CompositeDecoder = decoder.beginStructure(descriptor)
        while (input.)
        loop@ while (true) {
            when (val i = input.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> req = HexConverter.parseHexBinary(input.decodeStringElement(descriptor, i))
                1 -> res = HexConverter.parseHexBinary(input.decodeStringElement(descriptor, i))
                else -> throw SerializationException("Unknown index $i")
            }
        }

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