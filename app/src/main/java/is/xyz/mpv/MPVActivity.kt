package is.xyz.mpv

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import is.xyz.mpv.databinding.ActivityMpvBinding

class MPVActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMpvBinding
    private lateinit var mpv: MPVView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMpvBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize mpv view
        mpv = binding.mpvView
        mpv.initialize(getExternalFilesDir(null)?.path + "/mpv")

        // Load media from intent
        intent.data?.let { uri ->
            mpv.play(uri.toString())
        }
    }

    override fun onPause() {
        mpv.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mpv.resume()
    }

    override fun onDestroy() {
        mpv.destroy()
        super.onDestroy()
    }

    // Optional: Add method for custom gesture configuration
    fun setCustomGestureConfig(config: Any) {
        // Placeholder for passing settings to TouchGestures if needed
    }
}
