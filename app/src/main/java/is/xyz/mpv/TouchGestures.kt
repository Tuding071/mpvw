package is.xyz.mpv

import android.content.SharedPreferences import android.content.res.Resources import android.graphics.PointF import android.os.SystemClock import android.util.Log import android.view.MotionEvent import kotlin.math.*

enum class PropertyChange { Init, Seek, Volume, Bright, Finalize,

/* Tap gestures */
SeekFixed,
PlayPause,
Custom,

}

internal interface TouchGesturesObserver { fun onPropertyChange(p: PropertyChange, diff: Float) }

internal class TouchGestures(private val observer: TouchGesturesObserver) {

private enum class State {
    Up,
    Down,
    ControlSeek,
    ControlVolume,
    ControlBright,
    ControlFrameSeek, // NEW: frame-seeking state
}

private var state = State.Up
private var stateDirection = 0

private var lastTapTime = 0L
private var lastDownTime = 0L

private var initialPos = PointF()
private var lastPos = PointF()

private var width = 0f
private var height = 0f
private var trigger = 0f

// Custom center-area flag
private var isCustomCenterTouch = false

private var gestureHoriz = State.Down
private var gestureVertLeft = State.Down
private var gestureVertRight = State.Down
private var tapGestureLeft : PropertyChange? = null
private var tapGestureCenter : PropertyChange? = null
private var tapGestureRight : PropertyChange? = null

// --- Frame seeking variables ---
private var lastFrameSeekX = 0f
private var isFrameSeeking = false

// Pixels per frame step (adjustable)
private val FRAME_SEEK_STEP = 12f // <-- EDITABLE: change swipe sensitivity here

// Frames to skip per step (adjustable)
private val FRAMES_PER_STEP = 1    // <-- EDITABLE: change how many frames per swipe increment

private inline fun checkFloat(vararg n: Float): Boolean {
    return !n.any { it.isInfinite() || it.isNaN() }
}
private inline fun assertFloat(vararg n: Float) {
    if (!checkFloat(*n))
        throw IllegalArgumentException()
}

fun setMetrics(width: Float, height: Float) {
    assertFloat(width, height)
    this.width = width
    this.height = height
    trigger = min(width, height) / TRIGGER_RATE
}

companion object {
    private const val TAG = "mpv"
    private const val TRIGGER_RATE = 30
    private const val TAP_DURATION = 300L
    private const val CONTROL_SEEK_MAX = 150f
    private const val CONTROL_VOLUME_MAX = 1.5f
    private const val CONTROL_BRIGHT_MAX = 1.5f
    private const val DEADZONE = 5
    private const val CUSTOM_CENTER_TOP_PERCENT = 5f
    private const val CUSTOM_CENTER_BOTTOM_PERCENT = 75f
}

private fun processTap(p: PointF): Boolean {
    if (state == State.Up) {
        lastDownTime = SystemClock.uptimeMillis()
        if (PointF(lastPos.x - p.x, lastPos.y - p.y).length() > trigger * 3)
            lastTapTime = 0
        return true
    }
    if (state != State.Down)
        return false

    val now = SystemClock.uptimeMillis()
    if (now - lastDownTime >= TAP_DURATION) {
        lastTapTime = 0
        return false
    }
    if (now - lastTapTime < TAP_DURATION) {
        if (p.x <= width * 0.28f)
            tapGestureLeft?.let { sendPropertyChange(it, -1f); return true }
        else if (p.x >= width * 0.72f)
            tapGestureRight?.let { sendPropertyChange(it, 1f); return true }
        else
            tapGestureCenter?.let { sendPropertyChange(it, 0f); return true }
        lastTapTime = 0
    } else {
        lastTapTime = now
    }
    return false
}

private fun processMovement(p: PointF): Boolean {
    if (PointF(lastPos.x - p.x, lastPos.y - p.y).length() < trigger / 3)
        return false
    lastPos.set(p)

    val dx = p.x - initialPos.x
    val dy = p.y - initialPos.y
    val dr = if (stateDirection == 0) (dx / width) else (-dy / height)

    when (state) {
        State.Up -> {}
        State.Down -> {
            if (abs(dx) > trigger) {
                state = gestureHoriz
                stateDirection = 0
            } else if (abs(dy) > trigger) {
                state = if (initialPos.x > width / 2) gestureVertRight else gestureVertLeft
                stateDirection = 1
            }
            if (state != State.Down)
                sendPropertyChange(PropertyChange.Init, 0f)
        }
        State.ControlSeek ->
            sendPropertyChange(PropertyChange.Seek, CONTROL_SEEK_MAX * dr)
        State.ControlVolume ->
            sendPropertyChange(PropertyChange.Volume, CONTROL_VOLUME_MAX * dr)
        State.ControlBright ->
            sendPropertyChange(PropertyChange.Bright, CONTROL_BRIGHT_MAX * dr)
        else -> {}
    }
    return state != State.Up && state != State.Down
}

private fun sendPropertyChange(p: PropertyChange, diff: Float) {
    observer.onPropertyChange(p, diff)
}

fun syncSettings(prefs: SharedPreferences, resources: Resources) {
    val get: (String, Int) -> String = { key, defaultRes ->
        val v = prefs.getString(key, "")
        if (v.isNullOrEmpty()) resources.getString(defaultRes) else v
    }
    val map = mapOf(
        "bright" to State.ControlBright,
        "seek" to State.ControlSeek,
        "volume" to State.ControlVolume
    )
    val map2 = mapOf(
        "seek" to PropertyChange.SeekFixed,
        "playpause" to PropertyChange.PlayPause,
        "custom" to PropertyChange.Custom
    )

    gestureHoriz = map[get("gesture_horiz", R.string.pref_gesture_horiz_default)] ?: State.Down
    gestureVertLeft = map[get("gesture_vert_left", R.string.pref_gesture_vert_left_default)] ?: State.Down
    gestureVertRight = map[get("gesture_vert_right", R.string.pref_gesture_vert_right_default)] ?: State.Down
    tapGestureLeft = map2[get("gesture_tap_left", R.string.pref_gesture_tap_left_default)]
    tapGestureCenter = map2[get("gesture_tap_center", R.string.pref_gesture_tap_center_default)]
    tapGestureRight = map2[get("gesture_tap_right", R.string.pref_gesture_tap_right_default)]
}

fun onTouchEvent(e: MotionEvent): Boolean {
    if (width < 1 || height < 1) return false
    if (!checkFloat(e.x, e.y)) return false

    var gestureHandled = false
    val point = PointF(e.x, e.y)

    when (e.action) {
        MotionEvent.ACTION_DOWN -> {
            val customCenterTopY = height * CUSTOM_CENTER_TOP_PERCENT / 100f
            val customCenterBottomY = height * CUSTOM_CENTER_BOTTOM_PERCENT / 100f
            isCustomCenterTouch = e.y in customCenterTopY..customCenterBottomY
            if (!isCustomCenterTouch) return false

            initialPos.set(point)
            lastPos.set(point)
            state = State.Down
            gestureHandled = true
        }
        MotionEvent.ACTION_MOVE -> {
            if (isCustomCenterTouch) {
                val dx = point.x - initialPos.x

                // --- FRAME SEEKING LOGIC ---
                if (!isFrameSeeking && abs(dx) > trigger) {
                    state = State.ControlFrameSeek
                    isFrameSeeking = true
                    lastFrameSeekX = initialPos.x
                    sendPropertyChange(PropertyChange.Init, 0f)
                    MPVLib.setPropertyBoolean("pause", true) // pause during frame seek
                }

                if (isFrameSeeking && state == State.ControlFrameSeek) {
                    val deltaX = point.x - lastFrameSeekX
                    val steps = (deltaX / FRAME_SEEK_STEP).toInt()
                    if (steps != 0) {
                        MPVLib.command(arrayOf("frame-step", (steps * FRAMES_PER_STEP).toString())) // <-- frame skip adjustable
                        lastFrameSeekX += steps * FRAME_SEEK_STEP
                    }
                }

                lastPos.set(point)
                gestureHandled = true
                // --- END FRAME SEEKING ---

            } else {
                gestureHandled = processMovement(point)
            }
        }
        MotionEvent.ACTION_UP -> {
            if (isFrameSeeking) {
                isFrameSeeking = false
                MPVLib.setPropertyBoolean("pause", false) // resume playback
                sendPropertyChange(PropertyChange.Finalize, 0f)
                gestureHandled = true
            } else if (isCustomCenterTouch && state == State.Down) {
                sendPropertyChange(PropertyChange.PlayPause, 0f)
                lastTapTime = SystemClock.uptimeMillis()
                gestureHandled = true
            } else {
                gestureHandled = processMovement(point) or processTap(point)
            }
            state = State.Up
            isCustomCenterTouch = false
        }
    }

    return gestureHandled
}

}

