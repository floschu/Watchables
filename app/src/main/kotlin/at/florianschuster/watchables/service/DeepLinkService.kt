package at.florianschuster.watchables.service

import android.content.res.Resources
import android.net.Uri
import android.webkit.URLUtil
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.Watchable
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.tailoredapps.androidutil.firebase.RxTasks
import io.reactivex.Maybe

interface DeepLinkService {
    sealed class Link {
        data class ToWatchable(val id: String, val type: Watchable.Type) : Link()
        object None : Link()
    }

    fun createDeepLink(watchable: Watchable): String
    fun parseDeepLink(url: String): Maybe<Link>
}

class FirebaseDeepLinkService(
    resources: Resources
) : DeepLinkService {
    private val deepLinkDomain = resources.getString(R.string.dynamic_deeplink_domain)
    private val watchableLinkPrefix = resources.getString(R.string.dynamic_watchable_link_prefix)

    override fun createDeepLink(watchable: Watchable): String {
        return createWatchableDeepLink(watchable).buildDynamicLink().uri.toString()
    }

    private fun createWatchableDeepLink(watchable: Watchable): DynamicLink.Builder {
        return FirebaseDynamicLinks.getInstance().createDynamicLink().apply {
            setDomainUriPrefix(deepLinkDomain)
            setLink(Uri.parse("$watchableLinkPrefix${watchable.type.name}/${watchable.id}"))
            setAndroidParameters(DynamicLink.AndroidParameters.Builder().apply {
                setMinimumVersion(30) // todo
            }.build())
            setSocialMetaTagParameters(DynamicLink.SocialMetaTagParameters.Builder().apply {
                setTitle(watchable.name)
            }.build())
        }
    }

    override fun parseDeepLink(url: String): Maybe<DeepLinkService.Link> {
        if (!URLUtil.isValidUrl(url)) return Maybe.empty()
        return RxTasks.maybe { FirebaseDynamicLinks.getInstance().getDynamicLink(Uri.parse(url)) }
            .map { it.link.toString() }
            .map {
                when {
                    it.startsWith(watchableLinkPrefix) -> {
                        val parts = it.removePrefix(watchableLinkPrefix).split("/")
                        if (parts.count() == 2) {
                            DeepLinkService.Link.ToWatchable(parts[1], enumValueOf(parts[0]))
                        } else DeepLinkService.Link.None
                    }
                    else -> DeepLinkService.Link.None
                }
            }
    }
}