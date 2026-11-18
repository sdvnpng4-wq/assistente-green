package com.green.assistente;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

public class MainActivity extends Activity {
    private static final int OVERLAY_REQUEST = 1001;
    private String sharedText;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Intent incoming = getIntent();
        if (incoming != null && Intent.ACTION_SEND.equals(incoming.getAction())
                && "text/plain".equals(incoming.getType())) {
            sharedText = incoming.getStringExtra(Intent.EXTRA_TEXT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(permission, OVERLAY_REQUEST);
        } else {
            startAssistant();
        }
    }

    private void startAssistant() {
        Intent service = new Intent(this, FloatingService.class);
        if (sharedText != null && !sharedText.trim().isEmpty()) {
            service.putExtra(FloatingService.EXTRA_SHARED_TEXT, sharedText);
        }
        startService(service);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))) {
            startAssistant();
        }
    }
}
