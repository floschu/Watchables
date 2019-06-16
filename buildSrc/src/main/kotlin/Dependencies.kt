@Suppress("SpellCheckingInspection")
object Versions {
    const val Kotlin = "1.3.31"
    const val Reaktor = "1.0.16"
    const val Koordinator = "0.0.15"
    const val androidutil = "13"
    const val Navigation = "2.0.0"
    const val Work = "2.0.1"
    const val Lifecycle = "2.0.0"
    const val Koin = "2.0.1"
    const val RxBinding = "3.0.0-alpha2"
    const val Retrofit = "2.5.0"
    const val OkHttp = "3.14.1"
    const val Leakcanary = "1.6.3"
    const val Glide = "4.9.0"
    const val Gander = "1.4.0"
}

@Suppress("SpellCheckingInspection", "unused")
object BuildScriptDependencies {
    const val BuildTools = "com.android.tools.build:gradle:3.4.1"
    const val KotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.Kotlin}"
    const val NavigationSafeArgs = "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.Navigation}"
    const val VersionsPlugin = "com.github.ben-manes:gradle-versions-plugin:0.21.0"
    const val EasyLauncher = "com.akaita.android:easylauncher:1.3.1"
    const val GoogleServices = "com.google.gms:google-services:4.2.0"
    const val Fabric = "io.fabric.tools:gradle:1.29.0"
}

