@Suppress("SpellCheckingInspection", "unused")
object Dependencies {
    const val KotlinStd = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.Kotlin}"
    const val Reaktor = "at.florianschuster.reaktor:reaktor-android-koin:${Versions.Reaktor}"
    const val Koordinator = "at.florianschuster.koordinator:koordinator-android-koin:${Versions.Koordinator}"

    const val AndroidUtilUI = "com.tailoredapps.androidutil:util-ui:${Versions.AndroidAppUtil}"
    const val AndroidUtilAsync = "com.tailoredapps.androidutil:util-async:${Versions.AndroidAppUtil}"
    const val AndroidUtilNetwork = "com.tailoredapps.androidutil:util-network:${Versions.AndroidAppUtil}"
    const val AndroidUtilOptional = "com.tailoredapps.androidutil:util-optional:${Versions.AndroidAppUtil}"
    const val AndroidUtilPermissions = "com.tailoredapps.androidutil:util-permissions:${Versions.AndroidAppUtil}"
    const val AndroidUtilValidation = "com.tailoredapps.androidutil:util-validation:${Versions.AndroidAppUtil}"
    const val AndroidUtilViewState = "com.tailoredapps.androidutil:util-viewstate:${Versions.AndroidAppUtil}"
    const val AndroidUtilFirebase = "com.tailoredapps.androidutil:util-firebase:${Versions.AndroidAppUtil}"

    const val AppCompat = "androidx.appcompat:appcompat:1.1.0-rc01"
    const val Material = "com.google.android.material:material:1.0.0"
    const val Fragment = "androidx.fragment:fragment:1.0.0"
    const val Constraintlayout = "androidx.constraintlayout:constraintlayout:1.1.3"
    const val CoreKTX = "androidx.core:core-ktx:1.0.1"

    const val NavigationUI = "androidx.navigation:navigation-ui-ktx:${Versions.Navigation}"
    const val NavigationFragment = "androidx.navigation:navigation-fragment-ktx:${Versions.Navigation}"

    const val LifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:${Versions.Lifecycle}"
    const val LifecycleCompiler = "androidx.lifecycle:lifecycle-compiler:${Versions.Lifecycle}"

    const val KoinCore = "org.koin:koin-core:${Versions.Koin}"
    const val KoinAndroid = "org.koin:koin-android:${Versions.Koin}"
    const val KoinAndroidScope = "org.koin:koin-androidx-scope:${Versions.Koin}"
    const val KoinAndroidViewModel = "org.koin:koin-androidx-viewmodel:${Versions.Koin}"

    const val ZxingCore = "com.google.zxing:core:3.4.0"
    const val ZxingAndroid = "com.journeyapps:zxing-android-embedded:3.6.0@aar"

    const val Retrofit = "com.squareup.retrofit2:retrofit:${Versions.Retrofit}"
    const val RetrofitGsonConverter = "com.squareup.retrofit2:converter-gson:${Versions.Retrofit}"
    const val RetrofitRxJava2Adapter = "com.squareup.retrofit2:adapter-rxjava2:${Versions.Retrofit}"
    const val OkHttp = "com.squareup.okhttp3:okhttp:${Versions.OkHttp}"
    const val OkHttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.OkHttp}"

    const val Hawk = "com.orhanobut:hawk:2.0.1"

    const val FirebaseCore = "com.google.firebase:firebase-core:17.0.1"
    const val FirebaseAuth = "com.google.firebase:firebase-auth:18.1.0"
    const val PlayServicesAuth = "com.google.android.gms:play-services-auth:17.0.0"
    const val FirebaseFirestore = "com.google.firebase:firebase-firestore:20.2.0"
    const val FirebaseCrashlytics = "com.crashlytics.sdk.android:crashlytics:2.10.1"
    const val FirebaseMessaging = "com.google.firebase:firebase-messaging:19.0.1"
    const val FirebasePrformance = "com.google.firebase:firebase-perf:18.0.1"
    const val FirebaseInvites = "com.google.firebase:firebase-invites:17.0.0"

    const val RxJava2 = "io.reactivex.rxjava2:rxjava:2.2.11"
    const val RxJava2Kotlin = "io.reactivex.rxjava2:rxkotlin:2.4.0"
    const val RxJava2Relay = "com.jakewharton.rxrelay2:rxrelay:2.1.0"
    const val RxJava2Android = "io.reactivex.rxjava2:rxandroid:2.1.1"

    const val RxBinding = "com.jakewharton.rxbinding3:rxbinding:${Versions.RxBinding}"
    const val RxBindingCore = "com.jakewharton.rxbinding3:rxbinding-core:${Versions.RxBinding}"
    const val RxBindingAppcompat = "com.jakewharton.rxbinding3:rxbinding-appcompat:${Versions.RxBinding}"
    const val RxBindingRecyclerview = "com.jakewharton.rxbinding3:rxbinding-recyclerview:${Versions.RxBinding}"
    const val RxBindingMaterial = "com.jakewharton.rxbinding3:rxbinding-material:${Versions.RxBinding}"

    const val Gson = "com.google.code.gson:gson:2.8.5"
    const val Timber = "com.jakewharton.timber:timber:4.7.1"
    const val Threeten = "com.jakewharton.threetenabp:threetenabp:1.2.1"
    const val Flick = "me.saket:flick:1.4.0"
    const val Gestureviews = "com.alexvasilkov:gesture-views:2.5.2"
    const val Aboutlibs = "com.mikepenz:aboutlibraries:6.2.1" //do not update this.. causes color errors?
    const val Customtabs = "com.android.support:customtabs:28.0.0"
    const val RxPermissions = "com.github.tbruyelle:rxpermissions:0.10.2"
    const val Shimmer = "com.facebook.shimmer:shimmer:0.5.0"

    const val LeakCanary = "com.squareup.leakcanary:leakcanary-android:${Versions.Leakcanary}"
    const val LeakCanaryNoOp = "com.squareup.leakcanary:leakcanary-android-no-op:${Versions.Leakcanary}"

    const val Glide = "com.github.bumptech.glide:glide:${Versions.Glide}"
    const val GlideCompiler = "com.github.bumptech.glide:compiler:${Versions.Glide}"
    const val GlideTransformation = "jp.wasabeef:glide-transformations:4.0.1"

    const val Worker = "androidx.work:work-runtime:${Versions.Work}"
    const val WorkerRxJava2 = "androidx.work:work-rxjava2:${Versions.Work}"

    val Test = mapOf( // todo
        "Junit" to ("junit:junit:4.12"),
        "Mockito" to ("org.mockito:mockito-core:${Versions.Mockito}"),
        "MockitoKotlin" to ("com.nhaarman:mockito-kotlin-kt1.1:1.5.0"),
        "Powermock" to ("org.powermock:powermock-module-junit4:${Versions.Powermock}"),
        "PowermockRule" to ("org.powermock:powermock-module-junit4-rule:${Versions.Powermock}"),
        "PowermockMockito2" to ("org.powermock:powermock-api-mockito2:${Versions.Powermock}"),
        "KotlinReflect" to ("org.jetbrains.kotlin:kotlin-reflect:${Versions.Kotlin}"),
        "Jsr305" to ("com.google.code.findbugs:jsr305:3.0.2"),
        "Mockk" to ("io.mockk:mockk:1.9.3"),
        "Kluent" to ("org.amshove.kluent:kluent:1.51"),
        "KoinTest" to ("org.koin:koin-test:${Versions.Koin}")
    )

    val AndroidTest = mapOf(
        "Core" to ("androidx.test:core:${Versions.AndroidXTest}"),
        "Runner" to ("androidx.test:runner:${Versions.AndroidXTest}"),
        "Rules" to ("androidx.test:rules:${Versions.AndroidXTest}"),
        "AndroidXTruth" to ("androidx.test.ext:truth:${Versions.AndroidXTest}"),
        "AndroidXJunit" to ("androidx.test.ext:junit:1.1.0"),
        "Truth" to ("com.google.truth:truth:1.0"),
        "Espresso" to ("androidx.test.espresso:espresso-core:${Versions.Espresso}"),
        "EspressoContrib" to ("androidx.test.espresso:espresso-contrib:${Versions.Espresso}"),
        "EspressoIntents" to ("androidx.test.espresso:espresso-intents:${Versions.Espresso}"),
        "EspressoWeb" to ("androidx.test.espresso:espresso-web:${Versions.Espresso}"),
        "Mockito" to ("org.mockito:mockito-android:${Versions.Mockito}")
    )
}