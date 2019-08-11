package at.florianschuster.watchables.ui.main

import at.florianschuster.koordinator.android.koin.coordinator
import at.florianschuster.reaktor.android.koin.reactor
import org.koin.dsl.module

/**
 * Created by Florian Schuster
 * florian.schuster@tailored-apps.com
 */

internal val mainModule = module {
    coordinator { MainCoordinator() }
    reactor { MainReactor(get(), get(), get()) }
}