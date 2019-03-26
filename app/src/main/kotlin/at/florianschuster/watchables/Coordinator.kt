package at.florianschuster.watchables

import android.app.Activity
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import timber.log.Timber

/**
 * Lazily creates a [Coordinator] and adds it as observer to the lifecycle of the [LifecycleOwner].
 */
fun <C : Coordinator<*, *>> LifecycleOwner.coordinator(factory: () -> C): Lazy<C> {
    return lazy { factory().also { lifecycle.addObserver(it) } }
}

/**
 * A Coordinator handles navigation or view flow for one or more view controller (e.g. [Fragment],
 * [Activity], [ViewGroup]). Its purpose is to isolate navigation logic.
 *
 * A [Route] defines the routes that the coordinator can navigate to with the help of a
 * [NavigationHandler].
 *
 * It needs a [Router] to get the routes to navigate to and can optionally accept a [LifecycleOwner]
 * that the [Coordinator] binds to, to automatically dispose of the [CompositeDisposable].
 */
abstract class Coordinator<Route, NavigationHandler>(
        router: Router,
        lifecycleOwner: LifecycleOwner? = null
) : LifecycleObserver where Route : AppRoute, NavigationHandler : Any {
    private val disposables = CompositeDisposable()
    private var handler: NavigationHandler? = null

    init {
        @Suppress("LeakingThis")
        lifecycleOwner?.lifecycle?.addObserver(this)

        router.routes
                .flatMapMaybe {
                    @Suppress("UNCHECKED_CAST")
                    val route: Route? = (it as? Route)
                    val handler: NavigationHandler? = handler

                    if (route != null && handler != null) {
                        Maybe.just(route to handler)
                    } else {
                        Maybe.empty()
                    }
                }
                .subscribe({ navigate(it.first, it.second) }, Timber::e)
                .addTo(disposables)
    }

    /**
     * Attaches a navigation handler to this [Coordinator] that is used to handle navigation.
     */
    fun provideNavigationHandler(handler: NavigationHandler) {
        this.handler = handler
    }

    /**
     * Method that handles the navigation that is defined through a [Route].
     */
    abstract fun navigate(route: Route, handler: NavigationHandler)

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onCleared() {
        disposables.clear()
        handler = null
    }

}