package at.florianschuster.watchables.all.util.gson

import at.florianschuster.watchables.model.Movie
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class MovieReleaseDateTypeTypeAdapter : TypeAdapter<Movie.ReleaseDates.Result.Date.Type>() {

    override fun write(out: JsonWriter, value: Movie.ReleaseDates.Result.Date.Type) {
        out.value(value.ordinal)
    }

    override fun read(`in`: JsonReader): Movie.ReleaseDates.Result.Date.Type =
        Movie.ReleaseDates.Result.Date.Type.values()[`in`.nextInt()]
}
