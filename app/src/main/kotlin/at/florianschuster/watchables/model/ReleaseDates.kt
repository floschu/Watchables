package at.florianschuster.watchables.model

import androidx.core.os.LocaleListCompat
import java.util.Locale
import org.threeten.bp.LocalDate

data class ReleaseDates(
    val preferred: LocalizedReleaseDate,
    val all: List<LocalizedReleaseDate> = listOf(preferred)
) {
    data class LocalizedReleaseDate(val locale: Locale, val date: LocalDate)
}

fun Show.releaseDates(): ReleaseDates? {
    val airingDate = nextEpisode?.airingDate ?: return null
    return ReleaseDates(ReleaseDates.LocalizedReleaseDate(Locale.US, airingDate))
}

fun Movie.releaseDates(): ReleaseDates? {
    val results = releaseDates?.results ?: return null
    val dates = results
        .mapNotNull {
            val date = it.dates.minBy(Movie.ReleaseDates.Result.Date::type)?.date
            if (date != null) {
                ReleaseDates.LocalizedReleaseDate(Locale("", it.countryIso), date)
            } else {
                null
            }
        }
        .sortedBy { it.locale.displayCountry }

    val sortedByCountry = LocaleListCompat.getAdjustedDefault()
        .let { locales -> (0 until locales.size()).map(locales::get) }
        .mapNotNull { locale -> dates.firstOrNull { locale.isO3Country == it.locale.isO3Country } }

    return ReleaseDates(sortedByCountry.firstOrNull() ?: dates.first(), dates)
}
