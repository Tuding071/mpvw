package `is`.xyz.mpv

import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.PointF
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import kotlin.math.*

enum class PropertyChange {
    Init,
    Seek,
    Volume,
    Bright,
    Finalize,

    /* Tap gestures */
    SeekFixed,
    PlayPause,
    Custom,
    
    /* Time seeking */
    TimeSeek,
    Pause,
    Resume,
}

internal interface TouchGesturesObserver {
    fun onPropertyChange(p: PropertyChange, diff: Float)
}

internal class TouchGestures(private val observer: TouchGesturesObserver) {

    private enum class State {
        Up,
        Down,
        ControlSeek,
        ControlVolume,
        ControlBright,
        ControlTimeSeek,
    }

    private enum class CustomAreaSection {
        LEFT,
        CENTER,
        RIGHT,
        NONE
    }

    private var state = State.Up
    private var stateDirection = 0

    private var lastTapTime = 0L
    private var lastDownTime = 0L

    private var initialPos = PointF()
    private var lastPos = PointF()
    
    private var timeSeekStartPos = PointF()
    private var lastTimeTriggerPos = PointF()

    private var width = 0f
    private var height = 0f
    private var trigger = 0f
    
    // NEW: Separate trigger for custom area time seeking
    private var customAreaTrigger = 0f
    
    private var isCustomCenterTouch = false 
    private var customAreaSection = CustomAreaSection.NONE
    private var wasVideoPlayingBeforeSeek = false

    private var gestureHoriz = State.Down
    private var gestureVertLeft = State.Down
    private var gestureVertRight = State.Down
    private var tapGestureLeft : PropertyChange? = null
    private var tapGestureCenter : PropertyChange? = null
    private var tapGestureRight : PropertyChange? = null

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
        // NEW: Custom area trigger is much smaller for immediate response
        customAreaTrigger = min(width, height) / CUSTOM_TRIGGER_RATE
    }

    companion object {
        private const val TAG = "mpv"

        private const val TRIGGER_RATE = 30
        // NEW: Much more sensitive trigger for custom area
        private const val CUSTOM_TRIGGER_RATE = 100

        private const val TAP_DURATION = 300L
        private const val CONTROL_SEEK_MAX = 150f
        private const val CONTROL_VOLUME_MAX = 1.5f
        private const val CONTROL_BRIGHT_MAX = 1.5f
        private const val DEADZONE = 5
        
        private const val CUSTOM_CENTER_TOP_PERCENT = 5f
        private const val CUSTOM_CENTER_BOTTOM_PERCENT = 75f
        
        // TIME SEEKING CONSTANTS - MAKE THESE MORE SENSITIVE
        private const val TIME_SEEK_PIXEL_TRIGGER = 8f // Reduced from 12px for faster response
        private const val MILLISECONDS_PER_TRIGGER = 80f
    }

    private fun getCustomAreaSection(x: Float, y: Float): CustomAreaSection {
        val customCenterTopY = height * CUSTOM_CENTER_TOP_PERCENT / 100f
        val customCenterBottomY = height * CUSTOM_CENTER_BOTTOM_PERCENT / 100f
        
        if (y < customCenterTopY || y > customCenterBottomY) {
            return CustomAreaSection.NONE
        }
        
        val sectionWidth = width / 3f
        
        return when {
            x < sectionWidth -> CustomAreaSection.LEFT
            x < sectionWidth * 2 -> CustomAreaSection.CENTER
            else -> CustomAreaSection.RIGHT
        }
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

        assertFloat(initialPos.x, initialPos.y)
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
            State.ControlTimeSeek -> {
                // Time seeking handled separately
            }
        }
        return state != State.Up && state != State.Down
    }
    
    // NEW: Simplified time seeking without throttling
    private fun processVerticalTimeSeek(p: PointF): Boolean {
        val dy = p.y - lastTimeTriggerPos.y
        
        // Check if we've moved enough pixels vertically (absolute value)
        if (abs(dy) >= TIME_SEEK_PIXEL_TRIGGER) {
            // UP = forward, DOWN = backward
            val direction = if (dy < 0) 1f else -1f
            
            // Send time seek command
            val seekMs = direction * MILLISECONDS_PER_TRIGGER
            sendPropertyChange(PropertyChange.TimeSeek, seekMs)
            
            // ALWAYS update trigger position to prevent accumulation
            lastTimeTriggerPos.set(lastTimeTriggerPos.x, p.y)
        }
        
        return true
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
        if (width < 1 || height < 1) {
            Log.w(TAG, "TouchGestures: width or height not set!")
            return false
        }
        if (!checkFloat(e.x, e.y)) {
            Log.w(TAG, "TouchGestures: ignoring invalid point ${e.x} ${e.y}")
            return false
        }
        var gestureHandled = false
        val point = PointF(e.x, e.y)
        when (e.action) {
            MotionEvent.ACTION_UP -> {
                if (isCustomCenterTouch) {
                    when (state) {
                        State.ControlTimeSeek -> {
                            if (wasVideoPlayingBeforeSeek) {
                                sendPropertyChange(PropertyChange.Resume, 0f)
                            }
                        }
                        State.Down -> {
                            if (customAreaSection == CustomAreaSection.CENTER) {
                                sendPropertyChange(PropertyChange.PlayPause, 0f)
                                lastTapTime = SystemClock.uptimeMillis() 
                            }
                        }
                        else -> {}
                    }
                    gestureHandled = true
                    isCustomCenterTouch = false
                    customAreaSection = CustomAreaSection.NONE
                    state = State.Up
                } else {
                    gestureHandled = processMovement(point) or processTap(point)
                    if (state != State.Down)
                        sendPropertyChange(PropertyChange.Finalize, 0f)
                    state = State.Up
                }
            }
            MotionEvent.ACTION_DOWN -> {
                customAreaSection = getCustomAreaSection(e.x, e.y)
                
                if (customAreaSection == CustomAreaSection.NONE) {
                    isCustomCenterTouch = false
                    return false
                }
                
                isCustomCenterTouch = true
                initialPos.set(point)
                timeSeekStartPos.set(point)
                lastTimeTriggerPos.set(point) // Reset trigger position
                
                if (customAreaSection == CustomAreaSection.LEFT || customAreaSection == CustomAreaSection.RIGHT) {
                    wasVideoPlayingBeforeSeek = true
                    sendPropertyChange(PropertyChange.Pause, 0f)
                }
                
                lastPos.set(point)
                state = State.Down
                gestureHandled = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isCustomCenterTouch) {
                    when (state) {
                        State.Down -> {
                            if (customAreaSection == CustomAreaSection.LEFT || customAreaSection == CustomAreaSection.RIGHT) {
                                val dy = point.y - initialPos.y
                                // USE CUSTOM TRIGGER for faster response
                                if (abs(dy) > customAreaTrigger) {
                                    state = State.ControlTimeSeek
                                    gestureHandled = processVerticalTimeSeek(point)
                                } else {
                                    lastPos.set(point)
                                    gestureHandled = true
                                }
                            } else {
                                lastPos.set(point)
                                gestureHandled = true
                            }
                        }
                        State.ControlTimeSeek -> {
                            gestureHandled = processVerticalTimeSeek(point)
                        }
                        else -> {
                            lastPos.set(point)
                            gestureHandled = true
                        }
                    }
                } else {
                    gestureHandled = processMovement(point)
                }
            }
        }
        return gestureHandled
    }
}
