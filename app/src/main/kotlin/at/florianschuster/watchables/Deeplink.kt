package at.florianschuster.watchables

import android.webkit.URLUtil
import at.florianschuster.watchables.model.Watchable
import retrofit2.http.Url
import java.net.URL

object Deeplink {
    data class WatchableLink(val id: String, val type: Watchable.Type)

    private const val protocol = "https"
    private const val host = "watchables.page.link"

    private const val linkQuery = "link"
    private const val linkPrefix = "https://florianschuster.at/watchables/"

    fun valid(url: String): Boolean {
        if (!URLUtil.isValidUrl(url)) return false
        return URL(url).let { it.protocol == protocol && it.host == host }
    }

    //https://watchables.page.link/app?link=https://florianschuster.at/watchables/movie/299537
    fun parseWatchableLink(url: String): WatchableLink? {
        if (!valid(url)) return null
        val query = URL(url).query
        if (!query.startsWith(linkQuery)) return null
        val pathSplits = query.removePrefix(linkQuery)
            .removePrefix("=")
            .removePrefix(linkPrefix)
            .split("/")
            .filter { it.isNotEmpty() }
        return WatchableLink(pathSplits[1], Watchable.Type.valueOf(pathSplits[0]))
    }
}