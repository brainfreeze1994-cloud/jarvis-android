package com.jarvis.ai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Transparent activity that receives shared content from other apps
 * and passes it to MainActivity.
 *
 * Register in AndroidManifest with ACTION_SEND intent filter.
 */
public class ShareReceiver extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleShare(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleShare(intent);
    }

    private void handleShare(Intent intent) {
        if (intent == null) { finish(); return; }

        String action = intent.getAction();
        String type   = intent.getType();

        String sharedText  = null;
        String sharedImage = null;  // URI string

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("text/")) {
                sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                // Also grab subject if present
                String subj = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                if (subj != null && !subj.isEmpty() && sharedText != null)
                    sharedText = subj + "\n\n" + sharedText;
            } else if (type.startsWith("image/")) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null) sharedImage = uri.toString();
            }
        }

        // Launch or bring MainActivity to front with the shared data
        Intent main = new Intent(this, MainActivity.class);
        main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        main.putExtra("shared_text",  sharedText);
        main.putExtra("shared_image", sharedImage);
        startActivity(main);
        finish();
    }
}
