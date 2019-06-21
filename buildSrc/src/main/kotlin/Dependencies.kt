@Suppress("SpellCheckingInspection", "unused")
object Dependencies {
    const val KotlinStd = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.Kotlin}"

    const val Reaktor = "at.florianschuster.reaktor:reaktor-android-koin:${Versions.Reaktor}"

    const val Koordinator = "at.florianschuster.koordinator:koordinator-android-koin:${Versions.Koordinator}"

    val AndroidUtil = mapOf(
        "UI" to "com.tailoredapps.androidutil:util-ui:${Versions.AndroidAppUtil}",
        "Async" to "com.tailoredapps.androidutil:util-async:${Versions.AndroidAppUtil}",
        "network" to "com.tailoredapps.androidutil:util-network:${Versions.AndroidAppUtil}",
        "Optional" to "com.tailoredapps.androidutil:util-optional:${Versions.AndroidAppUtil}",
        "Permissions" to "com.tailoredapps.androidutil:util-permissions:${Versions.AndroidAppUtil}",
        "Validation" to "com.tailoredapps.androidutil:util-validation:${Versions.AndroidAppUtil}",
        "ViewState" to "com.tailoredapps.androidutil:util-viewstate:${Versions.AndroidAppUtil}",
        "Firebase" to "com.tailoredapps.androidutil:util-firebase:${Versions.AndroidAppUtil}"
    )

    val UI = mapOf(
        "Material" to "com.google.android.material:material:1.0.0",
        "AppCompat" to "androidx.appcompat:appcompat:1.0.2",
        "Fragment" to "androidx.fragment:fragment:1.0.0",
        "Constraintlayout" to "androidx.constraintlayout:constraintlayout:1.1.3",
        "CoreKTX" to "androidx.core:core-ktx:1.0.1"
    )

    val Navigation = mapOf(
        "Core" to "androidx.navigation:navigation-fragment-ktx:${Versions.Navigation}",
        "UI" to "androidx.navigation:navigation-ui-ktx:${Versions.Navigation}"
    )

    val Lifecycle = mapOf(
        "Extensions" to "androidx.lifecycle:lifecycle-extensions:${Versions.Lifecycle}",
        "Compiler" to "androidx.lifecycle:lifecycle-compiler:${Versions.Lifecycle}"
    )

    val Koin = mapOf(
        "Core" to "org.koin:koin-core:${Versions.Koin}",
        "Android" to "org.koin:koin-android:${Versions.Koin}",
        "Scrop" to "org.koin:koin-androidx-scope:${Versions.Koin}",
        "ViewModel" to "org.koin:koin-androidx-viewmodel:${Versions.Koin}"
    )

    val Network = mapOf(
        "Core" to "com.squareup.retrofit2:retrofit:${Versions.Retrofit}",
        "Gson" to "com.squareup.retrofit2:converter-gson:${Versions.Retrofit}",
        "RxJava" to "com.squareup.retrofit2:adapter-rxjava2:${Versions.Retrofit}",
        "OkHttp" to "com.squareup.okhttp3:okhttp:${Versions.OkHttp}",
        "OkHttpLogging" to "com.squareup.okhttp3:logging-interceptor:${Versions.OkHttp}"
    )

    val Local = mapOf(
        "Hawk" to "com.orhanobut:hawk:2.0.1"
    )

    val Firebase = mapOf(
        "Core" to "com.google.firebase:firebase-core:17.0.0",
        "Auth" to "com.google.firebase:firebase-auth:18.0.0",
        "gmsauth" to "com.google.android.gms:play-services-auth:17.0.0",
        "Firestore" to "com.google.firebase:firebase-firestore:20.0.0",
        "Crashlytics" to "com.crashlytics.sdk.android:crashlytics:2.10.1",
        "Messaging" to "com.google.firebase:firebase-messaging:19.0.0",
        "Performance" to "com.google.firebase:firebase-perf:18.0.0",
        "Invites" to "com.google.firebase:firebase-invites:17.0.0"
    )

    val RxJava = mapOf(
        "Core" to "io.reactivex.rxjava2:rxjava:2.2.9",
        "Kotlin" to "io.reactivex.rxjava2:rxkotlin:2.3.0",
        "Relay" to "com.jakewharton.rxrelay2:rxrelay:2.1.0",
        "Android" to "io.reactivex.rxjava2:rxandroid:2.1.1"
    )

    val RxBinding = mapOf(
        "Binding" to "com.jakewharton.rxbinding3:rxbinding:${Versions.RxBinding}",
        "Core" to "com.jakewharton.rxbinding3:rxbinding-core:${Versions.RxBinding}",
        "Appcompat" to "com.jakewharton.rxbinding3:rxbinding-appcompat:${Versions.RxBinding}",
        "Drawerlayout" to "com.jakewharton.rxbinding3:rxbinding-drawerlayout:${Versions.RxBinding}",
        "Recyclerview" to "com.jakewharton.rxbinding3:rxbinding-recyclerview:${Versions.RxBinding}",
        "Swiperefreshlayout" to "com.jakewharton.rxbinding3:rxbinding-swiperefreshlayout:${Versions.RxBinding}",
        "Viewpager" to "com.jakewharton.rxbinding3:rxbinding-viewpager:${Versions.RxBinding}",
        "Material" to "com.jakewharton.rxbinding3:rxbinding-material:${Versions.RxBinding}"
    )

    val MiscUtil = mapOf(
        "Gson" to "com.google.code.gson:gson:2.8.5",
        "Timber" to "com.jakewharton.timber:timber:4.7.1",
        "Threeten" to "com.jakewharton.threetenabp:threetenabp:1.2.1",
        "Flick" to "me.saket:flick:1.4.0",
        "Gestureviews" to "com.alexvasilkov:gesture-views:2.5.2",
        "Aboutlibs" to "com.mikepenz:aboutlibraries:6.2.1", //do not update this.. causes color errors?
        "Customtabs" to "com.android.support:customtabs:28.0.0"
    )

    val LeakCanary = mapOf(
        "Op" to "com.squareup.leakcanary:leakcanary-android:${Versions.Leakcanary}",
        "NoOp" to "com.squareup.leakcanary:leakcanary-android-no-op:${Versions.Leakcanary}"
    )

    val Glide = mapOf(
        "Core" to "com.github.bumptech.glide:glide:${Versions.Glide}",
        "Compiler" to "com.github.bumptech.glide:compiler:${Versions.Glide}",
        "Transformation" to "jp.wasabeef:glide-transformations:4.0.1"
    )

    val Work = mapOf(
        "Core" to "androidx.work:work-runtime:${Versions.Work}",
        "RxJava" to "androidx.work:work-rxjava2:${Versions.Work}"
    )
}