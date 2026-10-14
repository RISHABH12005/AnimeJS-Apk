package com.example.animejs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity

private const val HOME_URL = "https://animejs.com/"

/**
 * Opens Anime.js in the user's installed browser instead of Android WebView.
 *
 * The Anime.js landing page relies on complex scroll-synchronized transforms.
 * The user's reference recording shows it rendering correctly in a full browser,
 * while Android WebView leaves stale animation frames behind. Delegating to the
 * browser uses the same rendering engine/path as the working reference.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(HOME_URL)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No web browser is installed to open Anime.js.",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            finish()
        }
    }
}
