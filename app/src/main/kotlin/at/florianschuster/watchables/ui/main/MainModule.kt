package at.florianschuster.watchables.ui.main

import com.tailoredapps.reaktor.android.koin.reactor
import org.koin.dsl.module

/**
 * Created by Florian Schuster
 * florian.schuster@tailored-apps.com
 */

internal val mainModule = module {
    reactor { MainReactor(get(), get()) }
}