@Suppress("SpellCheckingInspection", "unused")
object Dependencies {
    const val KotlinStd = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.Kotlin}"

    const val Reaktor = "at.florianschuster.reaktor:reaktor-android-koin:${Versions.Reaktor}"

    const val Koordinator = "at.florianschuster.koordinator:koordinator-android-koin:${Versions.Koordinator}"

    val AndroidUtil = dependencyGroup(
        "UI" fromRemote "com.tailoredapps.androidutil:util-ui:${Versions.androidutil}",
        "Async" fromRemote "com.tailoredapps.androidutil:util-async:${Versions.androidutil}",
        "network" fromRemote "com.tailoredapps.androidutil:util-network:${Versions.androidutil}",
        "Optional" fromRemote "com.tailoredapps.androidutil:util-optional:${Versions.androidutil}",
        "Permissions" fromRemote "com.tailoredapps.androidutil:util-permissions:${Versions.androidutil}",
        "Validation" fromRemote "com.tailoredapps.androidutil:util-validation:${Versions.androidutil}",
        "ViewState" fromRemote "com.tailoredapps.androidutil:util-viewstate:${Versions.androidutil}",
        "Firebase" fromRemote "com.tailoredapps.androidutil:util-firebase:${Versions.androidutil}"
    )

    val UI = dependencyGroup(
        "Material" fromRemote "com.google.android.material:material:1.0.0",
        "AppCompat" fromRemote "androidx.appcompat:appcompat:1.0.2",
        "Fragment" fromRemote "androidx.fragment:fragment:1.0.0",
        "Constraintlayout" fromRemote "androidx.constraintlayout:constraintlayout:1.1.3",
        "CoreKTX" fromRemote "androidx.core:core-ktx:1.0.1",
        "Fastscroll" fromRemote "com.simplecityapps:recyclerview-fastscroll:1.0.20"
    )

    val Navigation = dependencyGroup(
        "Core" fromRemote "androidx.navigation:navigation-fragment-ktx:${Versions.Navigation}",
        "UI" fromRemote "androidx.navigation:navigation-ui-ktx:${Versions.Navigation}"
    )

    val Lifecycle = dependencyGroup(
        "Extensions" fromRemote "androidx.lifecycle:lifecycle-extensions:${Versions.Lifecycle}",
        "Compiler" fromRemote "androidx.lifecycle:lifecycle-compiler:${Versions.Lifecycle}"
    )

    val Koin = dependencyGroup(
        "Core" fromRemote "org.koin:koin-core:${Versions.Koin}",
        "Android" fromRemote "org.koin:koin-android:${Versions.Koin}",
        "Scrop" fromRemote "org.koin:koin-androidx-scope:${Versions.Koin}",
        "ViewModel" fromRemote "org.koin:koin-androidx-viewmodel:${Versions.Koin}"
    )

    val Network = dependencyGroup(
        "Core" fromRemote "com.squareup.retrofit2:retrofit:${Versions.Retrofit}",
        "Gson" fromRemote "com.squareup.retrofit2:converter-gson:${Versions.Retrofit}",
        "RxJava" fromRemote "com.squareup.retrofit2:adapter-rxjava2:${Versions.Retrofit}",
        "OkHttp" fromRemote "com.squareup.okhttp3:okhttp:${Versions.OkHttp}",
        "OkHttpLogging" fromRemote "com.squareup.okhttp3:logging-interceptor:${Versions.OkHttp}"
    )

    val Local = dependencyGroup(
        "Hawk" to "com.orhanobut:hawk:2.0.1"
    )

    val Firebase = dependencyGroup(
        "Core" fromRemote "com.google.firebase:firebase-core:16.0.9",
        "Auth" fromRemote "com.google.firebase:firebase-auth:17.0.0",
        "gmsauth" fromRemote "com.google.android.gms:play-services-auth:16.0.1",
        "Firestore" fromRemote "com.google.firebase:firebase-firestore:19.0.0",
        "Crashlytics" fromRemote "com.crashlytics.sdk.android:crashlytics:2.9.9",
        "Messaging" fromRemote "com.google.firebase:firebase-messaging:18.0.0",
        "Storage" fromRemote "com.google.firebase:firebase-storage:17.0.0",
        "Performance" fromRemote "com.google.firebase:firebase-perf:17.0.0",
        "Invites" fromRemote "com.google.firebase:firebase-invites:17.0.0"
    )

    val RxJava = dependencyGroup(
        "Core" fromRemote "io.reactivex.rxjava2:rxjava:2.2.9",
        "Kotlin" fromRemote "io.reactivex.rxjava2:rxkotlin:2.3.0",
        "Relay" fromRemote "com.jakewharton.rxrelay2:rxrelay:2.1.0",
        "Android" fromRemote "io.reactivex.rxjava2:rxandroid:2.1.1"
    )

    val RxBinding = dependencyGroup(
        "Binding" fromRemote "com.jakewharton.rxbinding3:rxbinding:${Versions.RxBinding}",
        "Core" fromRemote "com.jakewharton.rxbinding3:rxbinding-core:${Versions.RxBinding}",
        "Appcompat" fromRemote "com.jakewharton.rxbinding3:rxbinding-appcompat:${Versions.RxBinding}",
        "Drawerlayout" fromRemote "com.jakewharton.rxbinding3:rxbinding-drawerlayout:${Versions.RxBinding}",
        "Recyclerview" fromRemote "com.jakewharton.rxbinding3:rxbinding-recyclerview:${Versions.RxBinding}",
        "Swiperefreshlayout" fromRemote "com.jakewharton.rxbinding3:rxbinding-swiperefreshlayout:${Versions.RxBinding}",
        "Viewpager" fromRemote "com.jakewharton.rxbinding3:rxbinding-viewpager:${Versions.RxBinding}",
        "Material" fromRemote "com.jakewharton.rxbinding3:rxbinding-material:${Versions.RxBinding}"
    )

    val MiscUtil = dependencyGroup(
        "Gson" fromRemote "com.google.code.gson:gson:2.8.5",
        "Timber" fromRemote "com.jakewharton.timber:timber:4.7.1",
        "Threeten" fromRemote "com.jakewharton.threetenabp:threetenabp:1.2.1",
        "Flick" fromRemote "me.saket:flick:1.4.0",
        "Gestureviews" fromRemote "com.alexvasilkov:gesture-views:2.5.2",
        "Aboutlibs" fromRemote "com.mikepenz:aboutlibraries:6.2.1",
        "Customtabs" fromRemote "com.android.support:customtabs:28.0.0"
    )

    val LeakCanary = dependencyGroup(
        "Op" fromRemote "com.squareup.leakcanary:leakcanary-android:${Versions.Leakcanary}",
        "NoOp" fromRemote "com.squareup.leakcanary:leakcanary-android-no-op:${Versions.Leakcanary}"
    )

    val Glide = dependencyGroup(
        "Core" fromRemote "com.github.bumptech.glide:glide:${Versions.Glide}",
        "Compiler" fromRemote "com.github.bumptech.glide:compiler:${Versions.Glide}",
        "Transformation" fromRemote "jp.wasabeef:glide-transformations:4.0.1"
    )

    val Work = dependencyGroup(
        "Core" fromRemote "androidx.work:work-runtime:${Versions.Work}",
        "RxJava" fromRemote "androidx.work:work-rxjava2:${Versions.Work}"
    )

    val Gander = dependencyGroup(
        "Op" fromRemote "com.ashokvarma.android:gander:${Versions.Gander}",
        "NoOp" fromRemote "com.ashokvarma.android:gander-no-op:${Versions.Gander}"
    )
}

private fun dependencyGroup(vararg dependencies: Pair<String, String>) = mapOf(*dependencies)
private infix fun String.fromRemote(that: String): Pair<String, String> = Pair(this, that)