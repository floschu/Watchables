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
import at.florianschuster.watchables.util.GlideApp
import io.reactivex.Completable
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import at.florianschuster.watchables.BuildConfig
import at.florianschuster.watchables.R
import at.florianschuster.watchables.model.Watchable
import at.florianschuster.watchables.model.original
import at.florianschuster.watchables.model.thumbnail
import at.florianschuster.watchables.util.extensions.subscribeOnIO
import io.reactivex.Single
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ShareService(private val activity: AppCompatActivity) {

    private val cachedName = "shareimage.jpg"
    private val tempFile: File by lazy { File(activity.cacheDir, cachedName) }

    fun share(watchable: Watchable): Completable = Single.fromCallable {
        when (watchable.type) {
            Watchable.Type.show -> activity.getString(R.string.share_text_show, watchable.name)
            Watchable.Type.movie -> activity.getString(R.string.share_text_movie, watchable.name)
        }
    }.flatMapCompletable { share(it, watchable.original) }

    fun shareApp(): Completable = Completable.fromAction {
        val text = activity.getString(R.string.share_watchables_app)
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }.let {
            Intent.createChooser(it, activity.getString(R.string.settings_share_app))
        }.also(activity::startActivity)
    }

    private fun share(text: String, imageUrl: String?): Completable = Single
            .fromCallable { GlideApp.with(activity).asBitmap().load(imageUrl).submit().get() }
            .flatMap(::cacheBitmapForShare)
            .flatMap(::contentUriFromFile)
            .subscribeOnIO()
            .doOnSuccess {
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    setDataAndType(it, activity.contentResolver.getType(it))
                    putExtra(Intent.EXTRA_STREAM, it)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }.let {
                    Intent.createChooser(it, text)
                }.also(activity::startActivity)
            }
            .ignoreElement()

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

}