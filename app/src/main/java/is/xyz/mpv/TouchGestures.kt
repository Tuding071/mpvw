package is.xyz.mpv

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import is.xyz.mpv.MPVView
import is.xyz.mpv.MPVLib

class TouchGestures(
    private val context: Context,
    private val parent: MPVView
) : GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {

    private val gestureDetector = GestureDetector(context, this)
    private val scaleGestureDetector = ScaleGestureDetector(context, this)

    // Placeholder for your custom gesture handling
    private fun customGestureHandler(event: MotionEvent): Boolean {
        // Add your custom seeking logic here
        // Example: Custom seeking based on horizontal drag
        /*
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previousX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - previousX
                if (Math.abs(deltaX) > 10f) { // Threshold to avoid noise
                    val seekDelta = (deltaX / parent.width) * 50f // Custom seek factor
                    MPVLib.command(arrayOf("seek", seekDelta.toString(), "relative"))
                    previousX = event.x
                    return true
                }
            }
        }
        */
        return false
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true // Required to receive further events
    }

    override fun onShowPress(e: MotionEvent) {
        // No default action
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        // Optional: Keep for UI toggle or customize
        parent.performClick()
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        // Removed default seeking, volume, brightness
        return customGestureHandler(e2)
    }

    override fun onLongPress(e: MotionEvent) {
        // No default action
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        // Removed default fling handling
        return customGestureHandler(e2)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        // Optional: Keep for UI toggle or customize
        parent.performClick()
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        // Optional: Keep for play/pause or customize
        MPVLib.command(arrayOf("cycle", "pause"))
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        // Removed default zoom; add custom if needed
        return false
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return false
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        // No return type needed in Kotlin for Unit
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return customGestureHandler(event) || true
    }
}
