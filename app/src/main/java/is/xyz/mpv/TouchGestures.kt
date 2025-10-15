package is.xyz.mpv

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class TouchGestures(private val context: Context, private val parent: MPVView) :
    GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {

    private val gestureDetector = GestureDetector(context, this)
    private val scaleGestureDetector = ScaleGestureDetector(context, this)

    // Placeholder for your custom gesture handling
    private fun customGestureHandler(event: MotionEvent): Boolean {
        // Add your custom logic here, e.g., for seeking
        // Example: Check for horizontal drag
        /*
        if (event.action == MotionEvent.ACTION_MOVE) {
            val deltaX = event.x - previousX  // Track previousX in your logic
            if (Math.abs(deltaX) > someThreshold) {
                val seekDelta = deltaX / parent.width * customSeekFactor
                MPVLib.command(arrayOf("seek", seekDelta.toString()))
                return true
            }
        }
        */
        return false
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true // Required to receive further events
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        // Removed default seeking, volume, brightness
        // Call custom handler if desired
        return customGestureHandler(e2)
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        // Removed default fling handling
        // Call custom handler if desired
        return customGestureHandler(e2)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        // Optional: Keep tap for UI toggle or customize
        parent.performClick()
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        // Optional: Keep double-tap for play/pause or customize
        MPVLib.command(arrayOf("cycle", "pause"))
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        // Removed default zoom; add custom if needed
        return false
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return false
    }

    override fun onScaleEnd(detector: ScaleGestureDetector): Boolean {
        return false
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        // Route touch events to detectors and custom handler
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return customGestureHandler(event) || true
    }
}
