package at.florianschuster.watchables.util.photodetail

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.State
import com.alexvasilkov.gestures.views.GestureImageView

private const val MAX_OVER_ZOOM = 4F

/**
 * https://github.com/saket/Flick/
 *
 * FYI: GestureImageView does not support a foreground ripple, because it intercepts
 * all touch events for handling scale and pan.
 */
class ZoomableGestureImageView(context: Context, attrs: AttributeSet) : GestureImageView(context, attrs) {

    private val gestureDetector: GestureDetector
    private val imageMovementRect = RectF()

    /**
     * Height of the thumbnailImage that is currently visible within this View's bounds.
     */
    // Subtract the portion that has gone outside display bounds due to zooming in, because they are longer visible.
    val visibleZoomedImageHeight: Float
        get() {
            var zoomedImageHeight = zoomedImageHeight
            val heightNotVisible = controller.state.y
            if (heightNotVisible < 0) {
                zoomedImageHeight += heightNotVisible
            }

            if (zoomedImageHeight > height) {
                zoomedImageHeight = height.toFloat()
            }

            return zoomedImageHeight
        }

    val zoomedImageHeight: Float
        get() {
            val zoom = controller.state.zoom
            return controller.settings.imageH.toFloat() * zoom
        }

    init {
        controller.settings.overzoomFactor = MAX_OVER_ZOOM
        controller.settings.isFillViewport = true
        controller.settings.fitMethod = Settings.Fit.HORIZONTAL

        controller.setOnGesturesListener(object : GestureController.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                // GestureImageView doesn't support a click
                // listener because it intercepts all touch events.
                performClick()
                return true
            }

            override fun onUpOrCancel(event: MotionEvent) {
                // Bug workaround: Image zoom stops working after first over-zoom.
                // Resetting it when the finger is lifted seems to solve the problem.
                controller.settings.overzoomFactor = MAX_OVER_ZOOM
            }
        })

        // Bug workarounds: GestureImageView doesn't request parent ViewGroups
        // to stop intercepting touch events when it starts consuming them to zoom.
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                parent.requestDisallowInterceptTouchEvent(true)
                return super.onDoubleTapEvent(e)
            }
        })
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Reject all touch events until the thumbnailImage is available.
        return drawable != null && super.dispatchTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN && event.pointerCount == 2) {
            // Two-finger zoom is probably going to start.
            parent.requestDisallowInterceptTouchEvent(true)
        }

        return super.onTouchEvent(event)
    }

    /**
     * Check if the thumbnailImage can be panned anymore vertically. FYI, downwardPan == upwards scroll.
     */
    override fun canScrollVertically(direction: Int): Boolean {
        val downwardPan = direction < 0

        val state = controller.state
        controller.stateController.getMovementArea(state, imageMovementRect)

        return (downwardPan.not() && State.compare(state.y, imageMovementRect.bottom) < 0f
                || downwardPan && State.compare(state.y, imageMovementRect.top) > 0f)
    }
}