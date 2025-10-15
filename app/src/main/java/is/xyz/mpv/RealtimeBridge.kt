package `is`.xyz.mpv

object RealtimeBridge {

    init {
        // Load the native library (we’ll create this later in JNI)
        System.loadLibrary("realtime_seek")
    }

    /**
     * Called when a seeking gesture happens.
     * @param diff The normalized seek amount (from TouchGestures).
     */
    external fun onSeek(diff: Float)

    /**
     * Initialize native resources. Call once when player starts.
     */
    external fun initPlayer()

    /**
     * Cleanup native resources. Call on exit.
     */
    external fun finalizePlayer()
}
