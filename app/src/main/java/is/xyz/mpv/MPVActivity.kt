package `is`.xyz.mpv

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MPVActivity : AppCompatActivity() {
    private lateinit var mpvView: MPVView
    private lateinit var jsOverlay: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mpvView = findViewById(R.id.mpv_view)

        // Setup the invisible JavaScript overlay system
        setupJSOverlay()

        // Hide any app UI if present (optional, depends on your layout)
        hideDefaultUI()
    }

    private fun hideDefaultUI() {
        // If your layout has top_controls, bottom_controls, etc., hide them here
        // Example:
        // findViewById<View>(R.id.top_controls)?.visibility = View.GONE
        // findViewById<View>(R.id.bottom_controls)?.visibility = View.GONE
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupJSOverlay() {
        jsOverlay = WebView(this)
        jsOverlay.setBackgroundColor(Color.TRANSPARENT)
        jsOverlay.isClickable = true
        jsOverlay.isFocusable = true

        val settings = jsOverlay.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true

        jsOverlay.webViewClient = WebViewClient()
        jsOverlay.addJavascriptInterface(JSBridge(), "Android")

        // Add WebView to the screen covering everything
        addContentView(
            jsOverlay,
            android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Load all .js scripts from /sdcard/mpv/scripts
        loadJSScripts()
    }

    private fun loadJSScripts() {
        val scriptDir = File(Environment.getExternalStorageDirectory(), "mpv/scripts")
        if (!scriptDir.exists()) {
            Log.w("MPV-JS", "Script directory not found: ${scriptDir.absolutePath}")
            return
        }

        val jsFiles = scriptDir.listFiles { f -> f.extension == "js" } ?: emptyArray()
        if (jsFiles.isEmpty()) {
            Log.w("MPV-JS", "No JS files found in ${scriptDir.absolutePath}")
            return
        }

        val combined = buildString {
            jsFiles.forEach { file ->
                appendLine(file.readText())
            }
        }

        val html = """
            <html>
            <body style='margin:0;padding:0;overflow:hidden;background:transparent;'>
            <script>
            $combined
            </script>
            </body>
            </html>
        """.trimIndent()

        jsOverlay.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        Log.i("MPV-JS", "Loaded ${jsFiles.size} JS files from ${scriptDir.absolutePath}")
    }

    inner class JSBridge {
        @JavascriptInterface
        fun sendCommand(cmd: String, arg: String?) {
            when (cmd) {
                "pause" -> mpvView.command(arrayOf("cycle", "pause"))
                "play" -> mpvView.command(arrayOf("set", "pause", "no"))
                "seek" -> mpvView.command(arrayOf("seek", arg ?: "0", "relative"))
                "set_speed" -> mpvView.command(arrayOf("set", "speed", arg ?: "1"))
                "show_ui" -> runOnUiThread { /* optional if UI exists */ }
                "hide_ui" -> runOnUiThread { /* optional if UI exists */ }
                else -> Log.d("MPV-JSBridge", "Unknown command: $cmd ($arg)")
            }
        }

        @JavascriptInterface
        fun log(msg: String) {
            Log.d("MPV-JS", msg)
        }
    }
}
