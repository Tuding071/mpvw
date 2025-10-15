package `is`.xyz.mpv

interface TouchGesturesObserver {
    enum class PropertyChange {
        Init, Seek, Volume, Bright, Finalize, SeekFixed, PlayPause, Custom,
        FrameScrubStart, FrameScrub, FrameScrubFinalize
    }
    
    fun onPropertyChange(p: PropertyChange, diff: Float)
}
