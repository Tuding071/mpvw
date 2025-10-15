package is.xyz.mpv

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.OverScroller
import kotlin.math.abs

class TouchGestures(private val context: Context, private val parent: MPVView) :
    GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {

    private val gestureDetector = GestureDetector(context, this)
    private val scaleGestureDetector = ScaleGestureDetector(context, this)

    private var scrollY = 0f
    private var scrollX = 0f
    private var isScaling = false
    private val scroller = OverScroller(context)

    override fun onDown(e: MotionEvent): Boolean {
        scroller.forceFinished(true)
        return true
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        // Removed all swiping/dragging handling (horizontal seeking and vertical volume/brightness)
        // Original logic commented out below for reference:
        /*
        scrollX -= distanceX
        scrollY -= distanceY
        val absX = abs(scrollX)
        val absY = abs(scrollY)
        if (absX > absY) {
            // Horizontal scroll - seeking
            val delta = scrollX / parent.width.toFloat() * 100f // percentage
            MPVLib.command(arrayOf("seek", delta.toString(), "relative-percent"))
            scrollX = 0f // reset for next
            return true
        } else if (absY > absX) {
            // Vertical scroll
            val halfWidth = parent.width / 2
            if (e2.x < halfWidth) {
                // Left - brightness
                val brightnessDelta = -distanceY / parent.height.toFloat() * 100f
                Utils.adjustBrightness(context, brightnessDelta)
            } else {
                // Right - volume
                val volumeDelta = -distanceY / parent.height.toFloat() * 100f
                Utils.adjustVolume(context, volumeDelta)
            }
            return true
        }
        */
        return false
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        // Removed all fling handling (e.g., horizontal large seeks)
        // Original logic commented out below for reference:
        /*
        if (abs(velocityX) > abs(velocityY)) {
            // Horizontal fling - larger seek
            val delta = velocityX / 1000f * 10 // example
            MPVLib.command(arrayOf("seek", delta.toString()))
            return true
        }
        */
        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        parent.performClick()
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        MPVLib.command(arrayOf("cycle", "pause"))
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        isScaling = true
        val scale = detector.scaleFactor
        MPVLib.setPropertyString("video-zoom", (MPVLib.getPropertyDouble("video-zoom") + Math.log(scale.toDouble())).toString())
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector): Boolean {
        isScaling = false
        return true
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP) {
            scrollX = 0f
            scrollY = 0f
        }
        return true
    }
}
