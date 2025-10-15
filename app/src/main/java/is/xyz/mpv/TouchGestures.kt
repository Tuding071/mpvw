package is.xyz.mpv

import android.view.GestureDetector
import android.view.MotionEvent

class TouchGestures(private val activity: MPVActivity) : GestureDetector.SimpleOnGestureListener() {

    private val gestureDetector: GestureDetector = GestureDetector(activity, this)

    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        activity.toggleControls()
        return true
    }

    // All other gestures are disabled by not overriding their methods
}
