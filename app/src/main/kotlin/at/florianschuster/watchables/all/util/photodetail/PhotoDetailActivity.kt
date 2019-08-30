package at.florianschuster.watchables.all.util.photodetail

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import at.florianschuster.watchables.R
import at.florianschuster.watchables.all.util.GlideApp
import at.florianschuster.watchables.all.util.photodetail.immersive.SystemUiHelper
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.tailoredapps.androidutil.ui.extensions.extra
import com.tailoredapps.androidutil.ui.extensions.extras
import io.reactivex.functions.Consumer
import java.io.File
import java.security.MessageDigest
import kotlinx.android.synthetic.main.activity_photo_detail.*
import me.saket.flick.ContentSizeProvider
import me.saket.flick.FlickCallbacks
import me.saket.flick.FlickGestureListener
import me.saket.flick.InterceptResult

private const val ARG_PHOTO_URL = "photo.url"
private const val ARG_FILE = "file"

val Context.photoDetailConsumer: Consumer<String>
    get() = Consumer {
        Intent(this, PhotoDetailActivity::class.java)
            .extras(ARG_PHOTO_URL to it)
            .let(::startActivity)
    }

val Context.filePhotoDetailConsumer: Consumer<File>
    get() = Consumer { file ->
        Intent(this, PhotoDetailActivity::class.java)
            .extras(ARG_FILE to file)
            .let(::startActivity)
    }

// https://github.com/saket/Flick/
class PhotoDetailActivity : AppCompatActivity() {
    private val url: String? by extra(ARG_PHOTO_URL)
    private val file: File? by extra(ARG_FILE)

    private lateinit var systemUiHelper: SystemUiHelper
    private lateinit var activityBackgroundDrawable: Drawable

    private val target by lazy { TargetWithEntryAnimation(imageView, progress) }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        super.onCreate(savedInstanceState)

        if (url == null && file == null) {
            finish()
            return
        }

        overridePendingTransition(0, 0)
        setContentView(R.layout.activity_photo_detail)

        animateDimmingOnEntry()

        GlideApp.with(this)
            .asBitmap()
            .apply {
                if (url != null) load(url)
                else if (file != null) load(file)
            }
            .transform(PaddingTransformation(1F, Color.TRANSPARENT))
            .priority(Priority.IMMEDIATE)
            .into(target)

        flickDismissLayout.gestureListener = flickGestureListener()

        systemUiHelper = SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE, 0, null)
        imageView.setOnClickListener { systemUiHelper.toggle() }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onBackPressed() {
        animateExit { super.onBackPressed() }
    }

    private fun finishInMillis(millis: Long) {
        rootLayout.postDelayed({ finish() }, millis)
    }

    private fun flickGestureListener(): FlickGestureListener {
        val contentHeightProvider = object : ContentSizeProvider {
            override fun heightForDismissAnimation(): Int {
                return imageView.zoomedImageHeight.toInt()
            }

            // A positive height value is important so that the user
            // can dismiss even while the progress indicator is visible.
            override fun heightForCalculatingDismissThreshold(): Int {
                return when {
                    imageView.drawable == null -> 240 * (resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
                    else -> imageView.visibleZoomedImageHeight.toInt()
                }
            }
        }

        val callbacks = object : FlickCallbacks {
            override fun onFlickDismiss(flickAnimationDuration: Long) {
                finishInMillis(flickAnimationDuration)
            }

            override fun onMove(@FloatRange(from = -1.0, to = 1.0) moveRatio: Float) {
                updateBackgroundDimmingAlpha(Math.abs(moveRatio))
            }
        }

        val gestureListener = FlickGestureListener(this, contentHeightProvider, callbacks)

        // Block flick gestures if the thumbnailImage can pan further.
        gestureListener.gestureInterceptor = { scrollY ->
            val isScrollingUpwards = scrollY < 0
            val directionInt = if (isScrollingUpwards) -1 else +1
            val canPanFurther = imageView.canScrollVertically(directionInt)

            when {
                canPanFurther -> InterceptResult.INTERCEPTED
                else -> InterceptResult.IGNORED
            }
        }

        return gestureListener
    }

    private fun animateDimmingOnEntry() {
        activityBackgroundDrawable = rootLayout.background.mutate()
        rootLayout.background = activityBackgroundDrawable

        ObjectAnimator.ofFloat(1F, 0f).apply {
            duration = 200
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { updateBackgroundDimmingAlpha(it.animatedValue as Float) }
            start()
        }
    }

    private fun animateExit(onEndAction: () -> Unit) {
        val animDuration: Long = 200
        flickDismissLayout.animate()
            .alpha(0f)
            .translationY(flickDismissLayout.height / 20F)
            .rotation(-2F)
            .setDuration(animDuration)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction(onEndAction)
            .start()

        ObjectAnimator.ofFloat(0F, 1F).apply {
            duration = animDuration
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animation ->
                updateBackgroundDimmingAlpha(animation.animatedValue as Float)
            }
            start()
        }
    }

    private fun updateBackgroundDimmingAlpha(@FloatRange(from = 0.0, to = 1.0) transparencyFactor: Float) {
        // Increase dimming exponentially so that the background is
        // fully transparent while the thumbnailImage has been moved by half.
        val dimming = 1f - Math.min(1f, transparencyFactor * 2)
        activityBackgroundDrawable.alpha = (dimming * 255).toInt()
    }
}

class TargetWithEntryAnimation(private val imageView: ImageView, private val progress: ProgressBar) : CustomViewTarget<ImageView, Bitmap>(imageView) {
    override fun onLoadFailed(errorDrawable: Drawable?) {
        progress.isVisible = false
    }

    override fun onResourceCleared(placeholder: Drawable?) {}

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        progress.isVisible = false
        imageView.apply {
            alpha = 0F
            translationY = resource.height / 20F
            rotation = -2F
        }

        imageView.setImageBitmap(resource)

        imageView.animate()
            .alpha(1F)
            .translationY(0F)
            .rotation(0F)
            .setInterpolator(FlickGestureListener.ANIM_INTERPOLATOR)
            .start()
    }
}

class PaddingTransformation(private val paddingPx: Float, @ColorInt private val paddingColor: Int) : BitmapTransformation() {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("padding_$paddingPx".toByteArray())
    }

    override fun transform(pool: BitmapPool, source: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        if (paddingPx == 0F) return source

        val targetWidth = source.width + paddingPx * 2F
        val targetHeight = source.height + paddingPx * 2F

        val bitmapWithPadding = Bitmap.createBitmap(targetWidth.toInt(), targetHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapWithPadding)

        val paint = Paint()
        paint.color = paddingColor
        canvas.drawRect(0F, 0F, targetWidth, targetHeight, paint)
        canvas.drawBitmap(source, paddingPx, paddingPx, null)

        return bitmapWithPadding
    }
}
