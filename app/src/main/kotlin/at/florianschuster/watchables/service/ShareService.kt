/*
 * Copyright 2019 Florian Schuster. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.florianschuster.watchables.service

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import at.florianschuster.watchables.BuildConfig
import at.florianschuster.watchables.R
import at.florianschuster.watchables.all.util.GlideApp
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.originalPoster
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

interface ShareService {
    fun share(watchable: Watchable): Completable
    fun shareApp(): Completable
}

class ActivityShareService(
    private val activity: AppCompatActivity,
    private val deepLinkService: DeepLinkService
) : ShareService {
    private val resources = activity.resources

    private val tempFile: File by lazy { File(activity.cacheDir, "shareimage.jpg") }

    override fun share(watchable: Watchable): Completable = downloadImageToShare(watchable.originalPoster)
        .flatMap { poster ->
            deepLinkService.createDeepLinkUrl(watchable)
                .map { poster to it }
                .toSingle(poster to "")
        }
        .flatMap { (poster, deepLink) ->
            val chooserText = when (watchable.type) {
                Watchable.Type.show -> {
                    resources.getString(R.string.share_watchable_chooser_text_show, watchable.name, deepLink)
                }
                Watchable.Type.movie -> {
                    resources.getString(R.string.share_watchable_chooser_text_movie, watchable.name, deepLink)
                }
            }
            chooserIntent(resources.getString(R.string.share_watchable_chooser_title, watchable.name), chooserText, poster)
        }
        .doOnSuccess(activity::startActivity)
        .ignoreElement()

    override fun shareApp(): Completable = chooserIntent(
        resources.getString(R.string.share_app_chooser_title),
        resources.getString(R.string.share_app_chooser_text, DeepLinkService.Link.App.link),
        null
    ).doOnSuccess(activity::startActivity).ignoreElement()

    private fun downloadImageToShare(imageUrl: String?): Single<Uri> = Single
        .fromCallable { GlideApp.with(activity).asBitmap().load(imageUrl).submit().get() }
        .flatMap(::cacheBitmapForShare)
        .flatMap(::contentUriFromFile)
        .subscribeOn(Schedulers.io())

    private fun cacheBitmapForShare(bitmap: Bitmap): Single<File> = Single.create { emitter ->
        FileOutputStream(tempFile).use {
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            if (success) emitter.onSuccess(tempFile)
            else emitter.onError(IOException())
        }
    }

    private fun contentUriFromFile(file: File): Single<Uri> = Single.fromCallable {
        FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".file_provider", file)
    }

    private fun chooserIntent(chooserTitle: String, intentText: String, imageUri: Uri?): Single<Intent> =
        Single.fromCallable {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, intentText)
                if (imageUri != null) {
                    type = activity.contentResolver.getType(imageUri)
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
            }
        }.map { Intent.createChooser(it, chooserTitle) }
}
