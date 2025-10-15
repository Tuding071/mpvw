package is.xyz.mpv

import android.os.Bundle
import android.view.View
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
        val uri = intent.data
        if (uri != null) {
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

    // Optional: Add methods for your custom configuration
    fun setCustomGestureConfig(config: Any) {
        // Example: Pass settings to TouchGestures if needed
    }
}
