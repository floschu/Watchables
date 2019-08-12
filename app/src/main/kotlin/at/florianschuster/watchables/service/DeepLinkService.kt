package at.florianschuster.watchables.service

import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import at.florianschuster.watchables.model.Watchable
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.tailoredapps.androidutil.firebase.RxTasks
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

interface DeepLinkService {
    sealed class Link(val link: Uri) {
        object App : Link(Uri.parse("$domain/app"))

        data class ToWatchable(
            val id: String,
            val type: Watchable.Type
        ) : Link(Uri.parse("$prefix/${type.name}/$id")) {
            companion object {
                const val prefix = "https://florianschuster.at/watchables"
            }
        }

        object None : Link(Uri.EMPTY)

        companion object {
            const val domain = "https://watchables.page.link"
        }
    }

    fun handleIntent(intent: Intent): Maybe<Link>
    fun parseDeepLink(url: String): Maybe<Link>

    fun createDeepLinkUrl(watchable: Watchable, short: Boolean = true): Maybe<String>
}

class FirebaseDeepLinkService : DeepLinkService {
    override fun handleIntent(intent: Intent): Maybe<DeepLinkService.Link> {
        return RxTasks.maybe { FirebaseDynamicLinks.getInstance().getDynamicLink(intent) }
            .map { it.mapToDeepLink() }
            .subscribeOn(Schedulers.io())
    }

    override fun parseDeepLink(url: String): Maybe<DeepLinkService.Link> {
        if (!URLUtil.isValidUrl(url)) return Maybe.empty()
        return Single.fromCallable { Uri.parse(url) }
            .flatMapMaybe { RxTasks.maybe { FirebaseDynamicLinks.getInstance().getDynamicLink(it) } }
            .map { it.mapToDeepLink() }
            .subscribeOn(Schedulers.io())
    }

    private fun PendingDynamicLinkData.mapToDeepLink(): DeepLinkService.Link {
        val link = link.toString()
        return when {
            link.startsWith(DeepLinkService.Link.ToWatchable.prefix) -> {
                val parts = link.removePrefix(DeepLinkService.Link.ToWatchable.prefix)
                    .split("/")
                if (parts.count() == 3) {
                    DeepLinkService.Link.ToWatchable(parts[2], enumValueOf(parts[1]))
                } else DeepLinkService.Link.None
            }
            else -> DeepLinkService.Link.None
        }
    }

    override fun createDeepLinkUrl(watchable: Watchable, short: Boolean): Maybe<String> {
        return when {
            short -> {
                RxTasks.maybe { createFirebaseDynamicLinkBuilder(watchable).buildShortDynamicLink() }
                    .map { it.shortLink.toString() }
            }
            else -> {
                Maybe.just(createFirebaseDynamicLinkBuilder(watchable).buildDynamicLink())
                    .map { it.uri.toString() }
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun createFirebaseDynamicLinkBuilder(watchable: Watchable): DynamicLink.Builder {
        return FirebaseDynamicLinks.getInstance().createDynamicLink().apply {
            setDomainUriPrefix(DeepLinkService.Link.domain)
            setLink(DeepLinkService.Link.ToWatchable(watchable.id, watchable.type).link)
            setAndroidParameters(DynamicLink.AndroidParameters.Builder().apply {
                setMinimumVersion(30)
            }.build())
            setSocialMetaTagParameters(DynamicLink.SocialMetaTagParameters.Builder().apply {
                setTitle(watchable.name)
            }.build())
        }
    }
}